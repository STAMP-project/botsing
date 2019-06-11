package eu.stamp.botsing.integration.graphs.cfg;

import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import eu.stamp.botsing.commons.graphs.cfg.BotsingActualControlFlowGraph;
import eu.stamp.botsing.commons.graphs.cfg.BotsingRawControlFlowGraph;
import eu.stamp.botsing.integration.IntegrationTestingProperties;
import org.evosuite.graphs.GraphPool;
import org.evosuite.graphs.cdg.ControlDependenceGraph;
import org.evosuite.graphs.cfg.*;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import org.objectweb.asm.Type;

public class CFGGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(CFGGenerator.class);

    protected Map<String,List<RawControlFlowGraph>> cfgs = new HashMap<>();
    private CFGGeneratorUtility utility = new CFGGeneratorUtility();

    CallerClass caller;
    CalleeClass callee;

    private BotsingRawControlFlowGraph rawInterProceduralGraph;
    private ActualControlFlowGraph actualInterProceduralGraph;
    private ControlDependenceGraph controlDependenceInterProceduralGraph;


    public CFGGenerator(Class caller, Class callee){
        this.caller =  new CallerClass(caller);
        utility.collectCFGS(caller,cfgs);

        this.callee =  new CalleeClass(callee);
        utility.collectCFGS(callee,cfgs);

        detectIntegrationPointsInCaller(caller,callee);

        this.caller.setListOfInvolvedCFGs(cfgs);
        this.callee.setListOfInvolvedCFGs(cfgs);
    }

    private void registerIndependetPaths() {
        // Collect independent paths of each involved method
        registerIndependetPathsOfMethod(this.caller.involvedCFGs);
        registerIndependetPathsOfMethod(this.callee.involvedCFGs);

        // Collect independent paths of each call_site
        registerIndependetPathsToCaLLSites(this.caller.involvedCFGs,this.caller.callSites);
        registerIndependetPathsToCaLLSites(this.callee.involvedCFGs,this.callee.callSites);
    }


    private void registerIndependetPathsToCaLLSites(List<RawControlFlowGraph> rawCFGs, Map<String,Map<BytecodeInstruction,List<Type>>> callSites){
        GraphPool graphPool = GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
        for (RawControlFlowGraph rcfg:rawCFGs){
            String methodName = rcfg.getMethodName();
            if(callSites.containsKey(methodName)){
                ActualControlFlowGraph actualControlFlowGraph = graphPool.getActualCFG(rcfg.getClassName(),rcfg.getMethodName());
                for(BytecodeInstruction CallSiteBCInst: callSites.get(methodName).keySet()){
                    if(CallSiteBCInst.getCalledMethodsClass().equals(this.callee.getClassName())){
                        BasicBlock callSiteBasicBlock = actualControlFlowGraph.getBlockOf(CallSiteBCInst);
                        PathsPool.getInstance().registerNewPathsToCallSite(rcfg.getClassName(),rcfg.getMethodName(),CallSiteBCInst,utility.detectIndependetPathsForMethod(actualControlFlowGraph,null, callSiteBasicBlock));
                        PathsPool.getInstance().registerNewPathsFromCallSite(rcfg.getClassName(),rcfg.getMethodName(),CallSiteBCInst,utility.detectIndependetPathsForMethod(actualControlFlowGraph,callSiteBasicBlock, null));
                    }
                }
            }
        }
    }


    private void registerIndependetPathsOfMethod(List<RawControlFlowGraph> rawCFGs){
        GraphPool graphPool = GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
        for (RawControlFlowGraph rcfg:rawCFGs){
            ActualControlFlowGraph actualControlFlowGraph = graphPool.getActualCFG(rcfg.getClassName(),rcfg.getMethodName());
            PathsPool.getInstance().registerNewPathsForMethod(rcfg.getClassName(),rcfg.getMethodName(),utility.detectIndependetPathsForMethod(actualControlFlowGraph,null, null));
        }
    }


    private void detectIntegrationPointsInCaller(Class caller, Class callee) {
        GraphPool graphPool = GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
        // Detect call_sites
        Map<String, RawControlFlowGraph> methodsGraphs = graphPool.getRawCFGs(caller.getName());
        if (methodsGraphs == null) {
            throw new IllegalStateException("Botsing could not detect any CFG in the caller class");
        }
        for (String methodName : methodsGraphs.keySet()) {
            for (BytecodeInstruction bcInstruction : methodsGraphs.get(methodName).determineMethodCalls()){
                if(bcInstruction.isMethodCallForClass(callee.getName()) || bcInstruction.isMethodCallForClass(caller.getName())){
                    if(!this.caller.callSites.containsKey(methodName)){
                        this.caller.callSites.put(methodName,new HashMap<>());
                    }
//                    HashMap<BytecodeInstruction,List<Type>> callSite = new HashMap<>();
                    Type[] argTypes = Type.getArgumentTypes(bcInstruction.getMethodCallDescriptor());
//                    callSite.put(bcInstruction,Arrays.asList(argTypes));
                    this.caller.callSites.get(methodName).put(bcInstruction,Arrays.asList(argTypes));
                    if(bcInstruction.isMethodCallForClass(callee.getName())){
                        this.callee.calledMethods.add(bcInstruction.getCalledMethod());
                    }
                }
            }
        }

        // detect return points
        methodsGraphs = graphPool.getRawCFGs(callee.getName());
        if (methodsGraphs == null) {
            throw new IllegalStateException("Botsing could not detect any CFG in the callee class");
        }
        for (String methodName : methodsGraphs.keySet()) {
            for (BytecodeInstruction bcInstruction : methodsGraphs.get(methodName).determineExitPoints()){
                if(bcInstruction.isReturn()){
                    if(!this.callee.returnPoints.containsKey(methodName)){
                        this.callee.returnPoints.put(methodName,new ArrayList<>());
                    }
                    this.callee.returnPoints.get(methodName).add(bcInstruction);
                }
            }

            for(BytecodeInstruction bcInstruction : methodsGraphs.get(methodName).determineMethodCalls()){
                if(bcInstruction.isMethodCallForClass(callee.getName())){
                    this.callee.addCallSite(bcInstruction);
                }
            }
        }
    }


    public void generateInterProceduralGraphs(){
        generateRawGraphs();
        LOG.info("Raw control flow graph is generated.");
        actualInterProceduralGraph = new BotsingActualControlFlowGraph(rawInterProceduralGraph);
        LOG.info("Actual control flow graph is generated.");
        GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).registerActualCFG(actualInterProceduralGraph);
        controlDependenceInterProceduralGraph = new ControlDependenceGraph(actualInterProceduralGraph);
        LOG.info("Control dependence graph is generated.");
        GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).registerControlDependence(controlDependenceInterProceduralGraph);

        logGeneratedCDG();
    }

    private void generateRawGraphs() {
        // 1- Make Inter-procedural cfg of caller and callee
        // 2- clone them in our rawInterProceduralGraph
        rawInterProceduralGraph = utility.makeBotsingRawControlFlowGraphObject();
        rawInterProceduralGraph.clone(caller.getCallersRawInterProceduralGraph());
        rawInterProceduralGraph.clone(callee.getCalleesRawInterProceduralGraph());
        // 3- add edges between classes
        for(Map.Entry<String, Map<BytecodeInstruction,List<Type>>> entry: caller.callSites.entrySet()){
            Set<BytecodeInstruction> callSitesInSameMethod = entry.getValue().keySet();
            for(BytecodeInstruction src : callSitesInSameMethod){
                String calledMethod = src.getCalledMethod();
                RawControlFlowGraph targetRCFG;
                if(src.getClassName().equals(caller.getClassName())){
                    targetRCFG = this.caller.getSingleInvolvedCFG(calledMethod);
                }else{
                    targetRCFG = this.callee.getSingleInvolvedCFG(calledMethod);
                }

                if(targetRCFG == null){
                    throw new IllegalStateException("could not fine the target rcfg");
                }
                BytecodeInstruction target = targetRCFG.determineEntryPoint();
                Set<BytecodeInstruction> exitPoints = targetRCFG.determineExitPoints();
                rawInterProceduralGraph.addInterProceduralEdge(src,target,exitPoints);
            }
        }
        // 4- Add fake entry point
        AbstractInsnNode fakeNode = new InsnNode(0);
        int instructionId =  Integer.MAX_VALUE;
        BytecodeInstruction fakceBc = new BytecodeInstruction(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT(), "IntegrationTestingGraph", "methodsIntegration", instructionId, -1, fakeNode);
        rawInterProceduralGraph.addVertex(fakceBc);
        rawInterProceduralGraph.addGeneralEntryPoint(fakceBc);
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


    public void generate() {
        for(IntegrationTestingProperties.FitnessFunction ff: IntegrationTestingProperties.fitnessFunctions){
            switch (ff){
                case Independent_Paths:
                    registerIndependetPaths();
                    break;
                case Regular_Branch_Coverage:
                    generateInterProceduralGraphs();
            }
        }
    }
}
