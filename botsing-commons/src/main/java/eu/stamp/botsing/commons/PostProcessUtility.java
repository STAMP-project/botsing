package eu.stamp.botsing.commons;

import org.evosuite.Properties;
import org.evosuite.TestSuiteGeneratorHelper;
import org.evosuite.TimeController;
import org.evosuite.contracts.FailingTestSet;
import org.evosuite.coverage.TestFitnessFactory;
import org.evosuite.junit.JUnitAnalyzer;
import org.evosuite.junit.writer.TestSuiteWriter;
import org.evosuite.result.TestGenerationResult;
import org.evosuite.result.TestGenerationResultBuilder;
import org.evosuite.rmi.ClientServices;
import org.evosuite.rmi.service.ClientState;
import org.evosuite.statistics.RuntimeVariable;
import org.evosuite.testcase.ConstantInliner;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.testsuite.TestSuiteMinimizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class PostProcessUtility {
    private static final Logger LOG = LoggerFactory.getLogger(PostProcessUtility.class);

    public static void postProcessTests(TestSuiteChromosome testSuite, List<TestFitnessFactory<? extends TestFitnessFunction>> fitnessFactories, boolean keepTestsCoveringSameGoal) {

        if (Properties.INLINE) {
            ConstantInliner inliner = new ConstantInliner();
            inliner.inline(testSuite);
        }


        if (Properties.MINIMIZE) {
            double before = testSuite.getFitness();



            LOG.info("* Minimizing test suite");
            if(!keepTestsCoveringSameGoal){
                TestSuiteMinimizer minimizer = new TestSuiteMinimizer(fitnessFactories);
                minimizer.minimize(testSuite, true);
            }


            double after = testSuite.getFitness();
            if (after > before + 0.01d) { // assume minimization
                throw new Error("EvoSuite bug: minimization lead fitness from " + before + " to " + after);
            }
        }




        compileAndCheckTests(testSuite);
    }


    private static void compileAndCheckTests(TestSuiteChromosome chromosome) {
        LOG.info("* Compiling and checking tests");

        if (!JUnitAnalyzer.isJavaCompilerAvailable()) {
            String msg = "No Java compiler is available. Make sure to run EvoSuite with the JDK and not the JRE."
                    + "You can try to setup the JAVA_HOME system variable to point to it, as well as to make sure that the PATH "
                    + "variable points to the JDK before any JRE.";
            LOG.error(msg);
            throw new RuntimeException(msg);
        }

        ClientServices.getInstance().getClientNode().changeState(ClientState.JUNIT_CHECK);

        // Store this value; if this option is true then the JUnit check
        // would not succeed, as the JUnit classloader wouldn't find the class
        boolean junitSeparateClassLoader = Properties.USE_SEPARATE_CLASSLOADER;
        Properties.USE_SEPARATE_CLASSLOADER = false;

        int numUnstable = 0;

        // note: compiling and running JUnit tests can be very time consuming
        if (!TimeController.getInstance().isThereStillTimeInThisPhase()) {
            Properties.USE_SEPARATE_CLASSLOADER = junitSeparateClassLoader;
            return;
        }

        List<TestCase> testCases = chromosome.getTests(); // make copy of
        // current tests

        // first, let's just get rid of all the tests that do not compile
        JUnitAnalyzer.removeTestsThatDoNotCompile(testCases);

        // compile and run each test one at a time. and keep track of total time
        long start = java.lang.System.currentTimeMillis();
        Iterator<TestCase> iter = testCases.iterator();
        while (iter.hasNext()) {
            if (!TimeController.getInstance().hasTimeToExecuteATestCase()) {
                break;
            }
            TestCase tc = iter.next();
            List<TestCase> list = new ArrayList<>();
            list.add(tc);
            numUnstable += JUnitAnalyzer.handleTestsThatAreUnstable(list);
            if (list.isEmpty()) {
                // if the test was unstable and deleted, need to remove it from
                // final testSuite
                iter.remove();
            }
        }
        /*
         * compiling and running each single test individually will take more
         * than compiling/running everything in on single suite. so it can be
         * used as an upper bound
         */
        long delta = java.lang.System.currentTimeMillis() - start;

        numUnstable += checkAllTestsIfTime(testCases, delta);

        // second passage on reverse order, this is to spot dependencies among
        // tests
        if (testCases.size() > 1) {
            Collections.reverse(testCases);
            numUnstable += checkAllTestsIfTime(testCases, delta);
        }

        chromosome.clearTests(); // remove all tests
        for (TestCase testCase : testCases) {
            chromosome.addTest(testCase); // add back the filtered tests
        }

        boolean unstable = (numUnstable > 0);

        if (!TimeController.getInstance().isThereStillTimeInThisPhase()) {
            LOG.warn("JUnit checking timed out");
        }

        ClientServices.track(RuntimeVariable.HadUnstableTests, unstable);
        ClientServices.track(RuntimeVariable.NumUnstableTests, numUnstable);
        Properties.USE_SEPARATE_CLASSLOADER = junitSeparateClassLoader;

    }


    private static int checkAllTestsIfTime(List<TestCase> testCases, long delta) {
        if (TimeController.getInstance().hasTimeToExecuteATestCase()
                && TimeController.getInstance().isThereStillTimeInThisPhase(delta)) {
            return JUnitAnalyzer.handleTestsThatAreUnstable(testCases);
        }
        return 0;
    }


    public static TestGenerationResult writeJUnitTestsAndCreateResult(TestSuiteChromosome testSuite, String suffix) {
        List<TestCase> tests = testSuite.getTests();
        if (Properties.JUNIT_TESTS) {
            ClientServices.getInstance().getClientNode().changeState(ClientState.WRITING_TESTS);

            TestSuiteWriter suiteWriter = new TestSuiteWriter();
            suiteWriter.insertTests(tests);

            String name = Properties.TARGET_CLASS.substring(Properties.TARGET_CLASS.lastIndexOf(".") + 1);
            String testDir = Properties.TEST_DIR;

            LOG.info("* Writing JUnit test case '" + (name + suffix) + "' to " + testDir);
            suiteWriter.writeTestSuite(name + suffix, testDir, testSuite.getLastExecutionResults());
        }
        return TestGenerationResultBuilder.buildSuccessResult();
    }



    public static void writeJUnitFailingTests() {
        if (!Properties.CHECK_CONTRACTS) {
            return;
        }

        FailingTestSet.sendStatistics();

        if (Properties.JUNIT_TESTS) {

            TestSuiteWriter suiteWriter = new TestSuiteWriter();
            //suiteWriter.insertTests(FailingTestSet.getFailingTests());

            TestSuiteChromosome suite = new TestSuiteChromosome();
            for(TestCase test : FailingTestSet.getFailingTests()) {
                test.setFailing();
                suite.addTest(test);
            }

            String name = Properties.TARGET_CLASS.substring(Properties.TARGET_CLASS.lastIndexOf(".") + 1);
            String testDir = Properties.TEST_DIR;
            LOG.info("* Writing failing test cases '" + (name + Properties.JUNIT_SUFFIX) + "' to " + testDir);
            suiteWriter.insertAllTests(suite.getTests());
            FailingTestSet.writeJUnitTestSuite(suiteWriter);

            suiteWriter.writeTestSuite(name + Properties.JUNIT_FAILED_SUFFIX, testDir, suite.getLastExecutionResults());
        }
    }
}
