package eu.stamp.botsing.integration.coverage.defuse;

import org.evosuite.graphs.cfg.ActualControlFlowGraph;
import org.evosuite.graphs.cfg.BasicBlock;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.RawControlFlowGraph;

import java.util.*;

public class IntegrationDefUsePool {

    private static IntegrationDefUsePool instance;

    // set of nodes between entry point of a method to the first call uses: <ClassName,<MethodName,<ParameterIndex,Set<BasicBlock>>>>
    Map<String,Map<String,Map<Integer,Set<BasicBlock>>>> methodsFirstUses = new HashMap<>();
    // set of nodes between last defs and the call_sites: <ClassName,<MethodName,<call_site,<paramIndex,Set<BasicBlock>>>>>
    Map<String,Map<String,Map<BytecodeInstruction,Map<Integer,Set<BasicBlock>>>>> callSitesLastDefs = new HashMap<>();




    private IntegrationDefUsePool(){
    }

    public static IntegrationDefUsePool getInstance(){
        if(instance == null){
            instance = new IntegrationDefUsePool();
        }

        return instance;
    }

    public void registerMethodsFirstUses(ActualControlFlowGraph controlFlowGraph, int paramindex, BasicBlock targetNode){
        List<BasicBlock> detectedNodes = new ArrayList<>();
        List<BasicBlock> nodesToHandle = new ArrayList<>();
        nodesToHandle.add(controlFlowGraph.getEntryPoint().getBasicBlock());
        while (!nodesToHandle.isEmpty()){
            BasicBlock currentNode = nodesToHandle.remove(0);
            if(currentNode.equals(targetNode)){
                detectedNodes.add(currentNode);
                continue;
            }

            if(controlFlowGraph.getDistance(currentNode,targetNode) <0){
                // there is no path from current node to target node
                continue;
            }

            if(detectedNodes.contains(currentNode)){
                // We already count this one
                continue;
            }

            detectedNodes.add(currentNode);
            nodesToHandle.addAll(controlFlowGraph.getChildren(currentNode));
        }

        registerMethodsFirstUses(controlFlowGraph.getClassName(),controlFlowGraph.getMethodName(),paramindex,new HashSet<>(detectedNodes));
    }

    private void registerMethodsFirstUses(String className, String methodName, int paramindex, HashSet<BasicBlock> detectedNodes) {
        if(!methodsFirstUses.containsKey(className)){
            methodsFirstUses.put(className,new HashMap<>());
        }

        if(!methodsFirstUses.get(className).containsKey(methodName)){
            methodsFirstUses.get(className).put(methodName,new HashMap<>());
        }

        if(!methodsFirstUses.get(className).get(methodName).containsKey(paramindex)){
            methodsFirstUses.get(className).get(methodName).put(paramindex,detectedNodes);
        }

    }

    public void registerCallSitesLastDef(String className, Map<String,Map<BytecodeInstruction,Map<Integer,Set<BasicBlock>>>> nodesForAllCouplingPaths) {
        callSitesLastDefs.put(className,nodesForAllCouplingPaths);
    }
}
