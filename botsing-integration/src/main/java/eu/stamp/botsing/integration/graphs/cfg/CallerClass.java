package eu.stamp.botsing.integration.graphs.cfg;

import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import eu.stamp.botsing.commons.graphs.cfg.BotsingRawControlFlowGraph;
import org.evosuite.graphs.ccg.ClassCallGraph;
import org.evosuite.graphs.ccg.ClassCallNode;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.RawControlFlowGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.objectweb.asm.Type;
import java.util.*;

import static eu.stamp.botsing.integration.graphs.cfg.CFGGeneratorUtility.isPrivateMethod;

public class CallerClass {

    private static final Logger LOG = LoggerFactory.getLogger(CallerClass.class);
    private CFGGeneratorUtility utility = new CFGGeneratorUtility();

    protected ClassCallGraph ClassCallGraph;
    protected Set<String> privateMethods = new HashSet<>();

    // call sites of the caller <methodName,List<BytecodeInstruction>>
    protected Map<String,Map<BytecodeInstruction,List<Type>>> callSites = new HashMap<>();

    public List<RawControlFlowGraph> involvedCFGs =  new ArrayList<>();

    protected Set<String> involvedMethods = new HashSet<>();


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

    protected void setListOfInvolvedCFGs(Map<String,List<RawControlFlowGraph>> cfgs){
        if(callSites == null){
            throw new IllegalArgumentException("There is no call_site from caller!");
        }

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

    public RawControlFlowGraph getMethodCFG(String methodName){
        for (RawControlFlowGraph cfg: involvedCFGs){
            if(cfg.getMethodName().equals(methodName)){
                return cfg;
            }
        }

        return null;
    }
}
