package eu.stamp.botsing.integration.graphs.cfg;

import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import eu.stamp.botsing.commons.graphs.cfg.BotsingRawControlFlowGraph;
import org.evosuite.graphs.GraphPool;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.RawControlFlowGraph;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CFGGeneratorUtility {
    private static final Logger LOG = LoggerFactory.getLogger(CFGGeneratorUtility.class);

    public void collectCFGS(Class clazz, Map<String,List<RawControlFlowGraph>> cfgs) {
        cfgs.put(clazz.getName(),new ArrayList<>());
        GraphPool graphPool = GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
        Map<String, RawControlFlowGraph> methodsGraphs = graphPool.getRawCFGs(clazz.getName());
        if (methodsGraphs != null) {
            for (Map.Entry<String, RawControlFlowGraph> entry : methodsGraphs.entrySet()) {
                RawControlFlowGraph cfg = entry.getValue();
                cfgs.get(clazz.getName()).add(cfg);
            }
        } else {
            LOG.warn("The generated control flow graphs for class {} was empty.", clazz);
        }
    }

    public static boolean isPrivateMethod(RawControlFlowGraph rcfg){
        return (rcfg.getMethodAccess() & Opcodes.ACC_PRIVATE) == Opcodes.ACC_PRIVATE;
    }

    public BotsingRawControlFlowGraph makeBotsingRawControlFlowGraphObject(){
        return new BotsingRawControlFlowGraph(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT(),"IntegrationTestingGraph","methodsIntegration",1);
    }


    public BotsingRawControlFlowGraph generateInterProceduralGraphOfClass(List<RawControlFlowGraph> involvedCFGs){
        BotsingRawControlFlowGraph rawInterProceduralGraph = makeBotsingRawControlFlowGraphObject();
        Map<String,RawControlFlowGraph> interestingMethods = new HashMap<>();
        for(RawControlFlowGraph rcfg: involvedCFGs){
            rawInterProceduralGraph.clone(rcfg);
            interestingMethods.put(rcfg.getMethodName(),rcfg);
        }


        for(RawControlFlowGraph rcfg: involvedCFGs){
            for(BytecodeInstruction instruction: rcfg.determineMethodCallsToOwnClass()){
                if(interestingMethods.keySet().contains(instruction.getCalledMethod())){
                    BytecodeInstruction src = instruction;
                    BytecodeInstruction target = interestingMethods.get(instruction.getCalledMethod()).determineEntryPoint();
                    Set<BytecodeInstruction> exitPoints = interestingMethods.get(instruction.getCalledMethod()).determineExitPoints();
                    rawInterProceduralGraph.addInterProceduralEdge(src,target,exitPoints);
                }
            }
        }


        return rawInterProceduralGraph;

    }

}
