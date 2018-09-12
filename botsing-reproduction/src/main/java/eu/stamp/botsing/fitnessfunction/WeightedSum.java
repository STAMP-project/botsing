package eu.stamp.botsing.fitnessfunction;

/*-
 * #%L
 * botsing-reproduction
 * %%
 * Copyright (C) 2017 - 2018 eu.stamp-project
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.StackTrace;
import eu.stamp.botsing.fitnessfunction.fitnessCalculator.CrashCoverageFitnessCalculator;
import eu.stamp.botsing.fitnessfunction.testcase.factories.FitnessFunctionHelper;
import eu.stamp.botsing.testgeneration.strategy.BotsingIndividualStrategy;
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
//        String fullTargetClass = givenStackTrace.getTargetClassFullName();
        String targetMethod =givenStackTrace.getTargetMethod();
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
        Iterator<String> iterateParents = WeightedSum.publicCalls.iterator();

        while (iterateParents.hasNext()) {
            String nextCall = iterateParents.next();
            LOG.info(nextCall);
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

        LinkedList<String> callers =  new LinkedList<String>(); // all of the callers will be stored here
        Set<String> visitedMethods = new HashSet<String>();    // list of visited methods
        HashMap<BytecodeInstruction, ArrayList<String>> CUTMethods = new HashMap<>(); // all of the methods in Class Under Test!
//        targetInstruction.getActualCFG().getMethodName();     // let's take the name of the method containing the instruction to cover
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
            if (visitedMethods.contains(privateMethod)) // if it has been already visited, we skip it
                continue;
            else
                visitedMethods.add(privateMethod);

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


    public String getKey(){
        StackTrace st = CrashProperties.getInstance().getStackTrace();
        //Using the CUT name and top method caller!
        return st.getTargetClass()+"_"+st.getTargetMethod();
    }

}
