package eu.stamp.cling.integrationtesting;

import eu.stamp.botsing.commons.SetupUtility;
import eu.stamp.botsing.commons.instrumentation.ClassInstrumentation;
import eu.stamp.cling.IntegrationTestingProperties;
import eu.stamp.cling.fitnessfunction.FitnessFunctions;
import eu.stamp.cling.graphs.cfg.CFGGenerator;
import org.evosuite.Properties;
import org.evosuite.coverage.TestFitnessFactory;
import org.evosuite.result.TestGenerationResult;
import org.evosuite.runtime.LoopCounter;
import org.evosuite.runtime.sandbox.Sandbox;
import org.evosuite.strategy.TestGenerationStrategy;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.TestCaseExecutor;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.testsuite.TestSuiteSerialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static eu.stamp.botsing.commons.PostProcessUtility.*;
import static eu.stamp.botsing.commons.SetupUtility.configureClassReInitializer;

public class IntegrationTesting {
    private static final Logger LOG = LoggerFactory.getLogger(IntegrationTesting.class);
    public static List<TestGenerationResult> execute(){
        LOG.info("Starting ...");
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
//        ClientServices.getInstance().getClientNode().changeState(ClientState.INITIALIZATION);

        // Deactivate loop counter to make sure classes initialize properly
        LoopCounter.getInstance().setActive(false);

        //Initialize EvoSuite test executor
        TestCaseExecutor.initExecutor();


        try{
            LOG.info("Instrumenting target classes {} and {}",IntegrationTestingProperties.TARGET_CLASSES[0],IntegrationTestingProperties.TARGET_CLASSES[1]);
            // In the first step initialize the target classes
            ClassInstrumentation classInstrumenter = new ClassInstrumentation();
            List <String> interestingClasses = Arrays.asList(IntegrationTestingProperties.TARGET_CLASSES);
            Collections.reverse(interestingClasses);
            String testingClass = interestingClasses.get(1);
            List<Class> instrumentedClasses = classInstrumenter.instrumentClasses(interestingClasses,testingClass);
            // We assume that first passed class is tha caller, and second one is the callee
            Class caller = instrumentedClasses.get(1);
            LOG.info("Instrumented caller class: {}",caller.getName());
            Class callee = instrumentedClasses.get(0);
            LOG.info("Instrumented callee class: {}",callee.getName());
            // Generate the graphs according to the chosen fitness function
            CFGGenerator cfgGenerator = new CFGGenerator(caller,callee);
            cfgGenerator.generate();
            // Analyze the dependencies of the caller class
            LOG.info("Analyzing dependencies...");
            SetupUtility.analyzeClassDependencies(caller.getName());

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

//        // Check if we lose CBC in the final test
//        TestCaseExecutor.initExecutor();
//        FitnessFunctions ffs = new FitnessFunctions();
//        int coveredGoals = 0;
//        for (TestFitnessFunction testFitnessFunction: ffs.getFitnessFunctionList()){
//            double min = Double.MAX_VALUE;
//            for (TestChromosome tc: testCases.getTestChromosomes()){
//                double value = testFitnessFunction.getFitness(tc);
//                if(value < min){
//                    min = value;
//                }
//            }
//            if (min == 0){
//                coveredGoals++;
//            }
//        }
        TestGenerationResult writingTest = writeJUnitTestsAndCreateResult(testCases, Properties.JUNIT_SUFFIX);


        if (Properties.CTG_SEEDS_FILE_OUT != null) {
            String user_dir = System.getProperty("user.dir");
            TestSuiteSerialization.saveTests(testCases, new File(Paths.get(user_dir,Properties.CTG_SEEDS_FILE_OUT).toString()));
        }
        writeJUnitFailingTests();
        TestCaseExecutor.pullDown();
        LOG.info("test size 7: {}",testCases.size());
        LOG.info("The solution test is saved!");

//
//        TestCaseExecutor.pullDown();
//
//        LOG.info("Number of Covered goals in the final test: {}", coveredGoals);
        return writingTest;

    }


    private static List<TestFitnessFactory<? extends TestFitnessFunction>> getFitnessFactories() {
        List<TestFitnessFactory<? extends TestFitnessFunction>> goalsFactory = new ArrayList<TestFitnessFactory<? extends TestFitnessFunction>>();
        goalsFactory.add(new IntegrationTestingGoalFactory());
        return goalsFactory;
    }
}
