package eu.stamp.botsing.graphs.cfg;

import com.google.common.collect.Lists;
import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import eu.stamp.botsing.commons.instrumentation.ClassInstrumentation;
import org.evosuite.graphs.GraphPool;
import org.evosuite.graphs.cdg.ControlDependenceGraph;
import org.evosuite.graphs.cfg.*;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CFGGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(CFGGenerator.class);

    ClassInstrumentation classInstrumenter = new ClassInstrumentation();
    private Map<String,List<RawControlFlowGraph>> cfgs = new HashMap<>();
    private List<FrameControlFlowGraph> frameCFGs =  new LinkedList<>();
    private BotsingRawControlFlowGraph rawInterProceduralGraph;
    private ActualControlFlowGraph actualInterProceduralGraph;
    private ControlDependenceGraph controlDependenceInterProceduralGraph;

    public void generateInterProceduralCFG() {
        List<String> interestingClasses = CrashProperties.getInstance().getTargetClasses();
        List<Class> instrumentedClasses = classInstrumenter.instrumentClasses(interestingClasses);
        if(!instrumentedClasses.isEmpty()){
            collectCFGS(instrumentedClasses);
        }else{
            LOG.error("There is no instrumented classes!");
        }

        generateRawGraph();
        actualInterProceduralGraph = new BotsingActualControlFlowGraph(rawInterProceduralGraph);
        controlDependenceInterProceduralGraph = new ControlDependenceGraph(actualInterProceduralGraph);

        loggGeneratedCDG();
    }

    // Logging the generated control dependence graph
    private void loggGeneratedCDG() {

        for(BasicBlock block: controlDependenceInterProceduralGraph.vertexSet()){
            LOG.debug("DEPTH of {} is:",block.explain());
            for (ControlDependency cd : controlDependenceInterProceduralGraph.getControlDependentBranches(block)){
                LOG.debug("--> {}",cd.toString());
            }
        }
    }


    private void collectCFGS(List<Class> instrumentedClasses) {
        GraphPool graphPool = GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
        for (Class clazz : instrumentedClasses){
            Map<String, RawControlFlowGraph> methodsGraphs = graphPool.getRawCFGs(clazz.getName());
            if (methodsGraphs != null) {
                cfgs.put(clazz.getName(),new ArrayList<>());
                for (Map.Entry<String, RawControlFlowGraph> entry : methodsGraphs.entrySet()) {
                    RawControlFlowGraph cfg = entry.getValue();
                    cfgs.get(clazz.getName()).add(cfg);
                }
            } else {
                LOG.warn("The generated control flow graphs for class {} was empty.", clazz);
            }
        }
    }

    private void generateRawGraph(){
        // Collect interesting method's cfgs
        if(frameCFGs.size() != CrashProperties.getInstance().getStackTrace().getNumberOfFrames()){
            frameCFGs.clear();
            CollectInterestingCFGs();

            BytecodeInstruction src = null;
            int cfgCounter = 0;
            for(FrameControlFlowGraph fcfg: Lists.reverse(frameCFGs)){
                LOG.debug("Class: {}",fcfg.getRcfg().getClassName());
                LOG.debug("Method: {}",fcfg.getRcfg().getMethodName());
                LOG.debug("CFG: {}",fcfg.getRcfg().toString());
                LOG.debug("Linking point: {}",(fcfg.getCallingInstruction()==null)?"NULL":fcfg.getCallingInstruction().toString());
                LOG.debug("~~~~~~~~~~~~~~~~");

                if(cfgCounter == 0){
                    rawInterProceduralGraph = new BotsingRawControlFlowGraph(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT(),"IntegrationTestingGraph","methodsIntegration",fcfg.getRcfg().getMethodAccess());
                    rawInterProceduralGraph.clone(fcfg.getRcfg());
                }else if (src != null){
                    BytecodeInstruction target = fcfg.getRcfg().determineEntryPoint();
                    LOG.debug("target of new edge is: {}",target.explain());
                    Set<BytecodeInstruction> fcfgExitPoints = fcfg.getRcfg().determineExitPoints();
                    rawInterProceduralGraph.clone(fcfg.getRcfg());
                    rawInterProceduralGraph.addInterProceduralEdge(src,target,fcfgExitPoints);
                }
                src=fcfg.getCallingInstruction();
                cfgCounter++;
            }

            LOG.debug("FINAL Result: {}",rawInterProceduralGraph.toString());


        }

        // TODO: Handling missing frames?

    }

    private void CollectInterestingCFGs() {
        ArrayList<StackTraceElement> frames = CrashProperties.getInstance().getStackTrace().getAllFrames();
        int targetFrameLevel = CrashProperties.getInstance().getStackTrace().getTargetFrameLevel();
        int frameCounter = 1;
        boolean lastMethodWasPrivate=false;
        for (StackTraceElement f: frames){
            if (frameCounter > targetFrameLevel && !lastMethodWasPrivate){
                break;
            }
            // Each frame should have a particular CFG
            String className = f.getClassName();
            String methodName = f.getMethodName();
            int lineNumber = f.getLineNumber();
            LOG.info(""+className);

            // Find the cfg
            boolean cfgFound = false;
            for (RawControlFlowGraph classCFG: cfgs.get(className)){
                if(cfgFound){
                    break;
                }
                if(classCFG.getMethodName().contains(methodName)){
                    List<BytecodeInstruction> bytecodeInstructions;
                    if(frameCounter==1){
                        bytecodeInstructions = new ArrayList(classCFG.vertexSet());
                    }else{
                        bytecodeInstructions =  classCFG.determineMethodCalls();
                    }
                    for (BytecodeInstruction instruction: bytecodeInstructions){
                        if(lineNumber==instruction.getLineNumber() && ((frameCounter==1) || (frameCounter!= 1 && instruction.getCalledMethod().contains(frames.get(frameCounter-2).getMethodName())))){
                            LOG.debug("The following cfg is the right one for class {}, method {}, and line number {} : {}",className,methodName,lineNumber,classCFG.toString());
                            frameCFGs.add(new FrameControlFlowGraph(classCFG,(frameCounter==1)?null:instruction));
                            cfgFound=true;
                            lastMethodWasPrivate = isPrivateMethod(classCFG);
                            break;
                        }
                    }
                }
            }
            if(!cfgFound){
                LOG.warn("Could not find the cfg of class {}, method {}, and line number {}.",className,methodName,lineNumber);
                if(isIrrelevantFrame(className,methodName,lineNumber)){
                    LOG.info("Frame level {} is an irrelevant frame. We do not count it in the InterProcedural graph",frameCounter);
                }
            }
            frameCounter++;
        }
    }

    private boolean isIrrelevantFrame(String className, String methodName, int lineNumber) {
        for (RawControlFlowGraph classCFG: cfgs.get(className)){
            if(classCFG.getMethodName().contains(methodName)){
                int maxLine = -1;
                int minLine = Integer.MAX_VALUE;
                for (BytecodeInstruction instruction: classCFG.vertexSet()){
                    int currentLineNumber = instruction.getLineNumber();
                    if(currentLineNumber > maxLine){
                        maxLine = currentLineNumber;
                    }
                    if(currentLineNumber < minLine && currentLineNumber>0){
                        minLine = currentLineNumber;
                    }
                }

                if(lineNumber<maxLine && lineNumber > minLine){
                    return false;
                }
            }
        }
        return true;
    }


    private boolean isPrivateMethod(RawControlFlowGraph acfg){
        return (acfg.getMethodAccess() & Opcodes.ACC_PRIVATE) == Opcodes.ACC_PRIVATE;
    }

}
