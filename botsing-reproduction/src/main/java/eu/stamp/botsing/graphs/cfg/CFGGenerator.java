package eu.stamp.botsing.graphs.cfg;

import com.google.common.collect.Lists;
import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import eu.stamp.botsing.commons.instrumentation.ClassInstrumentation;
import org.evosuite.graphs.GraphPool;
import org.evosuite.graphs.cdg.ControlDependenceGraph;
import org.evosuite.graphs.cfg.*;
import org.evosuite.utils.Randomness;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CFGGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(CFGGenerator.class);

    ClassInstrumentation classInstrumenter = new ClassInstrumentation();
    protected Map<String,List<RawControlFlowGraph>> cfgs = new HashMap<>();
    protected List<FrameControlFlowGraph> frameCFGs =  new LinkedList<>();
    private BotsingRawControlFlowGraph rawInterProceduralGraph;
    private ActualControlFlowGraph actualInterProceduralGraph;
    private ControlDependenceGraph controlDependenceInterProceduralGraph;

    public void generateInterProceduralCFG() {
        List<String> interestingClasses = CrashProperties.getInstance().getTargetClasses();
        List<Class> instrumentedClasses = classInstrumenter.instrumentClasses(interestingClasses);
        if(!instrumentedClasses.isEmpty()){
            collectCFGS(instrumentedClasses);
        }else{
            throw new IllegalArgumentException("There is no instrumented classes!");
        }

        generateRawGraph();
        actualInterProceduralGraph = new BotsingActualControlFlowGraph(rawInterProceduralGraph);
        GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).registerActualCFG(actualInterProceduralGraph);
        controlDependenceInterProceduralGraph = new ControlDependenceGraph(actualInterProceduralGraph);
        GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).registerControlDependence(controlDependenceInterProceduralGraph);

        logGeneratedCDG();
    }

    // Logging the generated control dependence graph
    protected void logGeneratedCDG() {
        for(BasicBlock block: controlDependenceInterProceduralGraph.vertexSet()){
            LOG.debug("DEPTH of {} is:",block.explain());
            for (ControlDependency cd : controlDependenceInterProceduralGraph.getControlDependentBranches(block)){
                LOG.debug("--> {}",cd.toString());
            }
        }
    }


    protected void collectCFGS(List<Class> instrumentedClasses) {
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

    protected BotsingRawControlFlowGraph makeBotsingRawControlFlowGraphObject(int methodAccess){
        return new BotsingRawControlFlowGraph(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT(),"IntegrationTestingGraph","methodsIntegration",methodAccess);
    }

    protected void generateRawGraph(){
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
                    rawInterProceduralGraph = makeBotsingRawControlFlowGraphObject(fcfg.getRcfg().getMethodAccess());
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

            if(frameCFGs.size() > 0){
                LOG.debug("FINAL Result: {}",rawInterProceduralGraph.toString());
            }



        }

        // TODO: Handling missing frames?

    }

    protected void CollectInterestingCFGs() {
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
            LOG.info("Analyzing "+className);

            // Find the cfg
            boolean cfgFound = false;
            if (cfgs.get(className) == null){
                LOG.info("[frame {}]Cannot load class {}. We do not count it in the InterProcedural graph",frameCounter,className);
                frameCounter++;
                continue;
            }
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
                boolean fixed = false;
                LOG.warn("Could not find the cfg of class {}, method {}, and line number {}.",className,methodName,lineNumber);
                HashMap<RawControlFlowGraph,List<BytecodeInstruction>> candidates = estimateTheRightLine(className,methodName,lineNumber,frameCounter,frames);
                if(frameCounter>1 && isIrrelevantFrame(className,methodName,lineNumber,frameCounter,frames)){
                    LOG.info("Frame level {} is an irrelevant frame. We do not count it in the InterProcedural graph",frameCounter);
                    CrashProperties.getInstance().getStackTrace().addIrrelevantFrameLevel(frameCounter);
                    fixed=true;
                }else if(candidates.size()>0) {
                    LOG.info("Found {} candidates to repair the stack trace line number",frameCounter);
                    if(CrashProperties.lineEstimation){
                        LOG.info("detect_missing_line option is enabled.We will make a link beetween each of the lin");
                        RawControlFlowGraph selectedMethod = Randomness.choice(candidates.keySet());
                        BytecodeInstruction selectedCandidate = Randomness.choice(candidates.get(selectedMethod));
                        LOG.info("Selected candidate is in line {} of method {}",selectedCandidate.getLineNumber(),selectedMethod.getMethodName());
                        frameCFGs.add(new FrameControlFlowGraph(selectedMethod,(frameCounter==1)?null:selectedCandidate));
                        fixed=true;
                    }else{
                        LOG.info("detect_missing_line option is disabled. So, this execution will be stopped");
                    }
                }else{
                    LOG.info("could not find any candidate.");
                    LOG.info("Probably, Frame level {} is an irrelevant frame. We do not count it in the InterProcedural graph",frameCounter);
                    CrashProperties.getInstance().getStackTrace().addIrrelevantFrameLevel(frameCounter);
                    fixed=true;
                }

                if(!fixed){
                    throw new IllegalArgumentException("Mismatched line numbers for frame "+ frameCounter);
                }
            }
            frameCounter++;
        }
    }

    protected HashMap<RawControlFlowGraph,List<BytecodeInstruction>> estimateTheRightLine(String className, String methodName, int lineNumber, int frameCounter, ArrayList<StackTraceElement> frames) {
        HashMap<RawControlFlowGraph,List<BytecodeInstruction>> candidates =  new HashMap<>();
        for (RawControlFlowGraph classCFG: cfgs.get(className)){
            if(classCFG.getMethodName().contains(methodName)){
                for (BytecodeInstruction instruction: classCFG.determineMethodCallsToOwnClass()){
                    if(frameCounter!= 1 && instruction.getCalledMethod().contains(frames.get(frameCounter-2).getMethodName())){
                        if(!candidates.containsKey(classCFG)){
                            candidates.put(classCFG,new ArrayList<>());
                        }
                        candidates.get(classCFG).add(instruction);
                    }
                }
            }
        }
        return candidates;
    }

    protected boolean isIrrelevantFrame(String className, String methodName, int lineNumber,int frameCounter, ArrayList<StackTraceElement> frames) {
        return (isNotInDomain(className,methodName,lineNumber) && frames.get(frameCounter-2).getMethodName().equals(methodName));
    }

    protected boolean isNotInDomain(String className, String methodName, int lineNumber) {
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


    protected boolean isPrivateMethod(RawControlFlowGraph acfg){
        return (acfg.getMethodAccess() & Opcodes.ACC_PRIVATE) == Opcodes.ACC_PRIVATE;
    }

}
