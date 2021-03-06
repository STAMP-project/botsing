package eu.stamp.cbc.calculator;


import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import eu.stamp.botsing.commons.instrumentation.ClassInstrumentation;
import eu.stamp.cbc.extraction.ExecutionTracePool;
import eu.stamp.cbc.Executor;
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

    public static void calculate(String tests, String caller, String callee){
        Set<TestFitnessFunction> goalsSet = preCalculation(caller,callee);
        // Here, we have the list of coupled branches in BranchPairPool.
        // Now, it is the time to execute the given tests
        if(tests.startsWith(":")){
            tests=tests.substring(1);
        }

        if (tests.endsWith(":")){
            tests= tests.substring(0, tests.length()-1);
        }
        String[] testsArr = tests.split(":");
        LOG.info("Number of given tests: {}",testsArr.length);
        Map<String,ExecutionTrace> traces = new HashMap<>();

        for (String testClass: testsArr){
            Executor executor = new Executor(testClass,caller,callee);
            executor.execute();
        }
        // execute the cling test



        // Here, we have the execution traces in a dedicated pool (ExecutionTracePool)
        // We just need to compare them to find the number of covered coupled branches by test suites.

        Set<BranchPairFF> coveredPairsByCling = getCoveredPairs(goalsSet, caller);
        LOG.info("Number of covered coupled branches by test suite: {}", coveredPairsByCling.size());

    }


    private static Set<BranchPairFF> getCoveredPairs( Set<TestFitnessFunction> goalsSet, String className){

        Set<BranchPairFF> result = new HashSet<>();
        for (TestFitnessFunction testFF: goalsSet){
            BranchPairFF branchPairFF = (BranchPairFF) testFF;
            BranchCoverageTestFitness firstBranchFF = branchPairFF.getFirstBranchFF();
            BranchCoverageTestFitness secondBranchFF = branchPairFF.getSecondBranchFF();
            // check both of the branches
            if(isBranchPairCovered(branchPairFF)){
                result.add(branchPairFF);
            }
//            if(isBranchCovered(firstBranchFF, className) && isBranchCovered(secondBranchFF, className)){
//                // The current branch pair is covered.
//                // Add it to covered goals
//                result.add(branchPairFF);
//            }
        }
        for (BranchPairFF bff : result){
            LOG.info("==========");
            if (bff.getFirstBranchFF() != null){
                LOG.info("1- {}",bff.getFirstBranchFF().toString());
            }
            if (bff.getSecondBranchFF() != null){
                LOG.info("2- {}",bff.getSecondBranchFF().toString());
            }

            if (bff.getBranchPair().isDependent()){
                LOG.info("expression- {}",bff.getBranchPair().getExpression());
            }
        }
        return result;
    }

    private static boolean isBranchPairCovered(BranchPairFF branchPairFF) {

        BranchCoverageTestFitness firstBranchFF = branchPairFF.getFirstBranchFF();
        BranchCoverageTestFitness secondBranchFF = branchPairFF.getSecondBranchFF();
        Map<String,ExecutionTrace> traces = ExecutionTracePool.getInstance().getExecutionTraces();


        for (String test : traces.keySet()){
            ExecutionTrace trace = traces.get(test);

            if(isBranchCoveredByTrace(trace,firstBranchFF) && isBranchCoveredByTrace(trace,secondBranchFF)){
                Set<Integer> coveredLines = trace.getCoveredLines(branchPairFF.getCallSite().getClassName());
                if (coveredLines.contains(branchPairFF.getCallSite().getLineNumber()) || firstBranchFF == null){
                    return true;
                }else{
                    LOG.debug("Call site is not covered");
                }
            }
        }

        return false;
    }

    private static boolean isBranchCoveredByTrace(ExecutionTrace trace, BranchCoverageTestFitness branchFF) {
        if (branchFF == null){
            return true;
        }
        boolean branchExpressionValue = branchFF.getBranchExpressionValue();
        Branch targetBranch = branchFF.getBranch();

        Set<Integer> branchIDsToCheck;
        if (branchExpressionValue){
            // check covered true
            branchIDsToCheck = trace.getCoveredTrueBranches();
        }else{
            // check covered false
            branchIDsToCheck = trace.getCoveredFalseBranches();
        }

        if(branchIDsToCheck.contains(targetBranch.getActualBranchId())){
            return true;
        }
        return false;
    }

    private static boolean isBranchCovered(BranchCoverageTestFitness branchFF, String className){
        if (branchFF == null){
            return true;
        }
        BranchPool branchPool = BranchPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
        Map<String,ExecutionTrace> traces = ExecutionTracePool.getInstance().getExecutionTraces();
        boolean branchExpressionValue = branchFF.getBranchExpressionValue();
        Branch targetBranch = branchFF.getBranch();

        for (String test : traces.keySet()){
            ExecutionTrace trace = traces.get(test);
            // Find branch ids for checking
            Set<Integer> branchIDsToCheck;
            if (branchExpressionValue){
                // check covered true
                branchIDsToCheck = trace.getCoveredTrueBranches();
            }else{
                // check covered false
                branchIDsToCheck = trace.getCoveredFalseBranches();
            }
            if(branchIDsToCheck.contains(targetBranch.getActualBranchId())){
                return true;
            }
        }
        return false;

    }

    private static void generateCFGs(Class caller, Class callee) {
        CFGGenerator cfgGenerator = new CFGGenerator(caller,callee);
        cfgGenerator.generate();
    }

    public static List<Class> instrumentClasses() {
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
