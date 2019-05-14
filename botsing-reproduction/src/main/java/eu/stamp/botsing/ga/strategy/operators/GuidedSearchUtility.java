package eu.stamp.botsing.ga.strategy.operators;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.commons.BotsingTestGenerationContext;
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

import javax.annotation.Resource;
import java.util.*;

public class GuidedSearchUtility<T extends Chromosome> {

    @Resource
    FitnessFunctionHelper fitnessFunctionHelper = new FitnessFunctionHelper();

    private static final Logger LOG = LoggerFactory.getLogger(GuidedSearchUtility.class);

    public Set<String> publicCalls = new HashSet<String>();

    public boolean includesPublicCall (T individual) {
        if(publicCalls.size()==0) {
            if (CrashProperties.getInstance().getCrashesSize() == 1) {
                getPublicCalls(CrashProperties.getInstance().getStackTrace(0).getTargetClass(), CrashProperties.getInstance().getStackTrace(0).getTargetLine());
            } else {
                throw new IllegalStateException("Public calls are empty");
            }
        }
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
                if (isCall2Method(callName, currentStatement)) {
                    return true;
                }
                if (isCall2Constructor(callName, currentStatement)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean isCall2Method(String callName, Statement currentStatement){
        if (!callName.contains(".") && currentStatement instanceof MethodStatement) {
            MethodStatement candidateMethod = (MethodStatement) currentStatement;
            return candidateMethod.getMethodName().equalsIgnoreCase(callName);
        }
        return false;
    }

    protected boolean isCall2Constructor(String callName, Statement currentStatement){
        if (callName.contains(".") && currentStatement instanceof ConstructorStatement) {
            return callName.equals(((ConstructorStatement) currentStatement).getDeclaringClassName());
        }
        return false;
    }

    protected Set<String> getPublicCalls(String targetClass, int targetLine, List<BytecodeInstruction> instructions){
        LOG.info("Detecting the target method call(s) ...");
//        int targetLine = trace.getTargetLine();
//        String targetClass = trace.getTargetClass();

        BytecodeInstruction targetInstruction = getTargetInstruction(instructions, targetLine);

        if (!isPrivateMethod(targetInstruction.getActualCFG())){
            LOG.info("The target method is public!");

            if(fitnessFunctionHelper.isConstructor(targetInstruction)){
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

        int counter = 1;
        while (iterateParents.hasNext()) {
            String nextCall = iterateParents.next();
            LOG.info("Target method #{} is {}",counter,nextCall);
            counter++;
        }

        return publicCalls;
    }


    public Set<String> getPublicCalls(String targetClass, int targetLine) {
        if (publicCalls.size() ==0){
//            StackTrace givenStackTrace = CrashProperties.getInstance().getStackTrace();
//            String targetClass = givenStackTrace.getTargetClass();
            List<BytecodeInstruction> instructions;
            if(CrashProperties.integrationTesting){
                instructions = BytecodeInstructionPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).getInstructionsIn(targetClass);
            }else{
                instructions = BytecodeInstructionPool.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT()).getInstructionsIn(targetClass);
            }
            publicCalls =  getPublicCalls(targetClass,targetLine, instructions);
        }

        return publicCalls;
    }


    private static boolean isPrivateMethod(ActualControlFlowGraph acfg){
        return (acfg.getMethodAccess() & Opcodes.ACC_PRIVATE) == Opcodes.ACC_PRIVATE;
    }

    private static String cleanMethodName(String method){
        String newMethodName = method.substring(0, method.indexOf('('));
        return newMethodName;
    }

    protected void searchForNonPrivateMethods(List<BytecodeInstruction> instructions, BytecodeInstruction targetInstruction){
        LinkedList<String> callers =  new LinkedList<String>(); // all of the callers will be stored here
        Set<String> visitedMethods = new HashSet<String>();    // list of visited methods
        HashMap<BytecodeInstruction, ArrayList<String>> CUTMethods = new HashMap<>(); // all of the methods in Class Under Test!
        callers.add(targetInstruction.getActualCFG().getMethodName()); // this is the first non-public method to visit
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
                    if (invokedMethod.equals(privateMethod)) {
                        // the key is a caller.
                        // Checking the caller to see if it is private or not.
                        if(!isPrivateMethod(key.getActualCFG())){
                            // this caller is public or protected.
                            if(fitnessFunctionHelper.isConstructor(key)){
                                LOG.debug("One target constructor is added");
                                publicCalls.add(key.getMethodName());
                            } else {
                                LOG.debug("One target method is added");
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

    /**
     * This method retrieves all public/protected methods that call the method containing the target bytecode instruction
     * @param targetInstruction target bytecode instruction (corresponding to the line of code in the target stack trace)
     * @param targetClass the class under test
     */
    protected void searchForNonPrivateMethods(BytecodeInstruction targetInstruction, String targetClass){
        List<BytecodeInstruction> instructions = null;
        if(CrashProperties.integrationTesting){
            instructions=BytecodeInstructionPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).getInstructionsIn(targetClass);
        }else{
            instructions=BytecodeInstructionPool.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT()).getInstructionsIn(targetClass);
        }
        searchForNonPrivateMethods(instructions, targetInstruction);
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
