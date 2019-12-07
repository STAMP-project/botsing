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


    public static void calculate(String givenTest, String caller, String callee){

        // Let's calculate Coupled Branches
        setClingProperties(caller,callee);
        List<Class> instrumentedClasses = instrumentClasses();

        generateCFGs(instrumentedClasses.get(1),instrumentedClasses.get(0));
        IntegrationTestingGoalFactory integrationTestingGoalFactory = new IntegrationTestingGoalFactory();
//        List<BranchPair> branchPairs = BranchPairPool.getInstance().getBranchPairs();
        Set<TestFitnessFunction> goalsSet = new HashSet<>(integrationTestingGoalFactory.getCoverageGoals());
        LOG.info("Total number of coupled branches goals: {}",goalsSet.size());

        // Here, we have the list of coupled branches in BranchPairPool.
        // Now, it is the time to execute the give test

        Executor executor = new Executor(givenTest,caller,callee);
        // execute the test for calculating the coverages
        executor.execute();
        // Here, we have the execution traces in a dedicated pool (ExecutionTracePool)




        // Here, we have the coverage data and coupled branches.
        // We just need to compare them to find the number of covered coupled branches.



//        int totalPairs = BranchPairPool.getInstance().getBranchPairs().size();
        int coveredPairs = getCoveredPairs(goalsSet).size();
        LOG.info("Number of covered coupled branches: {}", coveredPairs);

    }

    private static List<BranchPairFF> getCoveredPairs( Set<TestFitnessFunction> goalsSet){

        List<BranchPairFF> result = new ArrayList<>();
        for (TestFitnessFunction testFF: goalsSet){
            BranchPairFF branchPairFF = (BranchPairFF) testFF;
            BranchCoverageTestFitness firstBranchFF = branchPairFF.getFirstBranchFF();
            BranchCoverageTestFitness secondBranchFF = branchPairFF.getSecondBranchFF();
            // check both of the branches
            if(isBranchCovered(firstBranchFF) && isBranchCovered(secondBranchFF)){
                // The current branch pair is covered.
                // Add it to covered goals
                result.add(branchPairFF);
            }
        }

        return result;
    }

    private static boolean isBranchCovered(BranchCoverageTestFitness branchFF){
        if (branchFF == null){
            return true;
        }
        BranchPool branchPool = BranchPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
        Collection<ExecutionTrace> traces = ExecutionTracePool.getInstance().getExecutionTraces();
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
}
