package eu.stamp.botsing.graphs.cfg;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import eu.stamp.botsing.commons.instrumentation.ClassInstrumentation;
import org.evosuite.graphs.GraphPool;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.RawControlFlowGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CFGGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(CFGGenerator.class);

    ClassInstrumentation classInstrumenter = new ClassInstrumentation();
    private Map<String,List<RawControlFlowGraph>> cfgs = new HashMap<>();
    private Map<String,List<RawControlFlowGraph>> interestingCFGs = new HashMap<>();

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
        // Collect interesting method's cfgs.
        // Start the loop from the higher frames to the deeper one:
        //      - Find the target frame (Including checking the public/private target method)
        //      - Get the target method's cfg, and find the BCinst that we should set as the new graph's source
        //      - Get the target method of the one-level deeper cfg and set its entry point as the target of new edge.
        //      - continue 2 last jobs for all of the frames
        // [?] What should we do for the missing frames?


        int counter = 0;
        for (Map.Entry<String, List<RawControlFlowGraph>> entry : cfgs.entrySet()) {
            String className = entry.getKey();
            List<RawControlFlowGraph> rcfgs = entry.getValue();
            BotsingControlFlowGraph bcfg = null;
            for(RawControlFlowGraph cfg: rcfgs){
                if(counter == 0){
                    LOG.info("CFG of class {}, method {} is: {}",cfg.getClassName(),cfg.getMethodName(),cfg.toString());
                    bcfg = new BotsingControlFlowGraph(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT(),cfg.getClassName(),cfg.getMethodName(),cfg.getMethodAccess());
                    bcfg.clone(cfg);
                    LOG.info("BCFG: {}",bcfg.toString());
                } else if (counter == 1 && bcfg != null){
                    BytecodeInstruction target = cfg.determineEntryPoint();
                    BytecodeInstruction src = null;
//                    BytecodeInstructionPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).getFirstInstructionAtLineNumber()
                    Set<BytecodeInstruction> exitPoints = bcfg.determineExitPoints();
                    for (BytecodeInstruction ep: exitPoints){
                        src = ep;
                        break;
                    }
                    if (src != null) {
                        bcfg.addInterProceduralEdge(src,target);
                    }else{
                        LOG.warn("SOURCE is empty");
                    }
                    LOG.info("BCFG2: {}",bcfg.toString());
                }
                counter++;
            }

        }
    }


}
