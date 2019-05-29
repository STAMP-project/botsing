package eu.stamp.botsing.integration.integrationtesting;

import org.evosuite.Properties;
import org.evosuite.coverage.TestFitnessFactory;
import org.evosuite.result.TestGenerationResult;
import org.evosuite.rmi.ClientServices;
import org.evosuite.rmi.service.ClientState;
import org.evosuite.runtime.LoopCounter;
import org.evosuite.runtime.sandbox.Sandbox;
import org.evosuite.strategy.TestGenerationStrategy;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.TestCaseExecutor;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static eu.stamp.botsing.commons.PostProcessUtility.*;
import static eu.stamp.botsing.commons.SetupUtility.analyzeClassDependencies;
import static eu.stamp.botsing.commons.SetupUtility.configureClassReInitializer;

public class IntegrationTesting {
    private static final Logger LOG = LoggerFactory.getLogger(IntegrationTesting.class);
    public static List<TestGenerationResult> execute(){
        List<TestGenerationResult> generatedTests = new ArrayList<TestGenerationResult>();


        //Before starting search, activate the sandbox
        if (Properties.SANDBOX) {
            Sandbox.initializeSecurityManagerForSUT();
        }


        generatedTests.add(generateTests());


        // Resetting sandbox after test generation
        if (Properties.SANDBOX) {
            Sandbox.resetDefaultSecurityManager();
        }


        return generatedTests;
    }


    private static TestGenerationResult generateTests(){
        ClientServices.getInstance().getClientNode().changeState(ClientState.INITIALIZATION);

        // Deactivate loop counter to make sure classes initialize properly
        LoopCounter.getInstance().setActive(false);

        //Initialize EvoSuite test executor
        TestCaseExecutor.initExecutor();

        // In the first step initialize the target class
        try{
            // ToDo: call to CFG Generator
            analyzeClassDependencies("classNameOfCaller");
        }catch (Exception e){
            LOG.error("Error in target initialization:");
            e.printStackTrace();
        }finally {
            if(Properties.RESET_STATIC_FIELDS){
                configureClassReInitializer();
            }
            LoopCounter.getInstance().setActive(true);
        }



        TestGenerationStrategy strategy = IntegrationTestingUtility.getTestGenerationFactory();
        TestSuiteChromosome testCases = strategy.generateTests();

        postProcessTests(testCases,getFitnessFactories());

        TestGenerationResult writingTest = writeJUnitTestsAndCreateResult(testCases, Properties.JUNIT_SUFFIX);
        writeJUnitFailingTests();
        TestCaseExecutor.pullDown();

        LOG.info("The solution test is saved!");

        return writingTest;

    }


    private static List<TestFitnessFactory<? extends TestFitnessFunction>> getFitnessFactories() {
        List<TestFitnessFactory<? extends TestFitnessFunction>> goalsFactory = new ArrayList<TestFitnessFactory<? extends TestFitnessFunction>>();
        goalsFactory.add(new IntegrationTestingGoalFactory());
        return goalsFactory;
    }
}
