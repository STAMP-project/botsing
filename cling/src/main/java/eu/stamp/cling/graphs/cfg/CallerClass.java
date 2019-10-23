package eu.stamp.cling.graphs.cfg;

import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import eu.stamp.botsing.commons.graphs.cfg.BotsingRawControlFlowGraph;
import org.evosuite.coverage.branch.Branch;
import org.evosuite.coverage.branch.BranchPool;
import org.evosuite.graphs.GraphPool;
import org.evosuite.graphs.ccg.ClassCallGraph;
import org.evosuite.graphs.ccg.ClassCallNode;
import org.evosuite.graphs.cdg.ControlDependenceGraph;
import org.evosuite.graphs.cfg.BasicBlock;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.ControlDependency;
import org.evosuite.graphs.cfg.RawControlFlowGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.objectweb.asm.Type;
import java.util.*;

import static eu.stamp.cling.graphs.cfg.CFGGeneratorUtility.isPrivateMethod;

public class CallerClass {

    private static final Logger LOG = LoggerFactory.getLogger(CallerClass.class);
    private CFGGeneratorUtility utility = new CFGGeneratorUtility();

    protected ClassCallGraph classCallGraph;
    protected Set<String> privateMethods = new HashSet<>();

    // <callsite, branches>
    protected Map<BytecodeInstruction,Set<Branch>> branchesAfterCallSite = new HashMap<>();
    protected Map<BytecodeInstruction,Set<Branch>> branchesBeforeCallSite = new HashMap<>();
    protected Map<BytecodeInstruction,Set<ControlDependency>> controlDependenciesOfCallSite = new HashMap<>();

    // call sites of the caller <methodName,List<BytecodeInstruction>>
    protected Map<String,Map<BytecodeInstruction,List<Type>>> callSites = new HashMap<>();

    public List<RawControlFlowGraph> involvedCFGs =  new ArrayList<>();

    protected Set<String> involvedMethods = new HashSet<>();


    private BotsingRawControlFlowGraph rawInterProceduralGraph;

    private Class originalClass;


    public CallerClass(Class caller){
        classCallGraph = new ClassCallGraph(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT(),caller.getName());
        originalClass = caller;
    }

    public BotsingRawControlFlowGraph getCallersRawInterProceduralGraph(){
        if(this.rawInterProceduralGraph == null){
            rawInterProceduralGraph = utility.generateInterProceduralGraphOfClass(involvedCFGs);
        }
        return rawInterProceduralGraph;
    }

    public Set<String> getInvolvedMethods(){
        if(involvedMethods.size() == 0){
            throw new IllegalStateException("involved methods in caller is empty!");
        }
        return involvedMethods;
    }

    public Map<BytecodeInstruction,List<Type>> getCallSitesOfMethod(String method){
        if(callSites.containsKey(method)){
            return callSites.get(method);
        }
        return null;
    }

    public void collectBranchesInSameHierarchy(String methodName, BytecodeInstruction callSiteBC, BytecodeInstruction rootCallSiteBC, Set<String> handledMethods){
        GraphPool graphPool = GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
        // Collect control dependencies of call site
        ControlDependenceGraph methodCDG = new ControlDependenceGraph(graphPool.getActualCFG(this.getClassName(),methodName));
        BasicBlock callSiteBlock = callSiteBC.getBasicBlock();
        Map<BytecodeInstruction,Boolean> controlDependencies = new HashMap();
        List<ControlDependency> dependenciesToCheck = new ArrayList<>();
        dependenciesToCheck.addAll(methodCDG.getControlDependentBranches(callSiteBlock));

        while (!dependenciesToCheck.isEmpty()){
            ControlDependency currentCD = dependenciesToCheck.remove(0);
            controlDependencies.put(currentCD.getBranch().getInstruction(),currentCD.getBranchExpressionValue());

        }

    }

    public void collectBranches(String methodName, BytecodeInstruction callSiteBC, BytecodeInstruction rootCallSiteBC, Set<String> handledMethods) {

        GraphPool graphPool = GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
        // Collect control dependencies of call site
        ControlDependenceGraph methodCDG = new ControlDependenceGraph(graphPool.getActualCFG(this.getClassName(),methodName));

        BasicBlock callSiteBlock = callSiteBC.getBasicBlock();
        Map<BytecodeInstruction,Boolean> controlDependencies = new HashMap();
        List<ControlDependency> dependenciesToCheck = new ArrayList<>();
        dependenciesToCheck.addAll(methodCDG.getControlDependentBranches(callSiteBlock));

        while (!dependenciesToCheck.isEmpty()){
            ControlDependency currentCD = dependenciesToCheck.remove(0);
            controlDependencies.put(currentCD.getBranch().getInstruction(),currentCD.getBranchExpressionValue());
            for(ControlDependency directCDs:methodCDG.getControlDependentBranches(currentCD.getBranch().getInstruction().getBasicBlock())){
                if(!controlDependencies.keySet().contains(directCDs.getBranch().getInstruction())){
                    dependenciesToCheck.add(directCDs);
                }
            }
        }

        RawControlFlowGraph methodsRCFG = graphPool.getRawCFG(this.getClassName(),methodName);
        for(BytecodeInstruction branch: methodsRCFG.determineBranches()){
            if(methodsRCFG.getDistance(branch,callSiteBC)>=0){
                LOG.debug("Branch {} should be registered as a pre branch for call site {}.",branch.explain(),callSiteBC.explain());
                if(controlDependencies.keySet().contains(branch)){
                    addControlDependenciesOfCallSite(rootCallSiteBC, branch,controlDependencies.get(branch));
                }else{
                    addBranchesBeforeCallSite(rootCallSiteBC, branch);
                }
            }else if(methodsRCFG.getDistance(callSiteBC,branch)>=0){
                LOG.debug("Branch {} should be registered as a post branch for call site {}.",branch.explain(),callSiteBC.explain());
                addBranchesAfterCallSite(rootCallSiteBC, branch);
            }else{
                LOG.debug("There is no link between branch {} and call site {}.",branch.explain(),callSiteBC.explain());
            }
        }
        handledMethods.add(methodName);
        // invoke this method for caller methods
        collectBranchesOfParents(methodName,rootCallSiteBC,handledMethods);
    }

    private void addControlDependenciesOfCallSite(BytecodeInstruction callSiteBC, BytecodeInstruction branch, Boolean expression) {
        BranchPool branchPool = BranchPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
        if(!controlDependenciesOfCallSite.containsKey(callSiteBC)){
            controlDependenciesOfCallSite.put(callSiteBC,new HashSet<>());
        }

        if(branchPool.isKnownAsNormalBranchInstruction(branch)){
            controlDependenciesOfCallSite.get(callSiteBC).add(new ControlDependency(branchPool.getBranchForInstruction(branch),expression));
        }else{
            for(Branch switchBranch: branchPool.getCaseBranchesForSwitch(branch)){
                controlDependenciesOfCallSite.get(callSiteBC).add(new ControlDependency(switchBranch,expression));
            }

        }
    }


    protected void setListOfInvolvedCFGsInHierarchy(boolean isSubClass, Class callee){
        GraphPool graphPool = GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
        for(String methodsWithCallSite: callSites.keySet()){
            List<ClassCallNode> nodesToCheck = new ArrayList<>();
            nodesToCheck.add(classCallGraph.getNodeByMethodName(methodsWithCallSite));
            while(!nodesToCheck.isEmpty()){
                ClassCallNode currentNode = nodesToCheck.remove(0);
                RawControlFlowGraph rcfg = graphPool.getRawCFG(this.getClassName(),currentNode.getMethod());
                if(!involvedCFGs.contains(rcfg)){
                    involvedCFGs.add(rcfg);
                    for(ClassCallNode parent: classCallGraph.getParents(currentNode)){
                        Set<String> methodsInCallee = graphPool.getRawCFGs(callee.getName()).keySet();
                        if(isSubClass || !methodsInCallee.contains(parent.getMethod())){
                            nodesToCheck.add(parent);
                        }
                    }
                }
            }
        }
    }

    private void collectBranchesOfParents(String methodName, BytecodeInstruction rootCallSiteBC, Set<String> handledMethods) {
        GraphPool graphPool = GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
        ClassCallNode callerMethodNode = classCallGraph.getNodeByMethodName(methodName);
        for(ClassCallNode parent: classCallGraph.getParents(callerMethodNode)){
            if(handledMethods.contains(parent.getMethod())){
                continue;
            }
            RawControlFlowGraph parentRCFG = graphPool.getRawCFG(this.getClassName(),parent.getMethod());
            for(BytecodeInstruction methodCall: parentRCFG.determineMethodCalls()){
                if(methodCall.getCalledMethod().equals(methodName)){
                    // collect branches of the caller methods which can impact the coverage of call site
                    collectBranches(parent.getMethod(),methodCall,rootCallSiteBC,handledMethods);
                }
            }
        }
    }

    private void addBranchesAfterCallSite(BytecodeInstruction callSiteBC, BytecodeInstruction branch) {
        BranchPool branchPool = BranchPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
        if(!branchesAfterCallSite.containsKey(callSiteBC)){
            branchesAfterCallSite.put(callSiteBC,new HashSet<>());
        }

        if(branchPool.isKnownAsNormalBranchInstruction(branch)){
            branchesAfterCallSite.get(callSiteBC).add(branchPool.getBranchForInstruction(branch));
        }else if(branchPool.isKnownAsSwitchBranchInstruction(branch)){
            for(Branch switchBranch:branchPool.getCaseBranchesForSwitch(branch)){
                branchesAfterCallSite.get(callSiteBC).add(switchBranch);
            }
        }
    }

    private void addBranchesBeforeCallSite(BytecodeInstruction callSiteBC, BytecodeInstruction branch) {
        BranchPool branchPool = BranchPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
        if(!branchesBeforeCallSite.containsKey(callSiteBC)){
            branchesBeforeCallSite.put(callSiteBC,new HashSet<>());
        }
        if(branchPool.isKnownAsNormalBranchInstruction(branch)){
            branchesBeforeCallSite.get(callSiteBC).add(branchPool.getBranchForInstruction(branch));
        }else if(branchPool.isKnownAsSwitchBranchInstruction(branch)){
            for(Branch switchBranch:branchPool.getCaseBranchesForSwitch(branch)){
                branchesBeforeCallSite.get(callSiteBC).add(switchBranch);
            }
        }

    }

    protected void setListOfInvolvedCFGs(Map<String,List<RawControlFlowGraph>> cfgs){
        if(callSites == null){
            throw new IllegalArgumentException("There is no call_site from caller!");
        }

        for(String callerMethod: callSites.keySet()){
            ClassCallNode callerMethodNode = classCallGraph.getNodeByMethodName(callerMethod);
            LinkedList<ClassCallNode> methodsToCheck = new LinkedList();
            methodsToCheck.addLast(callerMethodNode);
            involvedMethods.add(callerMethod);
            while (methodsToCheck.size() > 0){
                ClassCallNode currentNode = methodsToCheck.pop();
                for(ClassCallNode parent: classCallGraph.getParents(currentNode)){
                    if(!involvedMethods.contains(parent.getMethod())){
                        methodsToCheck.addLast(parent);
                        involvedMethods.add(parent.getMethod());
                    }
                }
            }
        }

        // Here, we have name of the involved methods. So, we get their raw CFGs and register both involved CFGs and private involved methods
        for(RawControlFlowGraph rcfg : cfgs.get(classCallGraph.getClassName())){
            if(involvedMethods.contains(rcfg.getMethodName())){
                involvedCFGs.add(rcfg);
                if(isPrivateMethod(rcfg)){
                    privateMethods.add(rcfg.getMethodName());
                }
            }
        }
    }

    public RawControlFlowGraph getMethodCFG(String methodName){
        for (RawControlFlowGraph cfg: involvedCFGs){
            if(cfg.getMethodName().equals(methodName)){
                return cfg;
            }
        }

        return null;
    }

    public String getClassName(){
        return originalClass.getName();
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

    public Set<ControlDependency> getControlDependenciesOfCallSite(BytecodeInstruction callSite){
        if(!controlDependenciesOfCallSite.containsKey(callSite)){
            return new HashSet<>();
        }
        return controlDependenciesOfCallSite.get(callSite);
    }

    public Set<Branch> getBranchesAfterCallSite(BytecodeInstruction callSiteBC) {
        if(!branchesAfterCallSite.containsKey(callSiteBC)) {
            return new HashSet<>();
        }
        return branchesAfterCallSite.get(callSiteBC);
    }

    public void addCallSite(String methodName, BytecodeInstruction bcInstruction){
        if(!this.callSites.containsKey(methodName)){
            this.callSites.put(methodName,new HashMap<>());
        }
        Type[] argTypes = Type.getArgumentTypes(bcInstruction.getMethodCallDescriptor());
        this.callSites.get(methodName).put(bcInstruction,Arrays.asList(argTypes));
    }

    public Class getOriginalClass() {
        return originalClass;
    }
}
