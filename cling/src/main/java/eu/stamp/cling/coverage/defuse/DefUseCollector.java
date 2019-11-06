package eu.stamp.cling.coverage.defuse;

import eu.stamp.cling.IntegrationTestingProperties;
import eu.stamp.cling.graphs.cfg.CalleeClass;
import eu.stamp.cling.graphs.cfg.CallerClass;
import org.evosuite.graphs.cfg.*;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class DefUseCollector {
    private static final Logger LOG = LoggerFactory.getLogger(DefUseCollector.class);

    public static void analyze(RawControlFlowGraph controlFlowGraph, CallerClass caller, CalleeClass callee){
        collectLastDefs(controlFlowGraph,caller,callee);
//        caller.collectCallSiteParams();
//        doCallerStuff(controlFlowGraph,caller);
        String className = controlFlowGraph.getClassName();
        String methodName = controlFlowGraph.getMethodName();
        LOG.info("Collecting Def/Use for method {} in class {}",className,methodName);
        for (BytecodeInstruction instruction : controlFlowGraph.vertexSet()) {
//            instruction.isMethodCall()
//            if(instruction.isArrayLoadInstruction())


            if(instruction.isMethodCall() &&
                    (instruction.getCalledMethodsClass().equals(IntegrationTestingProperties.TARGET_CLASSES[0]) ||
                    instruction.getCalledMethodsClass().equals(IntegrationTestingProperties.TARGET_CLASSES[1]))
                    ){
                LOG.info("{} is a method call to callee.",instruction);

            }



            if(!instruction.isDefUse()){
                continue;
            }


            if(instruction.isUse()){
                LOG.info("{} is Use of variable {}",instruction,instruction.getVariableName());

            }


            if(instruction.isDefinition()){
                LOG.info("{} is Def of variable {}",instruction,instruction.getVariableName());
            }
        }

    }

    private static void collectLastDefs(RawControlFlowGraph controlFlowGraph, CallerClass caller, CalleeClass callee) {
//        collectLastDefsBeforeCall(controlFlowGraph,caller);
//        collectLastDefsBeforeReturn(controlFlowGraph,callee);
    }

    private static void collectLastDefsBeforeCall(CallerClass caller) {
        // method --> call_site --> paramIndex --> Set<Nodes>
        Map<String,Map<BytecodeInstruction,Map<Integer,Set<BasicBlock>>>> nodesForAllCouplingPaths = new HashMap<>();
//        Set<BytecodeInstruction> nodesForAllCouplingPaths = new HashSet<>();
        // method --> call_site --> variable --> Set<Edges>
        Map<String,Map<BytecodeInstruction,Map<String,Set<ControlFlowEdge>>>> edgesForAllCouplingPaths = new HashMap<>();
//        Set<ControlFlowEdge> edgesForAllCouplingPaths = new HashSet<>();

//        List<BytecodeInstruction> handled = new ArrayList<>();




        for (String method: caller.getInvolvedMethods()){
            Map<BytecodeInstruction,List<Type>> callSitesOfMethod = caller.getCallSitesOfMethod(method);
            if(callSitesOfMethod == null){
                LOG.info("method {} does not have call_site.",method);
                continue;
            }
            nodesForAllCouplingPaths.put(method,new HashMap<>());
            edgesForAllCouplingPaths.put(method,new HashMap<>());
            for (Map.Entry<BytecodeInstruction, List<Type>> entry : callSitesOfMethod.entrySet()) {
                List<BytecodeInstruction> parents = new ArrayList<>();
                BytecodeInstruction call_site = entry.getKey();
                List<Type> types = entry.getValue();
                nodesForAllCouplingPaths.get(method).put(call_site,new HashMap<>());
                edgesForAllCouplingPaths.get(method).put(call_site,new HashMap<>());
                List<String> varNames = detectVariableNames(call_site,types,caller.getMethodCFG(call_site.getMethodName()));
                int varCounter = 0;
                for(String var : varNames){
                    varCounter++;
                    if(var == null){
                        continue;
                    }
                    nodesForAllCouplingPaths.get(method).get(call_site).put(varCounter,new HashSet<>());
                    edgesForAllCouplingPaths.get(method).get(call_site).put(var,new HashSet<>());
                    // We perform this process for each varName
                    parents.clear();
                    parents.add(call_site);
                    while(!parents.isEmpty()){
                        // get the candidate node
                        BytecodeInstruction currentNode = parents.remove(0);
                        if(currentNode.getMethodName().equals(method)){
                            nodesForAllCouplingPaths.get(method).get(call_site).get(varCounter).add(currentNode.getBasicBlock());

                            if(currentNode.isDefinition() && currentNode.getVariableName().equals(var)){
                                // This node is a last_definition for the current variable
                                LOG.info("Node {} is the last definition for variable {} which is parameter #{} for call_site {}",currentNode,var,varCounter,call_site);
                                continue;
                            }
                        }

                        for (BytecodeInstruction parent: caller.getMethodCFG(method).getParents(currentNode)){
                            if(nodesForAllCouplingPaths.get(method).get(call_site).get(varCounter).contains(parent)){
                                continue;
                            }
                            if(!parent.getMethodName().equals(method)){
                                parents.add(parent);
                                continue;
                            }
                            ControlFlowEdge edgeToParent  =  caller.getMethodCFG(method).getEdge(parent,currentNode);
                            edgesForAllCouplingPaths.get(method).get(call_site).get(var).add(edgeToParent);
                            parents.add(parent);
                        }
                    }
                }
            }
        }


        IntegrationDefUsePool.getInstance().registerCallSitesLastDef(caller.getClassName(),nodesForAllCouplingPaths);
    }

    private static List<String> detectVariableNames(BytecodeInstruction call_site,List<Type> types, RawControlFlowGraph controlFlowGraph) {
        List<String> variableNames = new ArrayList<>();
        List<BytecodeInstruction> parents = new ArrayList<>();
        parents.addAll(controlFlowGraph.getParents(call_site));

        while (variableNames.size() != types.size()){
            BytecodeInstruction currentParent = parents.remove(0);
            if(currentParent.isUse() && currentParent.getMethodName().equals(call_site.getMethodName())){
                variableNames.add(currentParent.getVariableName());
            }else if (currentParent.isConstant() && currentParent.getMethodName().equals(call_site.getMethodName())){
                // One of the inputs is a constant value
                variableNames.add(null);
            }
            parents.addAll(controlFlowGraph.getParents(currentParent));
        }

        return variableNames;
    }

    private static void collectLastDefsBeforeReturn(CalleeClass callee) {
        String className = callee.getClassName();
        for (String methodName: callee.getReturnPoints().keySet()){
            for(BytecodeInstruction returnPoint: callee.getReturnPoints().get(methodName)){
                String variableName = detectReturnVariableName(returnPoint);
                Set<BasicBlock> setOfNodes = new HashSet<>();

                List<BytecodeInstruction> parents = new ArrayList<>();
                parents.add(returnPoint);

                while(!parents.isEmpty()){
                    BytecodeInstruction currentNode = parents.remove(0);
                    if(setOfNodes.contains(currentNode)){
                        // We already analyze this node. We will skip it.
                        continue;
                    }
                    setOfNodes.add(currentNode.getBasicBlock());
                    if(currentNode.isDefinition() && currentNode.getVariableName().equals(variableName)){
                        continue;
                        // we reached to the definition we will not continue in this path
                    }

                    parents.addAll(currentNode.getRawCFG().getParents(currentNode));
                }

                // register the nodes to the integration defUsePool
                IntegrationDefUsePool.getInstance().registerReturnsLastDefs(className,methodName,returnPoint,setOfNodes);
            }
        }
    }


    private static String detectReturnVariableName(BytecodeInstruction returnPoint){
        List<BytecodeInstruction> candidates = new ArrayList<>();
        candidates.add(returnPoint);
        while(!candidates.isEmpty()){
            BytecodeInstruction currentBC = candidates.remove(0);
            if(currentBC.isUse()){
                return currentBC.getVariableName();
            }
            candidates.addAll(currentBC.getRawCFG().getParents(currentBC));
        }
        return null;
    }


    public static void registerDefUsePaths(CallerClass caller, CalleeClass callee) {
        // Register first uses of input parameters in each method of callee
        registerUsesOfMethod(callee);
        LOG.info("last uses of methods of callee have been collected");

        // Register last Defs before each call_site in the caller
        collectLastDefsBeforeCall(caller);
        LOG.info("last defs of methods of caller have been collected");

        // Register last Defs before return in the callee
        collectLastDefsBeforeReturn(callee);
        LOG.info("last defs of return points of callee have been collected");

        // Register first uses after each call_site in the caller
        collectFirstUsesAfterCallSites(caller);
        LOG.info("First uses of call sites of caller have been collected");
        // We need the class call graphs of callee and caller.
    }

    private static void collectFirstUsesAfterCallSites(CallerClass caller) {
        for (String method: caller.getInvolvedMethods()) {
            Map<BytecodeInstruction, List<Type>> callSitesOfMethod = caller.getCallSitesOfMethod(method);
            if (callSitesOfMethod == null) {
                LOG.info("method {} does not have call_site.", method);
                continue;
            }


            for (BytecodeInstruction call_site : callSitesOfMethod.keySet()) {
                // First, we need to check if the call site used as a definition!
                String variableName = null;
                List<BytecodeInstruction> children = new ArrayList<>();
                children.add(call_site);
                while (!children.isEmpty()){
                    BytecodeInstruction currentInstruction = children.remove(0);
                    if(currentInstruction.getLineNumber() != call_site.getLineNumber()){
                        continue;
                    }
                    if(currentInstruction.isDefinition()){
                        variableName = currentInstruction.getVariableName();
                        break;
                    }
                    children.addAll(currentInstruction.getRawCFG().getChildren(currentInstruction));
                }
                if(variableName == null){
                    // there is no definition for the return value
                    continue;
                    // ToDo: What about cases that uses the return value directly?
                }

                Set<BasicBlock> nodeSet = new HashSet<>();

                children.clear();
                children.add(call_site);
                while (!children.isEmpty()){
                    BytecodeInstruction currentInstruction = children.remove(0);
                    if(nodeSet.contains(currentInstruction)){
                        continue;
                    }

                    nodeSet.add(currentInstruction.getBasicBlock());

                    if(currentInstruction.isUse() && currentInstruction.getVariableName().equals(variableName)){
                        // one last use is found. We will not add its children anymore.
                        continue;
                    }

                    children.addAll(currentInstruction.getRawCFG().getChildren(currentInstruction));
                }
                // Save the detected node set
                IntegrationDefUsePool.getInstance().registerReturnsFirstUses(caller.getClassName(),method,call_site,nodeSet);
            }

        }
    }

    private static void registerUsesOfMethod(CalleeClass callee) {
        for(RawControlFlowGraph methodRCFG: callee.getInvolvedCFGs()){
            String methodDesc = methodRCFG.getMethodName().substring(methodRCFG.getMethodName().indexOf('('));
            Type[] argTypes = Type.getArgumentTypes(methodDesc);

            for (int paramIndex = 1; paramIndex<=argTypes.length; paramIndex++){
                Set<BasicBlock> nodeSet = new HashSet<>();
                List<BytecodeInstruction> children = new ArrayList<>();
                children.add(methodRCFG.determineEntryPoint());
                while (!children.isEmpty()){
                    BytecodeInstruction currentInstruction = children.remove(0);
                    if(nodeSet.contains(currentInstruction)){
                        continue;
                    }
                    nodeSet.add(currentInstruction.getBasicBlock());
                    if(currentInstruction.isUse() && currentInstruction.getVariableName().endsWith("_LV_"+paramIndex)){
                        LOG.debug("{} is usage of parameter number {}.",currentInstruction,paramIndex);
                        continue;
                    }
                    children.addAll(currentInstruction.getRawCFG().getChildren(currentInstruction));
                }

                IntegrationDefUsePool.getInstance().registerMethodsFirstUses(callee.getClassName(),methodRCFG.getMethodName(),paramIndex,nodeSet);
            }
        }
    }
}
