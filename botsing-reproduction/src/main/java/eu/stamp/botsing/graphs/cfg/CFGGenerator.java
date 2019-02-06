package eu.stamp.botsing.graphs.cfg;

import com.google.common.collect.Lists;
import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import eu.stamp.botsing.commons.instrumentation.ClassInstrumentation;
import org.evosuite.graphs.GraphPool;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.RawControlFlowGraph;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CFGGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(CFGGenerator.class);

    ClassInstrumentation classInstrumenter = new ClassInstrumentation();
    private Map<String,List<RawControlFlowGraph>> cfgs = new HashMap<>();
    private List<FrameControlFlowGraph> frameCFGs =  new LinkedList<>();
    private BotsingControlFlowGraph InterProceduralGraph;

    public void generateInterProceduralCFG() {
        List<String> interestingClasses = CrashProperties.getInstance().getTargetClasses();
        List<Class> instrumentedClasses = classInstrumenter.instrumentClasses(interestingClasses);
        if(!instrumentedClasses.isEmpty()){
            collectCFGS(instrumentedClasses);
        }else{
            LOG.error("There is no instrumented classes!");
        }

        generateGraph();
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

    private void generateGraph(){
        // Collect interesting method's cfgs
        if(frameCFGs.size() != CrashProperties.getInstance().getStackTrace().getNumberOfFrames()){
            frameCFGs.clear();
            CollectInterestingCFGs();

            BytecodeInstruction src = null;
            int cfgCounter = 0;
            for(FrameControlFlowGraph fcfg: Lists.reverse(frameCFGs)){

                LOG.info("Class: {}",fcfg.getRcfg().getClassName());
                LOG.info("Method: {}",fcfg.getRcfg().getMethodName());
                LOG.info("CFG: {}",fcfg.getRcfg().toString());
                LOG.info("Linking point: {}",(fcfg.getExitingBCInst()==null)?"NULL":fcfg.getExitingBCInst().toString());
                LOG.info("~~~~~~~~~~~~~~~~");

                if(cfgCounter == 0){
                    InterProceduralGraph = new BotsingControlFlowGraph(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT(),"InterPro","multiple",fcfg.getRcfg().getMethodAccess());
                    InterProceduralGraph.clone(fcfg.getRcfg());
                }else if (src != null){
                    BytecodeInstruction target = fcfg.getRcfg().determineEntryPoint();
                    InterProceduralGraph.clone(fcfg.getRcfg());
                    InterProceduralGraph.addInterProceduralEdge(src,target);
                }
                src=fcfg.getExitingBCInst();
                cfgCounter++;
            }

            LOG.info("FINAL Result: {}",InterProceduralGraph.toString());


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
                    LOG.info("Method signature: {}",classCFG.getMethodName());
                    List<BytecodeInstruction> bytecodeInstructions;
                    if(frameCounter==1){
                        bytecodeInstructions = new ArrayList(classCFG.vertexSet());
                    }else{
                        bytecodeInstructions =  classCFG.determineMethodCalls();
                    }
                    for (BytecodeInstruction instruction: bytecodeInstructions){
                        if(lineNumber==instruction.getLineNumber() && ((frameCounter==1) || (frameCounter!= 1 && instruction.getCalledMethod().contains(frames.get(frameCounter-2).getMethodName())))){
                            LOG.info("The following cfg is the right one for class {}, method {}, and line number {}: {}",className,methodName,lineNumber,classCFG.toString());
                            frameCFGs.add(new FrameControlFlowGraph(classCFG,(frameCounter==1)?null:instruction));
                            cfgFound=true;
                            lastMethodWasPrivate = isPrivateMethod(classCFG);
                            break;
                        }
                    }
                }
            }
            if(!cfgFound){
                LOG.error("Could not find the cfg of class {}, method {}, and line number {}.",className,methodName,lineNumber);
            }
            frameCounter++;
        }
    }


    private boolean isPrivateMethod(RawControlFlowGraph acfg){
        return (acfg.getMethodAccess() & Opcodes.ACC_PRIVATE) == Opcodes.ACC_PRIVATE;
    }

}
