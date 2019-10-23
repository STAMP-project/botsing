package eu.stamp.cling.coverage.defuse;

import org.evosuite.graphs.cfg.ActualControlFlowGraph;
import org.evosuite.graphs.cfg.BasicBlock;
import org.evosuite.graphs.cfg.BytecodeInstruction;

import java.util.*;

public class IntegrationDefUsePool {

    private static IntegrationDefUsePool instance;

    // set of nodes between entry point of a method to the first call uses: <ClassName,<MethodName,<ParameterIndex,Set<BasicBlock>>>>
    Map<String,Map<String,Map<Integer,Set<BasicBlock>>>> methodsFirstUses = new HashMap<>();
    // set of nodes between last defs and the call_sites: <ClassName,<MethodName,<call_site,<paramIndex,Set<BasicBlock>>>>>
    Map<String,Map<String,Map<BytecodeInstruction,Map<Integer,Set<BasicBlock>>>>> callSitesLastDefs = new HashMap<>();
    // set of nodes between last defs and the return point: <ClassName,<MethodName,<returnPoint,Set<BasicBlock>>>>
    Map<String,Map<String,Map<BytecodeInstruction, Set<BasicBlock>>>> returnsLastDefs = new HashMap<>();
    // set of nodes between call_sites and first uses: <ClassName,<MethodName,<call_site,Set<BasicBlock>>>>
    Map<String,Map<String,Map<BytecodeInstruction, Set<BasicBlock>>>> returnsFirstUses = new HashMap<>();



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

    public void registerMethodsFirstUses(String className, String methodName, int paramindex, Set<BasicBlock> detectedNodes) {
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

    public void registerReturnsLastDefs(String className, String methodName, BytecodeInstruction returnPoint, Set<BasicBlock> nodes){
        if(!returnsLastDefs.containsKey(className)){
            returnsLastDefs.put(className,new HashMap<>());
        }

        if(!returnsLastDefs.get(className).containsKey(methodName)){
            returnsLastDefs.get(className).put(methodName,new HashMap<>());
        }

        if(!returnsLastDefs.get(className).get(methodName).containsKey(returnPoint)){
            returnsLastDefs.get(className).get(methodName).put(returnPoint,nodes);
        }
    }

    public void registerReturnsFirstUses(String className, String methodName, BytecodeInstruction callSites, Set<BasicBlock> nodes){
        if(!returnsFirstUses.containsKey(className)){
            returnsFirstUses.put(className,new HashMap<>());
        }

        if(!returnsFirstUses.get(className).containsKey(methodName)){
            returnsFirstUses.get(className).put(methodName,new HashMap<>());
        }

        if(!returnsFirstUses.get(className).get(methodName).containsKey(callSites)){
            returnsFirstUses.get(className).get(methodName).put(callSites,nodes);
        }
    }

    public void registerCallSitesLastDef(String className, Map<String,Map<BytecodeInstruction,Map<Integer,Set<BasicBlock>>>> nodesForAllCouplingPaths) {
        callSitesLastDefs.put(className,nodesForAllCouplingPaths);
    }
}
