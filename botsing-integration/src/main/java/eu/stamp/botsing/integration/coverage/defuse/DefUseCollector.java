package eu.stamp.botsing.integration.coverage.defuse;

import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import eu.stamp.botsing.integration.IntegrationTestingProperties;
import eu.stamp.botsing.integration.graphs.cfg.CalleeClass;
import eu.stamp.botsing.integration.graphs.cfg.CallerClass;
import org.evosuite.graphs.GraphPool;
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
        collectLastDefsBeforeReturn(controlFlowGraph,callee);
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

    private static void collectLastDefsBeforeReturn(RawControlFlowGraph controlFlowGraph, CalleeClass callee) {
    }


    public static void registerDefUsePaths(CallerClass caller, CalleeClass callee) {
        // Register first uses of input parameters in each method of callee
        registerUsesOfMethod(callee);
        LOG.info("last uses of methods of callee have been collected");

        // Register last Defs before each call_site in the caller
        collectLastDefsBeforeCall(caller);
        LOG.info("last defs of methods of caller have been collected");

        // Register last Defs before return in the callee and caller (for passed values) !



        // Register first uses after each call_site in the caller

        // We need the class call graphs of callee and caller.
    }

    private static void registerUsesOfMethod(CalleeClass callee) {
        for(RawControlFlowGraph methodRCFG: callee.getInvolvedCFGs()){
            String methodDesc = methodRCFG.getMethodName().substring(methodRCFG.getMethodName().indexOf('('));
            Type[] argTypes = Type.getArgumentTypes(methodDesc);

            for (BytecodeInstruction instruction: methodRCFG.vertexSet()){
                if(instruction.isUse()){
                    String varName = instruction.getVariableName();
                    for (int paramIndex = 1; paramIndex<=argTypes.length; paramIndex++){
                        if(varName.endsWith("_LV_"+paramIndex)){
                            LOG.debug("{} is usage of parameter number {}.",instruction,paramIndex);
                            ActualControlFlowGraph actualControlFlowGraph = GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).getActualCFG(methodRCFG.getClassName(),methodRCFG.getMethodName());
                            IntegrationDefUsePool.getInstance().registerMethodsFirstUses(actualControlFlowGraph,paramIndex,instruction.getBasicBlock());
                        }
                    }
                }
            }
        }

    }
}
