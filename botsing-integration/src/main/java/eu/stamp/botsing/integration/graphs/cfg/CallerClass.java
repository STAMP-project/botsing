package eu.stamp.botsing.integration.graphs.cfg;

import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import eu.stamp.botsing.commons.graphs.cfg.BotsingRawControlFlowGraph;
import org.evosuite.graphs.ccg.ClassCallGraph;
import org.evosuite.graphs.ccg.ClassCallNode;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.RawControlFlowGraph;

import java.util.*;

import static eu.stamp.botsing.integration.graphs.cfg.CFGGeneratorUtility.isPrivateMethod;

public class CallerClass {
    private CFGGeneratorUtility utility = new CFGGeneratorUtility();

    protected ClassCallGraph ClassCallGraph;
    protected Set<String> privateMethods = new HashSet<>();

    // call sites of the caller <methodName,List<BytecodeInstruction>>
    protected Map<String,List<BytecodeInstruction>> callSites = new HashMap<>();

    protected List<RawControlFlowGraph> involvedCFGs =  new ArrayList<>();


    private BotsingRawControlFlowGraph rawInterProceduralGraph;


    public CallerClass(Class caller){
        ClassCallGraph = new ClassCallGraph(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT(),caller.getName());
    }

    public BotsingRawControlFlowGraph getCallersRawInterProceduralGraph(){
        if(this.rawInterProceduralGraph == null){
            rawInterProceduralGraph = utility.generateInterProceduralGraphOfClass(involvedCFGs);
        }
        return rawInterProceduralGraph;
    }


    protected void setListOfInvolvedCFGs(Map<String,List<RawControlFlowGraph>> cfgs){
        if(callSites == null){
            throw new IllegalArgumentException("There is no call_site from caller!");
        }

        Set<String> involvedMethods = new HashSet<>();
        for(String callerMethod: callSites.keySet()){
            ClassCallNode callerMethodNode = ClassCallGraph.getNodeByMethodName(callerMethod);
            LinkedList<ClassCallNode> methodsToCheck = new LinkedList();
            methodsToCheck.addLast(callerMethodNode);
            involvedMethods.add(callerMethod);
            while (methodsToCheck.size() > 0){
                ClassCallNode currentNode = methodsToCheck.pop();
                for(ClassCallNode parent: ClassCallGraph.getParents(currentNode)){
                    if(!involvedMethods.contains(parent.getMethod())){
                        methodsToCheck.addLast(parent);
                        involvedMethods.add(parent.getMethod());
                    }
                }
            }
        }

        // Here, we have name of the involved methods. So, we get their raw CFGs and register both involved CFGs and private involved methods
        for(RawControlFlowGraph rcfg : cfgs.get(ClassCallGraph.getClassName())){
            if(involvedMethods.contains(rcfg.getMethodName())){
                involvedCFGs.add(rcfg);
                if(isPrivateMethod(rcfg)){
                    privateMethods.add(rcfg.getMethodName());
                }
            }
        }
    }
}
