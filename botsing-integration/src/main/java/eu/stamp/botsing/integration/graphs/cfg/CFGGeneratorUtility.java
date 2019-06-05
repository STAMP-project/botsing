package eu.stamp.botsing.integration.graphs.cfg;

import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import eu.stamp.botsing.commons.graphs.cfg.BotsingRawControlFlowGraph;
import org.evosuite.graphs.GraphPool;
import org.evosuite.graphs.cfg.ActualControlFlowGraph;
import org.evosuite.graphs.cfg.BasicBlock;
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

    public List<List<BasicBlock>> detectIndependetPathsForMethod(ActualControlFlowGraph controlFlowGraph,BasicBlock targetNode){

        int maximumPaths = controlFlowGraph.getCyclomaticComplexity()-1; // There is always one edge from entry block to exit block which ++ the complexity!
        // BFS
        BasicBlock entryPoint = controlFlowGraph.getEntryPoint().getBasicBlock();
        // Detected paths using BFS
        List<List<BasicBlock>> detectedPaths = makePath(entryPoint,controlFlowGraph, new HashMap<>(),targetNode);
        // remove non-independent paths
        if(targetNode!=null || detectedPaths.size() > maximumPaths){
            removeNonIndependentPaths(detectedPaths);
        }

        return detectedPaths;
    }

    private void removeNonIndependentPaths(List<List<BasicBlock>> detectedPaths){
        Map<BasicBlock,List<BasicBlock>> coveredEdges = new HashMap<>();
        List<Integer> shouldRemove = new ArrayList<>();
        for (List<BasicBlock> path: detectedPaths){
            boolean newEdgeAdded = false;
            for(int sequenceIndex=1;sequenceIndex<path.size();sequenceIndex++){
                BasicBlock src = path.get(sequenceIndex-1);
                BasicBlock target = path.get(sequenceIndex);
                if(!coveredEdges.containsKey(src) || !coveredEdges.get(src).contains(target)){
                    // Not covered yet
                    if(!coveredEdges.containsKey(src)){
                        coveredEdges.put(src,new ArrayList<>());
                    }
                    coveredEdges.get(src).add(target);
                    newEdgeAdded=true;
                }
            }


            if(!newEdgeAdded){
                // This path does not add sth new to the other existing paths
                shouldRemove.add(detectedPaths.indexOf(path));
            }
        }

        for(int index:shouldRemove){
            detectedPaths.remove(index);
        }

    }

    private List<List<BasicBlock>> makePath(BasicBlock currentNode,  ActualControlFlowGraph controlFlowGraph, Map<BasicBlock,Integer> coveredNodes, BasicBlock targetNode) {
        List<List<BasicBlock>> result = new ArrayList<>();
        Set<BasicBlock>  children = controlFlowGraph.getChildren(currentNode);
        for(BasicBlock child : children){
            // If a node is counted two times, we won't include it for for the further part of the path (Avoiding loops)

            if(coveredNodes.containsKey(child) && coveredNodes.get(child) == 2){
                continue;
            }
            if(!coveredNodes.containsKey(child)){
                coveredNodes.put(child,1);
            }else {
                coveredNodes.replace(child,coveredNodes.get(child)+1);
            }

            // If there is no path from child to the target node, we won't include it for for the further part of the path
            if(targetNode!= null && controlFlowGraph.getDistance(child,targetNode) < 0){
                continue;
            }

            List<List<BasicBlock>> furthurPath = makePath(child,controlFlowGraph,coveredNodes,targetNode);
            for (List<BasicBlock> path: furthurPath){
//                List tempPath = new ArrayList<>();
//                tempPath.addAll(path);
                result.add(path);
            }
        }

        if(result.size() == 0){
            result.add(new ArrayList<>());
        }

        for (List<BasicBlock> path: result){
            path.add(0,currentNode);
        }

        return result;
    }

}
