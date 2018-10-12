package eu.stamp.botsing.ga.strategy.operators;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.StackTrace;
import eu.stamp.botsing.fitnessfunction.FitnessFunctionHelper;
import org.evosuite.TestGenerationContext;
import org.evosuite.graphs.cfg.ActualControlFlowGraph;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.BytecodeInstructionPool;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.statements.ConstructorStatement;
import org.evosuite.testcase.statements.MethodStatement;
import org.evosuite.testcase.statements.Statement;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.evosuite.ga.Chromosome;

import java.util.*;

public class GuidedSearchUtility<T extends Chromosome> {

    private static final Logger LOG = LoggerFactory.getLogger(GuidedSearchUtility.class);

    public static Set<String> publicCalls = new HashSet<String>();

    public boolean includesPublicCall (T individual) {
        Iterator<String> publicCallsIterator = publicCalls.iterator();
        TestChromosome candidateChrom = (TestChromosome) individual;
        TestCase candidate = candidateChrom.getTestCase();
        if (candidate.size() == 0){
            return false;
        }
        while (publicCallsIterator.hasNext()){
            String callName = publicCallsIterator.next();
            for (int index= 0 ; index < candidate.size() ;index++) {
                Statement currentStatement = candidate.getStatement(index);
                if (!callName.contains(".") && currentStatement instanceof MethodStatement) {
                    MethodStatement candidateMethod = (MethodStatement) candidate.getStatement(index);
                    if (candidateMethod.getMethodName().equalsIgnoreCase(callName)) {
                        return true;
                    }
                } else if (callName.contains(".") && currentStatement instanceof ConstructorStatement){
                    if (callName.equals(((ConstructorStatement) currentStatement).getDeclaringClassName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public Set<String> getPublicCalls() {
        StackTrace givenStackTrace = CrashProperties.getInstance().getStackTrace();
        String targetClass = givenStackTrace.getTargetClass();

        int targetLine = givenStackTrace.getTargetLine();
        List<BytecodeInstruction> instructions = BytecodeInstructionPool.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT()).getInstructionsIn(targetClass);
        BytecodeInstruction targetInstruction = getTargetInstruction(instructions, targetLine);

        if (targetInstruction.getActualCFG().isPublicMethod() ||
                isProtectedMethod(targetInstruction.getActualCFG())){
            LOG.info("The target method is public!");

            if(FitnessFunctionHelper.isConstructor(targetInstruction)){
                LOG.info("The target is a constructor!");
                publicCalls.add(targetClass);
            } else {
                LOG.info("The target is a method!");
                publicCalls.add(cleanMethodName(targetInstruction.getMethodName()));
            }

        } else {
            // The target call is private
            LOG.info("The target call '{}' is private!", targetInstruction.getMethodName());
            LOG.info("Searching for public callers");
            searchForNonPrivateMethods(targetInstruction, targetClass);
        }

        LOG.info("Botsing found "+publicCalls.size()+" Target call(s):");
        Iterator<String> iterateParents = publicCalls.iterator();

        while (iterateParents.hasNext()) {
            String nextCall = iterateParents.next();
            LOG.info(nextCall);
        }

        return publicCalls;
    }

    private static boolean isProtectedMethod(ActualControlFlowGraph acfg){
        return (acfg.getMethodAccess() & Opcodes.ACC_PROTECTED) == Opcodes.ACC_PROTECTED;
    }

    private static String cleanMethodName(String method){
        String newMethodName = method.substring(0, method.indexOf('('));
        return newMethodName;
    }


    private static void searchForNonPrivateMethods(BytecodeInstruction targetInstruction, String targetClass){
        List<BytecodeInstruction> instructions = BytecodeInstructionPool.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT()).getInstructionsIn(targetClass);

        LinkedList<String> callers =  new LinkedList<String>(); // all of the callers will be stored here
        Set<String> visitedMethods = new HashSet<String>();    // list of visited methods
        HashMap<BytecodeInstruction, ArrayList<String>> CUTMethods = new HashMap<>(); // all of the methods in Class Under Test!
        callers.add(targetInstruction.getActualCFG().getMethodName()); // this is the first non-public method to visit //FixMe: Why??
        // Preparing CUTMethods
        for (BytecodeInstruction instruct : instructions) {
            if(!CUTMethods.containsKey(instruct)) {
                ArrayList<String> calledMethods =  new ArrayList<>();
                for (BytecodeInstruction method_call : instruct.getRawCFG().determineMethodCalls()){
                    calledMethods.add(method_call.getCalledMethod());
                }
                CUTMethods.put(instruct, calledMethods);
            }
        }
        // until there are non-public methods to visit
        while (callers.size()>0){
            String privateMethod = callers.removeFirst();
            if (visitedMethods.contains(privateMethod)) { // if it has been already visited, we skip it
                continue;
            }else {
                visitedMethods.add(privateMethod);
            }

            for( BytecodeInstruction key : CUTMethods.keySet()) {
                ArrayList<String> list = CUTMethods.get(key);
                for (String invokedMethod : list) {
                    if (invokedMethod.equals(CUTMethods)) {
                        // the key is a caller.
                        // Checking the caller to see if it is private or not.
                        if(key.getActualCFG().isPublicMethod() || isProtectedMethod(key.getActualCFG())){
                            // this caller is public or protected.
                            if(FitnessFunctionHelper.isConstructor(key)){
                                LOG.info("One target constructor is added");
                                publicCalls.add(key.getMethodName());
                            } else {
                                LOG.info("One target method is added");
                                publicCalls.add(cleanMethodName(key.getMethodName()));
                            }
                        }else {
                            //this parent is private
                            callers.addLast(key.getMethodName());

                        }
                    }
                }
            }

        }
    }

    private static BytecodeInstruction getTargetInstruction (List<BytecodeInstruction> instructions , int targetLine) {
        BytecodeInstruction targetInstruction = null;
        // Looking for the instructions of the target line...
        for (BytecodeInstruction ins : instructions) {
            // and if the instruction for the target line number
            if (ins.getLineNumber() == targetLine){
                targetInstruction = ins;
            }
        }

        return targetInstruction;
    }

}
