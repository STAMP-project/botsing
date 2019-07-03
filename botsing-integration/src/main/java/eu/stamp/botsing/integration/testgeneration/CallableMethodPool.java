package eu.stamp.botsing.integration.testgeneration;

import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import eu.stamp.botsing.integration.IntegrationTestingProperties;
import eu.stamp.botsing.integration.coverage.branch.BranchPairPool;
import org.evosuite.ga.Chromosome;
import org.evosuite.graphs.GraphPool;
import org.evosuite.graphs.ccg.ClassCallGraph;
import org.evosuite.graphs.ccg.ClassCallNode;
import org.evosuite.setup.TestCluster;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.statements.Statement;
import org.evosuite.utils.generic.GenericAccessibleObject;
import org.evosuite.utils.generic.GenericConstructor;
import org.evosuite.utils.generic.GenericMethod;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CallableMethodPool {
    private static final Logger LOG = LoggerFactory.getLogger(CallableMethodPool.class);
    private static CallableMethodPool instance;
    // pool of public/protected method calls which can be used to cover the integration points
    Set<GenericAccessibleObject<?>> callableMethods = new HashSet<>();

    private List<GenericAccessibleObject<?>> allMethods = new LinkedList<GenericAccessibleObject<?>>();

    private CallableMethodPool(){
        allMethods.addAll(TestCluster.getInstance().getTestCalls());
        String callerClass = IntegrationTestingProperties.TARGET_CLASSES[1];
        ClassCallGraph callGraph = new ClassCallGraph(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT(),callerClass);
        Set<String> methodsWithCallSite = BranchPairPool.getInstance().getSetOfMethodsWithCallSite();
        for (String methodName: methodsWithCallSite){
            collectPublicCallers(callGraph, methodName);
        }
    }

    public static CallableMethodPool getInstance(){
        if(instance == null){
            instance = new CallableMethodPool();
        }

        return instance;
    }


    private void collectPublicCallers(ClassCallGraph callGraph, String methodName) {
        GraphPool graphPool = GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());

        List<ClassCallNode> nodesToHandle = new ArrayList<>();
        nodesToHandle.add(callGraph.getNodeByMethodName(methodName));

        while (!nodesToHandle.isEmpty()){
            ClassCallNode currentNode = nodesToHandle.remove(0);
            // If the method is not private, we will add it as a callable class
            int methodAccess = graphPool.getActualCFG(callGraph.getClassName(),methodName).getMethodAccess();
            if(!isPrivateMethod(methodAccess)){
                GenericAccessibleObject<?> methodGenericObj = getGenericObject(currentNode.getMethod());
                if(methodGenericObj != null){
                    callableMethods.add(methodGenericObj);
                }
            }
            // Detecting the new callers
            for(ClassCallNode caller : callGraph.getParents(currentNode)){
                GenericAccessibleObject<?> callerMethodGenericObj = getGenericObject(caller.getMethod());
                // Avoiding Loop
                if(callerMethodGenericObj==null || callableMethods.contains(callerMethodGenericObj)){
                    continue;
                }
                nodesToHandle.add(caller);
            }
        }
    }


    private GenericAccessibleObject<?> getGenericObject(String method) {
        for(GenericAccessibleObject obj: allMethods){
            if(obj.isMethod()){
                String objMethodName = obj.getName()+Type.getMethodDescriptor(((GenericMethod) obj).getMethod());
                if(objMethodName.equals(method)){
                    return obj;
                }
            }else if (obj.isConstructor()){
                String objConstructorNameName = "<init>"+Type.getConstructorDescriptor(((GenericConstructor) obj).getConstructor());
                if(objConstructorNameName.equals(method)){
                    return obj;
                }
            }else{
                throw new IllegalStateException();
            }

        }
        LOG.info("method "+method+" is not detected!");
        return null;
    }

    private static boolean isPrivateMethod(int methodAccess){
        return (methodAccess & Opcodes.ACC_PRIVATE) == Opcodes.ACC_PRIVATE;
    }

    public List<GenericAccessibleObject<?>> getAllMethods(){
        return allMethods;
    }


    public Set<GenericAccessibleObject<?>> getCallableMethods() {
        return callableMethods;
    }

    public <T extends Chromosome> boolean includesPublicCall(TestChromosome candidateChrom) {
        Iterator<GenericAccessibleObject<?>> publicCallsIterator = CallableMethodPool.getInstance().getCallableMethods().iterator();
        TestCase candidate = candidateChrom.getTestCase();
        if (candidate.size() == 0){
            return false;
        }
        while (publicCallsIterator.hasNext()){
            GenericAccessibleObject<?> call = publicCallsIterator.next();
            for (int index= 0 ; index < candidate.size() ;index++) {
                Statement currentStatement = candidate.getStatement(index);
                if(currentStatement.getAccessibleObject() != null && currentStatement.getAccessibleObject().equals(call)){
                    return true;
                }
            }
        }
        return false;
    }
}
