package eu.stamp.botsing.fitnessfunction;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.StackTrace;
import eu.stamp.botsing.fitnessfunction.fitnessCalculator.CrashCoverageFitnessCalculator;
import eu.stamp.botsing.testgeneration.strategy.BotsingIndividualStrategy;
import org.evosuite.Properties;
import org.evosuite.TestGenerationContext;
import org.evosuite.coverage.exception.ExceptionCoverageHelper;

import org.evosuite.ga.stoppingconditions.MaxFitnessEvaluationsStoppingCondition;
import org.evosuite.graphs.cfg.ActualControlFlowGraph;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.BytecodeInstructionPool;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.utils.LoggingUtils;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class WeightedSum extends TestFitnessFunction {

    public static Set<String> publicCalls = new HashSet<String>();

    private static final Logger LOG = LoggerFactory.getLogger(BotsingIndividualStrategy.class);

    Throwable targetException;

    public WeightedSum (Throwable targetException){
        this.targetException = targetException;
        this.publicCalls = getPublicCalls();
    }
    @Override
    public double getFitness(TestChromosome testChromosome, ExecutionResult executionResult) {
        LOG.debug("Fitness calculation ... ");
        double exceptionCoverage = 1.0;
        double frameSimilarity = 1.0;
        // Priority 1) Line coverage
        double LineCoverageFitness = CrashCoverageFitnessCalculator.getLineCoverageFitness( executionResult, CrashProperties.getInstance().getStackTrace().getTargetLine());

        if(LineCoverageFitness == 0.0){
            //Priority 2) Exception coverage
            for (Integer ExceptionLocator : executionResult.getPositionsWhereExceptionsWereThrown()) {
                String thrownException = ExceptionCoverageHelper.getExceptionClass(executionResult, ExceptionLocator).getName();
                exceptionCoverage = 1;
                frameSimilarity = 1;
                if (thrownException.equals(CrashProperties.getInstance().getStackTrace().getExceptionType())){
                    exceptionCoverage = 0.0;
                    // Priority 3) Frame similarity
                    double tempFitness = CrashCoverageFitnessCalculator.calculateFrameSimilarity(executionResult.getExceptionThrownAtPosition(ExceptionLocator).getStackTrace());
                    if (tempFitness == 0.0){
                        frameSimilarity = 0.0;
                        break;
                    }else if (tempFitness<frameSimilarity){
                        frameSimilarity = tempFitness;
                    }
                }
            }
        }
        double fitnessValue = 3 * LineCoverageFitness  + 2 * exceptionCoverage + frameSimilarity;
        LOG.debug("Fitness Function: "+fitnessValue);
        testChromosome.setFitness(this,fitnessValue);
        testChromosome.increaseNumberOfEvaluations();
        return fitnessValue;
    }




    @Override
    public int compareTo(TestFitnessFunction testFitnessFunction) {
        // TODO Add this when we have multple fitness functions
        return 0;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( CrashProperties.getInstance().getStackTrace().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        return true;
    }

    @Override
    public String getTargetClass() {
        return CrashProperties.getInstance().getStackTrace().getTargetClass();
    }

    @Override
    public String getTargetMethod() {
        return CrashProperties.getInstance().getStackTrace().getTargetMethod();
    }
    private static Set<String> getPublicCalls() {
        StackTrace givenStackTrace = CrashProperties.getInstance().getStackTrace();
        int numberOfFrames = givenStackTrace.getNumberOfFrames();
        String targetClass = givenStackTrace.getTargetClass();
        String targetMethod =givenStackTrace.getTargetMethod();
        int targetLine = givenStackTrace.getTargetLine();
        List<BytecodeInstruction> instructions = BytecodeInstructionPool.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT()).getInstructionsIn(targetClass);
        BytecodeInstruction targetInstruction = getTargetInstruction(instructions, targetLine);

        if (targetInstruction.getActualCFG().isPublicMethod() ||
                isProtectedMethod(targetInstruction.getActualCFG())){
            LOG.info("The target method is public!");
            // If the public call is to a constructor, just add the class name
            // because this is how it will be retrieved in RootMethodTestChromosomeFactory!
            if(targetInstruction.getActualCFG().getName().contains("<init>")){
                publicCalls.add(targetClass);
            } else {
                publicCalls.add(cleanMethodName(targetInstruction.getMethodName()));
            }

        } else {
            // So the target call is private
            LOG.info("The target call '{}' is private!", targetInstruction.getMethodName());
            LOG.info("Searching for public callers");
            searchForNonPrivateMethods(targetInstruction, targetClass);
        }


        LoggingUtils.getEvoLogger().info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> The retrived call(s) to inject in the tests are:");
        Iterator<String> iterateParents = WeightedSum.publicCalls.iterator();

        // Fill up the set of parent calls by assessing the method names
        while (iterateParents.hasNext()) {
            String nextCall = iterateParents.next();
            LoggingUtils.getEvoLogger().info(">>>>>> " + nextCall);
        }

        return publicCalls;
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


    private static boolean isProtectedMethod(ActualControlFlowGraph acfg){
        return (acfg.getMethodAccess() & Opcodes.ACC_PROTECTED) == Opcodes.ACC_PROTECTED;
    }


    private static String cleanMethodName(String method){
        String newMethodName = method.substring(0, method.indexOf('('));
        return newMethodName;
    }


    private static void searchForNonPrivateMethods(BytecodeInstruction targetInstruction, String targetClass){
        List<BytecodeInstruction> instructions = BytecodeInstructionPool.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT()).getInstructionsIn(targetClass);

        LinkedList<String> methods =  new LinkedList<String>(); // linked list to store all methods to analyze
        Set<String> visitedMethods = new HashSet<String>();    // set to keep track of already visited methods
        HashMap<BytecodeInstruction, ArrayList<String>> methodsInCUT = new HashMap<>(); // all of the methods in Class Under Test!
        targetInstruction.getActualCFG().getMethodName();     // let's take the name of the method containing the instruction to cover
        methods.add(targetInstruction.getActualCFG().getMethodName()); // this is the first non-public method to visit
        // let's prepare the hashMap containing methods and the methods which is called by them!
        for (BytecodeInstruction instruct : instructions) {
            if(!methodsInCUT.containsKey(instruct)) {
                ArrayList<String> calledMethods =  new ArrayList<>();
                for (BytecodeInstruction method_call : instruct.getRawCFG().determineMethodCalls()){
                    calledMethods.add(method_call.getCalledMethod());
                }
                methodsInCUT.put(instruct, calledMethods);
            }
        }
        // until there are non-public methods to visit
        while (methods.size()>0){
            String target_method = methods.removeFirst(); // get the name of one of the private methods to analyze
            if (visitedMethods.contains(target_method)) // if it has been already visited, we skip it to avoid infinite loop
                continue;
            else
                visitedMethods.add(target_method);

            for( BytecodeInstruction key : methodsInCUT.keySet()) {
                ArrayList<String> list = methodsInCUT.get(key);
                for (String invokedMethod : list) {
                    if (invokedMethod.equals(target_method)) {
                        // we know that key is parent.
                        // now, we want to know if key is private or not!
                        if(key.getActualCFG().isPublicMethod() || isProtectedMethod(key.getActualCFG())){
                            // this parent is public or protected.
                            if(key.getMethodName().contains("<init>")){
                                LoggingUtils.getEvoLogger().info("* EvoCrash: The target call is made to a protected constructor!");
                                publicCalls.add(key.getMethodName());
                            } else {
                                LoggingUtils.getEvoLogger().info("* EvoCrash: The target call is made to a protected method!");
                                publicCalls.add(cleanMethodName(key.getMethodName()));
                            }
                        }else {
                            //this parent is private
                            methods.addLast(key.getMethodName());

                        }
                    }
                }
            }

        } // while

        LoggingUtils.getEvoLogger().info("CrashCoverageTestFitness: public calls size after search: " + publicCalls.size());
    }


    public String getKey(){
        StackTrace st = CrashProperties.getInstance().getStackTrace();
        //Using the CUT name and top method caller!
        return st.getTargetClass()+"_"+st.getTargetMethod();
    }

}
