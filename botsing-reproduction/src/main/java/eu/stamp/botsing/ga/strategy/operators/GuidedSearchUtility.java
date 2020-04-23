package eu.stamp.botsing.ga.strategy.operators;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.StackTrace;
import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import org.evosuite.Properties;
import org.evosuite.TestGenerationContext;
import org.evosuite.graphs.cfg.ActualControlFlowGraph;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.BytecodeInstructionPool;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.statements.Statement;
import org.evosuite.utils.generic.GenericAccessibleObject;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.evosuite.ga.Chromosome;

import java.util.*;

public class GuidedSearchUtility<T extends Chromosome> {


    private static final Logger LOG = LoggerFactory.getLogger(GuidedSearchUtility.class);

    public Set<BytecodeInstruction> publicCallsBC = new HashSet<>();

    public BytecodeInstruction collectPublicCalls(StackTrace trace){
        Set<BytecodeInstruction> result = new HashSet<>();

        boolean nonPrivateFrameFound = false;
        int currentTargetFramelevel = trace.getTargetFrameLevel();

        while(!nonPrivateFrameFound){
//            Get current target frame
            StackTraceElement currentTargetFrame = trace.getAllFrames().get(currentTargetFramelevel-1);
            publicCallsBC.clear();
            getPublicCalls(currentTargetFrame.getClassName(), currentTargetFrame.getLineNumber());
            for(BytecodeInstruction bc: publicCallsBC){
                if(bc.getClassName().equals(currentTargetFrame.getClassName()) &&
                        cleanMethodName(bc.getMethodName()).equals(currentTargetFrame.getMethodName()) &&
                        bc.getLineNumber() == currentTargetFrame.getLineNumber()){
                    result.add(bc);
                    nonPrivateFrameFound=true;
                    trace.updatePublicTargetFrameLevel(currentTargetFramelevel);
                    break;
                }
            }
            currentTargetFramelevel++;
        }
        if(result.size() != 1){
            throw new IllegalStateException("There should be only one target bytecode instruction!");
        }
        return (BytecodeInstruction) result.toArray()[0];
    }


    public boolean includesPublicCall(T individual, Set<GenericAccessibleObject<?>> publicCalls) {
        Iterator<GenericAccessibleObject<?>> publicCallsIterator = publicCalls.iterator();
        TestChromosome candidateChrom = (TestChromosome) individual;
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

    protected void getPublicCalls(String targetClass, int targetLine, List<BytecodeInstruction> instructions){
        LOG.debug("Detecting the target method call(s) ...");

        BytecodeInstruction targetInstruction = getTargetInstruction(instructions, targetLine);

        if (!isPrivateMethod(targetInstruction.getActualCFG())){
            LOG.debug("The target method is public!");
            publicCallsBC.add(targetInstruction);
        } else {
            // The target call is private
            LOG.debug("The target call '{}' is private!", targetInstruction.getMethodName());
            LOG.debug("Searching for public callers");
            searchForNonPrivateMethods(targetInstruction, targetClass);
        }
    }


    protected void getPublicCalls(String targetClass, int targetLine) {
            List<BytecodeInstruction> instructions;
            if(CrashProperties.integrationTesting){
                instructions = BytecodeInstructionPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).getInstructionsIn(targetClass);
            }else{
                instructions = BytecodeInstructionPool.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT()).getInstructionsIn(targetClass);
            }
            getPublicCalls(targetClass,targetLine, instructions);
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
                            publicCallsBC.add(key);
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
        List<BytecodeInstruction> instructions;
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
