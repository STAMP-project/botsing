package eu.stamp.botsing.integration.integrationtesting;

import eu.stamp.botsing.commons.instrumentation.ClassInstrumentation;
import eu.stamp.botsing.integration.IntegrationTestingProperties;
import eu.stamp.botsing.integration.graphs.cfg.CFGGenerator;
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
import java.util.Arrays;
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


        try{
            // In the first step initialize the target classes
            ClassInstrumentation classInstrumenter = new ClassInstrumentation();
            List <String> interestingClasses = Arrays.asList(IntegrationTestingProperties.TARGET_CLASSES);
            List<Class> instrumentedClasses = classInstrumenter.instrumentClasses(interestingClasses,interestingClasses.get(0));
            // We assume that first passed class is tha caller, and second one is the callee
            Class caller = instrumentedClasses.get(0);
            Class calee = instrumentedClasses.get(1);
            // Generate the inter-procedural graphs (IRCFG, IACFG, and ICDG)
            CFGGenerator cfgGenerator = new CFGGenerator(caller,calee);
            cfgGenerator.generateInterProceduralGraphs();
            // Analyze the dependencies of the caller class
            analyzeClassDependencies(caller.getName());
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
