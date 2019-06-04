package eu.stamp.botsing.integration.graphs.cfg;

import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import eu.stamp.botsing.commons.graphs.cfg.BotsingRawControlFlowGraph;
import org.evosuite.graphs.ccg.ClassCallGraph;
import org.evosuite.graphs.ccg.ClassCallNode;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.RawControlFlowGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CalleeClass {
    private static final Logger LOG = LoggerFactory.getLogger(CalleeClass.class);
    private CFGGeneratorUtility utility = new CFGGeneratorUtility();

    protected ClassCallGraph ClassCallGraph;

    protected Map<String,List<BytecodeInstruction>> returnPoints = new HashMap<>();

    protected List<String> calledMethods =  new ArrayList<>();
    protected List<RawControlFlowGraph> involvedCFGs =  new ArrayList<>();
    private BotsingRawControlFlowGraph rawInterProceduralGraph;

    public CalleeClass(Class callee){
        ClassCallGraph = new ClassCallGraph(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT(),callee.getName());
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
}
