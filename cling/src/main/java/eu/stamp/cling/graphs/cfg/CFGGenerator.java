package eu.stamp.cling.graphs.cfg;

import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import eu.stamp.botsing.commons.graphs.cfg.BotsingActualControlFlowGraph;
import eu.stamp.botsing.commons.graphs.cfg.BotsingRawControlFlowGraph;
import eu.stamp.cling.IntegrationTestingProperties;
import eu.stamp.cling.coverage.branch.BranchPairPool;
import eu.stamp.cling.coverage.defuse.DefUseCollector;
import eu.stamp.cling.integrationtesting.IntegrationTestingUtility;
import org.evosuite.coverage.branch.Branch;
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

        Class parentInHierarchyTree = IntegrationTestingUtility.detectParentInHierarchyTree(caller,callee);
        if(parentInHierarchyTree == null){
            detectIntegrationPointsInCaller(caller,callee);
            this.caller.setListOfInvolvedCFGs(cfgs);
            this.callee.setListOfInvolvedCFGs();
        }else{
            detectIntegrationPointsInHierarchyTree(caller,callee, parentInHierarchyTree);
            setListOfInvolvedCFGsInCaller(parentInHierarchyTree);
            setListOfInvolvedCFGsInCallee(parentInHierarchyTree);
        }
    }

    // This method and next one are only executed if we caller and callee are in the same hierarchy tree
    private void setListOfInvolvedCFGsInCaller(Class parentInHierarchyTree) {
        if(this.caller.getOriginalClass().equals(parentInHierarchyTree)){
            // caller is super class
            caller.setListOfInvolvedCFGsInHierarchy(false,callee.getOriginalClass());
        }else{
            // caller is sub class
            caller.setListOfInvolvedCFGsInHierarchy(true,callee.getOriginalClass());
        }
    }

    private void setListOfInvolvedCFGsInCallee(Class parentInHierarchyTree) {
        if(this.callee.getOriginalClass().equals(parentInHierarchyTree)){
            // callee is super class
            callee.setListOfInvolvedCFGsInHierarchy(false,caller.getOriginalClass());
        }else{
            // callee is sub class
            callee.setListOfInvolvedCFGsInHierarchy(true,caller.getOriginalClass());
        }
    }


    private void registerIndependetPaths() {
        // Collect independent paths of each involved method
//        registerIndependetPathsOfMethod(this.caller.involvedCFGs);
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


    private void detectIntegrationPointsInHierarchyTree(Class caller, Class callee, Class parentInHierarchyTree) {
        GraphPool graphPool = GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
        if(caller.equals(parentInHierarchyTree)){
            // caller is super class, and callee is sub class
            Map<String, RawControlFlowGraph> methodsGraphs = graphPool.getRawCFGs(caller.getName());
            if (methodsGraphs == null) {
                throw new IllegalStateException("Botsing could not detect any CFG in the caller class");
            }

            Set<String> calleeMethods = graphPool.getRawCFGs(callee.getName()).keySet();
            for (String methodName : methodsGraphs.keySet()) {
                for (BytecodeInstruction bcInstruction : methodsGraphs.get(methodName).determineMethodCalls()){
                    String calledMethod = bcInstruction.getCalledMethod();
                    String calledClass = bcInstruction.getCalledMethodsClass();

                    if(!calledClass.equals(this.caller.getClassName()) && !calledClass.equals(this.callee.getClassName())){
                        continue;
                    }
                    // Check if called method is overridden in the sub class
                    if(calleeMethods.contains(calledMethod)){
                        this.caller.addCallSite(methodName,bcInstruction);
                        // Add called method to callee
                        this.callee.calledMethods.add(calledMethod);
                    }
                }
            }
        }else if(callee.equals(parentInHierarchyTree)){
            // caller is sub class, and callee is super class
            Map<String, RawControlFlowGraph> methodsGraphs = graphPool.getRawCFGs(caller.getName());
            if (methodsGraphs == null) {
                throw new IllegalStateException("Botsing could not detect any CFG in the caller class");
            }

            Set<String> calleeMethods = graphPool.getRawCFGs(callee.getName()).keySet();
            for (String methodName : methodsGraphs.keySet()) {
                for (BytecodeInstruction bcInstruction : methodsGraphs.get(methodName).determineMethodCalls()){
                    String calledMethod = bcInstruction.getCalledMethod();
                    // Check if called method is not overridden in the sub class
                    if(!methodsGraphs.keySet().contains(calledMethod) && calleeMethods.contains(calledMethod)){
                        LOG.debug("** the class name is {}, and the method name is {}. They are all in method {}",bcInstruction.getCalledMethodsClass(),calledMethod,bcInstruction.getMethodName());
                        this.caller.addCallSite(methodName,bcInstruction);
                        // Add called method to callee
                        this.callee.calledMethods.add(calledMethod);
                    }
                }
            }


        }else{
            throw new IllegalStateException("Caller and callee are not in the same hierarchy tree.");
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
                if(bcInstruction.isMethodCallForClass(callee.getName()) || IntegrationTestingUtility.isExtendedBy(callee.getName(),bcInstruction.getCalledMethodsClass())){
                    this.caller.addCallSite(methodName,bcInstruction);

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
                    throw new IllegalStateException("could not find the target rcfg");
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
                case Use_Def:
                    DefUseCollector.registerDefUsePaths(caller,callee);
                    break;
                case Branch_Pairs:
                    registerBranchPairs();
                    break;
                case Regular_Branch_Coverage:
                    generateInterProceduralGraphs();
            }
        }
    }

    private void registerBranchPairs() {
        // Collect interesting branches for each integration call_site
        for(String methodInCaller: this.caller.callSites.keySet()){
            Map<BytecodeInstruction,List<Type>> callSites = this.caller.callSites.get(methodInCaller);
            // collect caller-side branches
            for(BytecodeInstruction callSiteBC: callSites.keySet()){
                this.caller.collectBranches(methodInCaller,callSiteBC,callSiteBC, new HashSet<>());
            }

            // collect callee-side branches
            List<String> alreadyHandledInnerMethods = new ArrayList<>();
            for(BytecodeInstruction callSiteBC: callSites.keySet()){
                this.callee.collectBranches(callSiteBC,alreadyHandledInnerMethods);
            }
        }

        // Detect pairs and register them
        collectPairs();
        LOG.info("Branch pairs are registered.");
    }

    private void collectPairs() {
        for (Map<BytecodeInstruction,List<Type>> callSite : caller.callSites.values()) {
            for(BytecodeInstruction callSiteBC: callSite.keySet()){
                // collect pairs for each call site
                // collect control dependent branches before the call site
                Set<ControlDependency> callerSideBranchesBeforeCallSite = this.caller.getControlDependenciesOfCallSite(callSiteBC);
                // collect branches after call site in the caller method
                Set<Branch> callerSideBranchesAfterCallSite = this.caller.getBranchesAfterCallSite(callSiteBC);
                // collect branches in the called methods (by call site) of callee
                Set<Branch> calleeSideBranches = this.callee.getBranchesOfCallSite(callSiteBC);

                // Handle empty lists of branches
                if(callerSideBranchesBeforeCallSite.isEmpty() && callerSideBranchesAfterCallSite.isEmpty() && calleeSideBranches.isEmpty()){
                    LOG.warn("There is no branch fo call_site {}",callSiteBC.explain());
                }

                if(callerSideBranchesBeforeCallSite.isEmpty()){
                    callerSideBranchesBeforeCallSite.add(null);
                }

                if(callerSideBranchesAfterCallSite.isEmpty()){
                    callerSideBranchesAfterCallSite.add(null);
                }

                if(calleeSideBranches.isEmpty()){
                    calleeSideBranches.add(null);
                }
                // register pairs of the branches before call site and branches in callee
                for(ControlDependency callerCD : callerSideBranchesBeforeCallSite){
                    for (Branch calleeBranch : calleeSideBranches){
                        if(callerCD != null){
                            BranchPairPool.getInstance().addPair(callerCD.getBranch(),calleeBranch,callSiteBC,callerCD.getBranchExpressionValue());
                        }else if(calleeBranch != null){
                            BranchPairPool.getInstance().addPair(null,calleeBranch,callSiteBC,false);
                        }

                    }
                }
                // register pairs of branches in the callee and branches after return
                for(Branch calleeBranch : calleeSideBranches){
                    for (Branch callerBranch : callerSideBranchesAfterCallSite){
                        if(calleeBranch == null && callerBranch == null){
                            continue;
                        }
                        BranchPairPool.getInstance().addPair(calleeBranch,callerBranch,callSiteBC);
                    }
                }

            }
        }
    }
}
