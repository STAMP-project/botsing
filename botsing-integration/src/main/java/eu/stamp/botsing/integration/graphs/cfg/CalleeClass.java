package eu.stamp.botsing.integration.graphs.cfg;

import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import eu.stamp.botsing.commons.graphs.cfg.BotsingRawControlFlowGraph;
import org.evosuite.coverage.branch.Branch;
import org.evosuite.coverage.branch.BranchPool;
import org.evosuite.graphs.GraphPool;
import org.evosuite.graphs.ccg.ClassCallGraph;
import org.evosuite.graphs.ccg.ClassCallNode;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.RawControlFlowGraph;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CalleeClass {
    private static final Logger LOG = LoggerFactory.getLogger(CalleeClass.class);
    private CFGGeneratorUtility utility = new CFGGeneratorUtility();

    protected ClassCallGraph ClassCallGraph;
    protected Map<String,ClassCallGraph> innerClassesCallGraphs = new HashMap<>();

    // call sites of the caller <methodName,List<BytecodeInstruction>>
    protected Map<String,Map<BytecodeInstruction,List<Type>>> callSites = new HashMap<>();

    protected Map<String,List<BytecodeInstruction>> returnPoints = new HashMap<>();

    // <callsite, branches>
    protected Map<BytecodeInstruction,Set<Branch>> branches = new HashMap<>();

    protected List<String> calledMethods =  new ArrayList<>();
    protected List<RawControlFlowGraph> involvedCFGs =  new ArrayList<>();
    private BotsingRawControlFlowGraph rawInterProceduralGraph;

    private Class originalClass;

    public CalleeClass(Class callee){
        ClassCallGraph = new ClassCallGraph(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT(),callee.getName());
        originalClass = callee;
    }

    private Map<String,Set<String>> detectCalledInnerMethods(RawControlFlowGraph methodRCFG, BytecodeInstruction callSiteBC, List<String> alreadyHandledInnerMethods) {
        String className = methodRCFG.getClassName();
        Map<String,Set<String>> interestingInnerMethods = new HashMap<>();
        for(BytecodeInstruction calls: methodRCFG.determineMethodCalls()){

            if(calls.getCalledMethodsClass().startsWith(className+"$")){
                String innerClass = calls.getCalledMethodsClass();
                String innerClassMethod = calls.getCalledMethod();

                if(!interestingInnerMethods.containsKey(innerClass) || !interestingInnerMethods.get(innerClass).contains(innerClassMethod)){
                    try {
                        if(Class.forName(innerClass,true,BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).isInterface()){
                            Set<String> loadedClasses = BotsingTestGenerationContext.getInstance().getClassLoaderForSUT().getLoadedClasses();
                            for (String loadedClass: loadedClasses){
                                if(loadedClass.startsWith(className+"$") && isExtendedBy(loadedClass,innerClass)){
                                    collectInterestingInnerMethods(loadedClass,innerClassMethod,interestingInnerMethods);
                                }
                            }

                        }else{
                            collectInterestingInnerMethods(innerClass,innerClassMethod,interestingInnerMethods);
                        }

                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        for (String innerClass: interestingInnerMethods.keySet()){
            for (String innerMethod: interestingInnerMethods.get(innerClass)){
                if(alreadyHandledInnerMethods.contains(innerClass+"."+innerMethod)){
                    continue;
                }
                RawControlFlowGraph innerMethodRCFG = GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).getRawCFG(innerClass,innerMethod);
                addBranchesOfCFG(innerMethodRCFG,callSiteBC);

                alreadyHandledInnerMethods.add(innerClass+"."+innerMethod);
            }

//            innerClassesCallGraphs.put(innerClass,)
        }

        return interestingInnerMethods;
    }

    private boolean isExtendedBy(String loadedClass,String innerClass) throws ClassNotFoundException {
        Class loadedClazz = Class.forName(loadedClass,true,BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
        for (Class interfaceClazz :loadedClazz.getInterfaces()){
            if(interfaceClazz.getName().contains("evosuite")){
                continue;
            }
            if(interfaceClazz.getName().equals(innerClass)){
                return true;
            }
        }
        return false;
    }

    private void collectInterestingInnerMethods(String innerClass, String innerClassMethod, Map<String,Set<String>> interestingInnerMethods) {

        if(GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).getRawCFG(innerClass,innerClassMethod) == null){
            return;
        }
        ClassCallGraph innerClassCallGraph = new ClassCallGraph(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT(),innerClass);
        List<ClassCallNode> nodesToCheck = new ArrayList<>();
        nodesToCheck.add(innerClassCallGraph.getNodeByMethodName(innerClassMethod));
        while (!nodesToCheck.isEmpty()){
            ClassCallNode currentNode = nodesToCheck.remove(0);

            if(!interestingInnerMethods.containsKey(innerClass)){
                interestingInnerMethods.put(innerClass,new HashSet<>());
            }
            interestingInnerMethods.get(innerClass).add(currentNode.getMethod());

            for(ClassCallNode child : innerClassCallGraph.getChildren(currentNode)){
                if(!interestingInnerMethods.get(innerClass).contains(child.getMethod())){
                    nodesToCheck.add(child);
                }
            }
        }
    }

    protected void setListOfInvolvedCFGs(Map<String,List<RawControlFlowGraph>> cfgs){
        Set<String> involvedMethods = new HashSet<>();
        for(String calledMethod: calledMethods){
            ClassCallNode calledMethodNode = ClassCallGraph.getNodeByMethodName(calledMethod);
            LinkedList<ClassCallNode> methodsToCheck = new LinkedList();
            methodsToCheck.addLast(calledMethodNode);
            involvedMethods.add(calledMethod);
            while (methodsToCheck.size() > 0){
                ClassCallNode currentNode = methodsToCheck.pop();
                for(ClassCallNode child: ClassCallGraph.getChildren(currentNode)){
                    if(!involvedMethods.contains(child.getMethod())){
                        methodsToCheck.addLast(child);
                        involvedMethods.add(child.getMethod());
                    }
                }
            }
        }
        for(RawControlFlowGraph rcfg : cfgs.get(ClassCallGraph.getClassName())){
            if(involvedMethods.contains(rcfg.getMethodName())){
                involvedCFGs.add(rcfg);
            }
        }
    }

    public BotsingRawControlFlowGraph getCalleesRawInterProceduralGraph() {
        if(this.rawInterProceduralGraph == null){
            rawInterProceduralGraph = utility.generateInterProceduralGraphOfClass(involvedCFGs);
        }
        return rawInterProceduralGraph;
    }

    public RawControlFlowGraph getSingleInvolvedCFG(String methodName){
        for(RawControlFlowGraph cfg : involvedCFGs){
            if(cfg.getMethodName().equals(methodName)){
                return cfg;
            }
        }
        LOG.warn("could not fine method {}",methodName);
        return null;
    }

    public List<RawControlFlowGraph> getInvolvedCFGs() {
        return involvedCFGs;
    }

    public String getClassName(){
        return originalClass.getName();
    }

    public void addCallSite(BytecodeInstruction bcInstruction){
        String methodName = bcInstruction.getMethodName();
        if(!this.callSites.containsKey(methodName)){
            this.callSites.put(methodName,new HashMap<>());
        }
        Type[] argTypes = Type.getArgumentTypes(bcInstruction.getMethodCallDescriptor());
        this.callSites.get(methodName).put(bcInstruction,Arrays.asList(argTypes));
    }


    public Map<String, List<BytecodeInstruction>> getReturnPoints() {
        return returnPoints;
    }

    // This method find the called methods in callee by the method which is passed as input
    public List<RawControlFlowGraph> getCalledMethodsBy(String methodName) {
        List<RawControlFlowGraph> result = new ArrayList<>();
        GraphPool graphPool = GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
        ClassCallNode calledMethodNode = ClassCallGraph.getNodeByMethodName(methodName);
        LinkedList<ClassCallNode> methodsToCheck = new LinkedList();
        methodsToCheck.addLast(calledMethodNode);

        // To avoid loops
        List<String> handled = new ArrayList<>();
        while (methodsToCheck.size() > 0){
            ClassCallNode currentNode = methodsToCheck.pop();
            if(handled.contains(currentNode.getMethod())){
                continue;
            }

            result.add(graphPool.getRawCFG(this.getClassName(),currentNode.getMethod()));
            handled.add(currentNode.getMethod());
            for(ClassCallNode called: ClassCallGraph.getChildren(currentNode)){
                methodsToCheck.addLast(called);
            }
        }


        return result;
    }

    // This method collects branches in the callee (from the called method and other methods in callee which are invoked because of the call site)
    public void collectBranches(BytecodeInstruction callSiteBC, List<String> alreadyHandledInnerMethods) {
        String methodName = callSiteBC.getCalledMethod();
        List<RawControlFlowGraph> calledMethods = getCalledMethodsBy(methodName);
        for(RawControlFlowGraph rcfg: calledMethods){
            detectCalledInnerMethods(rcfg,callSiteBC,alreadyHandledInnerMethods);
            // add Branches Of Regular Methods
            addBranchesOfCFG(rcfg,callSiteBC);
        }

    }

    private void addBranchesOfCFG(RawControlFlowGraph rcfg, BytecodeInstruction callSiteBC) {
        BranchPool branchPool = BranchPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
        for(BytecodeInstruction branchBC: rcfg.determineBranches()){
            if(branchPool.isKnownAsNormalBranchInstruction(branchBC)){
                Branch branch = branchPool.getBranchForInstruction(branchBC);
                addBranchesOfCallSite(branch,callSiteBC);
            }else if(branchPool.isKnownAsSwitchBranchInstruction(branchBC)){
                for (Branch branch: branchPool.getCaseBranchesForSwitch(branchBC)){
                    addBranchesOfCallSite(branch,callSiteBC);
                }
            }else{
//                    throw new IllegalStateException("branch "+branchBC.explain()+" is not switch or normal.");
            }
        }
    }

    private void addBranchesOfCallSite(Branch branch, BytecodeInstruction callSiteBC) {
        if(!branches.containsKey(callSiteBC)){
            branches.put(callSiteBC,new HashSet<>());
        }

        branches.get(callSiteBC).add(branch);
    }

    public Set<Branch> getBranchesOfCallSite(BytecodeInstruction callSiteBC) {
        if(!branches.containsKey(callSiteBC)) {
            return new HashSet<>();
        }

        return branches.get(callSiteBC);
    }
}
