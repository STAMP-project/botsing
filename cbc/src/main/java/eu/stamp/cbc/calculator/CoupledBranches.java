package eu.stamp.cbc.calculator;


import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import eu.stamp.botsing.commons.instrumentation.ClassInstrumentation;
import eu.stamp.cbc.extraction.ExecutionTracePool;
import eu.stamp.cbc.testsuite.execution.Executor;
import eu.stamp.cling.IntegrationTestingProperties;
import eu.stamp.cling.fitnessfunction.BranchPairFF;
import eu.stamp.cling.graphs.cfg.CFGGenerator;
import eu.stamp.cling.integrationtesting.IntegrationTestingGoalFactory;
import org.evosuite.coverage.branch.Branch;
import org.evosuite.coverage.branch.BranchCoverageTestFitness;
import org.evosuite.coverage.branch.BranchPool;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionTrace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CoupledBranches {
    private static final Logger LOG = LoggerFactory.getLogger(CoupledBranches.class);


    private static Set<TestFitnessFunction> preCalculation(String caller, String callee){
        // Let's calculate Coupled Branches
        setClingProperties(caller,callee);
        List<Class> instrumentedClasses = instrumentClasses();

        generateCFGs(instrumentedClasses.get(1),instrumentedClasses.get(0));

        IntegrationTestingGoalFactory integrationTestingGoalFactory = new IntegrationTestingGoalFactory();
        Set<TestFitnessFunction> goalsSet = new HashSet<>(integrationTestingGoalFactory.getCoverageGoals());
        LOG.info("Total number of coupled branches goals: {}",goalsSet.size());
        return goalsSet;
    }

    public static void calculate(String clingTest, String caller, String callee){
        Set<TestFitnessFunction> goalsSet = preCalculation(caller,callee);
        // Here, we have the list of coupled branches in BranchPairPool.
        // Now, it is the time to execute the give test

        // execute the cling test
        Executor executor = new Executor(clingTest,caller,callee);
        executor.execute();

        // Here, we have the execution traces in a dedicated pool (ExecutionTracePool)
        // We just need to compare them to find the number of covered coupled branches by test suites.

        List<BranchPairFF> coveredPairsByCling = getCoveredPairs(goalsSet, caller);
        LOG.info("Number of covered coupled branches by test suite: {}", coveredPairsByCling.size());

    }

    public static void calculate(String givenTestCaller, String givenTestCallee, String caller, String callee){
        Set<TestFitnessFunction> goalsSet = preCalculation(caller,callee);
        // Here, we have the list of coupled branches in BranchPairPool.
        // Now, it is the time to execute the give tests

        // execute the callee test for calculating the coverages
        Executor executor = new Executor(givenTestCallee,caller,callee);
        executor.execute();
        // execute the caller test for calculating the coverages
        executor = new Executor(givenTestCaller,caller,callee);
        executor.execute();

        // Here, we have the execution traces in a dedicated pool (ExecutionTracePool)
        // We just need to compare them to find the number of covered coupled branches by test suites.
        List<BranchPairFF> coveredPairsByE = getCoveredPairs(goalsSet, callee);
        LOG.info("Number of covered coupled branches by test suite E: {}", coveredPairsByE.size());
        List<BranchPairFF> coveredPairsByR = getCoveredPairs(goalsSet, caller);
        LOG.info("Number of covered coupled branches by test suite R: {}", coveredPairsByR.size());
        List<BranchPairFF> coveredByBoth = union(coveredPairsByE, coveredPairsByR);
        LOG.info("Number of covered coupled branches by both: {}", coveredByBoth.size());
    }

    private static List<BranchPairFF> getCoveredPairs( Set<TestFitnessFunction> goalsSet, String className){

        List<BranchPairFF> result = new ArrayList<>();
        for (TestFitnessFunction testFF: goalsSet){
            BranchPairFF branchPairFF = (BranchPairFF) testFF;
            BranchCoverageTestFitness firstBranchFF = branchPairFF.getFirstBranchFF();
            BranchCoverageTestFitness secondBranchFF = branchPairFF.getSecondBranchFF();
            // check both of the branches
            if(isBranchCovered(firstBranchFF, className) && isBranchCovered(secondBranchFF, className)){
                // The current branch pair is covered.
                // Add it to covered goals
                result.add(branchPairFF);
            }
        }

        return result;
    }

    private static boolean isBranchCovered(BranchCoverageTestFitness branchFF, String className){
        if (branchFF == null){
            return true;
        }
        BranchPool branchPool = BranchPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
        Collection<ExecutionTrace> traces = ExecutionTracePool.getInstance().getExecutionTraces(className);
        boolean branchExpressionValue = branchFF.getBranchExpressionValue();
        Branch targetBranch = branchFF.getBranch();

        for (ExecutionTrace trace : traces){
            // Find branch ids for checking
            Set<Integer> branchIDsToCheck;
            if (branchExpressionValue){
                // check covered true
                branchIDsToCheck = trace.getCoveredFalseBranches();
            }else{
                // check covered false
                branchIDsToCheck = trace.getCoveredTrueBranches();
            }
            for(Integer id: branchIDsToCheck){
                Branch currentBranch = branchPool.getBranch(id);
                if(currentBranch.equals(targetBranch)){
                    return true;
                }
            }
        }
        return false;

    }

    private static void generateCFGs(Class caller, Class callee) {
        CFGGenerator cfgGenerator = new CFGGenerator(caller,callee);
        cfgGenerator.generate();
    }

    private static List<Class> instrumentClasses() {
        ClassInstrumentation classInstrumenter = new ClassInstrumentation();
        List<String> interestingClasses = Arrays.asList(IntegrationTestingProperties.TARGET_CLASSES);
        Collections.reverse(interestingClasses);
        String testingClass = interestingClasses.get(1);
        List<Class> instrumentedClasses = classInstrumenter.instrumentClasses(interestingClasses,testingClass);
        return instrumentedClasses;
    }

    private static void setClingProperties(String caller, String callee) {
        IntegrationTestingProperties.fitnessFunctions = new IntegrationTestingProperties.FitnessFunction[]{IntegrationTestingProperties.FitnessFunction.Branch_Pairs};
        IntegrationTestingProperties.TARGET_CLASSES = new String[]{caller, callee};
    }

    private static  <T> List<T> union(List<T> list1, List<T> list2) {
        Set<T> set = new HashSet<T>();

        set.addAll(list1);
        set.addAll(list2);

        return new ArrayList<T>(set);
    }

}
