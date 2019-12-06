package eu.stamp.cbc.calculator;


import eu.stamp.botsing.commons.instrumentation.ClassInstrumentation;
import eu.stamp.cbc.testsuite.execution.Executor;
import eu.stamp.cling.IntegrationTestingProperties;
import eu.stamp.cling.coverage.branch.BranchPair;
import eu.stamp.cling.coverage.branch.BranchPairPool;
import eu.stamp.cling.graphs.cfg.CFGGenerator;
import eu.stamp.cling.integrationtesting.IntegrationTestingGoalFactory;
import org.evosuite.testcase.TestFitnessFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CoupledBranches {
    private static final Logger LOG = LoggerFactory.getLogger(CoupledBranches.class);


    public static void calculate(String givenTest, String caller, String callee){
        Executor executor = new Executor(givenTest,caller,callee);
        // execute the test for calculating the coverages
        executor.execute();

        // Here, we have the coverage data in a dedicated pool

        // Let's calculate Coupled Branches
        setClingProperties(caller,callee);
        List<Class> instrumentedClasses = instrumentClasses();

        generateCFGs(instrumentedClasses.get(1),instrumentedClasses.get(0));
        IntegrationTestingGoalFactory integrationTestingGoalFactory = new IntegrationTestingGoalFactory();
        List<BranchPair> branchPairs = BranchPairPool.getInstance().getBranchPairs();
        Set<TestFitnessFunction> goalsSet = new HashSet<>(integrationTestingGoalFactory.getCoverageGoals());

        // Here, we have the coverage data and coupled branches. We just need to compare them to find the number of covered coupled branches

        for (BranchPair pair : branchPairs){

        }
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
