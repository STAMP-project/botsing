package eu.stamp.botsing.reproduction;

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
import eu.stamp.botsing.graphs.cfg.CFGGenerator;
import org.evosuite.Properties;
import org.evosuite.TimeController;
import org.evosuite.classpath.ClassPathHandler;
import org.evosuite.contracts.FailingTestSet;
import org.evosuite.coverage.TestFitnessFactory;
import org.evosuite.junit.JUnitAnalyzer;
import org.evosuite.junit.writer.TestSuiteWriter;
import org.evosuite.result.TestGenerationResult;
import org.evosuite.result.TestGenerationResultBuilder;
import org.evosuite.rmi.ClientServices;
import org.evosuite.rmi.service.ClientState;
import org.evosuite.runtime.LoopCounter;
import org.evosuite.runtime.sandbox.Sandbox;
import org.evosuite.setup.DependencyAnalysis;
import org.evosuite.statistics.RuntimeVariable;
import org.evosuite.strategy.TestGenerationStrategy;
import org.evosuite.testcase.ConstantInliner;
import org.evosuite.testcase.DefaultTestCase;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testcase.execution.ExecutionTrace;
import org.evosuite.testcase.execution.ExecutionTracer;
import org.evosuite.testcase.execution.TestCaseExecutor;
import org.evosuite.testcase.execution.reset.ClassReInitializer;
import org.evosuite.testcase.statements.MethodStatement;
import org.evosuite.testcase.statements.Statement;
import org.evosuite.testcase.statements.StringPrimitiveStatement;
import org.evosuite.testcase.statements.numeric.BooleanPrimitiveStatement;
import org.evosuite.testcase.variable.VariableReference;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.testsuite.TestSuiteMinimizer;
import org.evosuite.utils.generic.GenericMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;

public class CrashReproduction {
    private static final Logger LOG = LoggerFactory.getLogger(CrashReproduction.class);


    public static List<TestGenerationResult> execute(){
        CrashProperties crashProperties = CrashProperties.getInstance();
        List<TestGenerationResult> generatedTests = new ArrayList<TestGenerationResult>();


        //Before starting search, activate the sandbox
        if (crashProperties.getBooleanValue("sandbox")) {
            Sandbox.initializeSecurityManagerForSUT();
        }


        generatedTests.add(generateCrashReproductionTests());


        // Resetting sandbox after test generation
        if (crashProperties.getBooleanValue("sandbox")) {
            Sandbox.resetDefaultSecurityManager();
        }


        return generatedTests;
    }


    private static TestGenerationResult generateCrashReproductionTests(){
//        List<TestGenerationResult> result= new ArrayList<>();
        ClientServices.getInstance().getClientNode().changeState(ClientState.INITIALIZATION);

        // Deactivate loop counter to make sure classes initialize properly
        LoopCounter.getInstance().setActive(false);

        //Initialize EvoSuite test executor
        TestCaseExecutor.initExecutor();

        // In the first step initialize the target class
        try{
//            analyzeClassPaths();
            if(CrashProperties.integrationTesting){
                initializeMultipleTargetClasses();
            }else{
                initializeTargetClass();
            }
        }catch (Exception e){
            LOG.error("Error in target initialization:");
            e.printStackTrace();
        }finally {
            if (CrashProperties.getInstance().getBooleanValue("reset_static_fields")) {
                configureClassReInitializer();
            }
            LoopCounter.getInstance().setActive(true);
        }

        // For seeding we should initializing the pool here


        if (!Properties.hasTargetClassBeenLoaded()) {
            // initialization failed, then build error message
            return TestGenerationResultBuilder.buildErrorResult("Could not load target class");
        }


        TestGenerationStrategy strategy = CrashReproductionHelper.getTestGenerationFactory();
        TestSuiteChromosome testCases = strategy.generateTests();

        postProcessTests(testCases);

        TestGenerationResult writingTest = writeJUnitTestsAndCreateResult(testCases, Properties.JUNIT_SUFFIX);
        writeJUnitFailingTests();
        TestCaseExecutor.pullDown();

        LOG.info("The solution test is saved!");

        return writingTest;

    }

    private static void analyzeClassPaths() throws ClassNotFoundException{
        String cp = ClassPathHandler.getInstance().getTargetProjectClasspath();
        List<String> cpList = Arrays.asList(cp.split(File.pathSeparator));
        LOG.info("Starting the dependency analysis. The number of detected jar files is {}.",cpList.size());
        DependencyAnalysis.analyzeClass(CrashProperties.getInstance().getStackTrace().getTargetClass(),Arrays.asList(cp.split(File.pathSeparator)));
        LOG.info("Analysing dependencies done!");
    }

    private static void initializeMultipleTargetClasses() {
        CFGGenerator cfgGenerator = new CFGGenerator();
        cfgGenerator.generateInterProceduralCFG();
    }

    private static void postProcessTests(TestSuiteChromosome testSuite) {

        if (Properties.INLINE) {
            ConstantInliner inliner = new ConstantInliner();
            inliner.inline(testSuite);
        }


        if (Properties.MINIMIZE) {
            double before = testSuite.getFitness();

            TestSuiteMinimizer minimizer = new TestSuiteMinimizer(getFitnessFactories());

            LOG.info("* Minimizing test suite");
            minimizer.minimize(testSuite, true);

            double after = testSuite.getFitness();
            if (after > before + 0.01d) { // assume minimization
                throw new Error("EvoSuite bug: minimization lead fitness from " + before + " to " + after);
            }
        }


        compileAndCheckTests(testSuite);
    }

    private static List<TestFitnessFactory<? extends TestFitnessFunction>> getFitnessFactories() {
        List<TestFitnessFactory<? extends TestFitnessFunction>> goalsFactory = new ArrayList<TestFitnessFactory<? extends TestFitnessFunction>>();
        goalsFactory.add(new CrashReproductionGoalFactory());
        return goalsFactory;
    }

    private static void configureClassReInitializer() {
        ExecutionTrace execTrace = ExecutionTracer.getExecutionTracer().getTrace();
        final List<String> initializedClasses = execTrace.getInitializedClasses();
        ClassReInitializer.getInstance().addInitializedClasses(initializedClasses);
        ClassReInitializer.getInstance().setReInitializeAllClasses(CrashProperties.getInstance().getBooleanValue("reset_all_classes_during_test_generation"));
    }


    private static void initializeTargetClass() throws ClassNotFoundException {
        String cp = ClassPathHandler.getInstance().getTargetProjectClasspath();
        DefaultTestCase test = generateTestForLoadingClass(CrashProperties.getInstance().getStackTrace().getTargetClass());

        // execute the test contains the target class
        ExecutionResult execResult = TestCaseExecutor.getInstance().execute(test, Integer.MAX_VALUE);

        if (hasThrownInitializerError(execResult)) {
            // create single test suite with Class.forName()
            ExceptionInInitializerError ex = getInitializerError(execResult);
            throw ex;
        } else if (!execResult.getAllThrownExceptions().isEmpty()) {
            // some other exception has been thrown during initialization
            Throwable t = execResult.getAllThrownExceptions().iterator().next();
            try {
                throw t;
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
        List<String> cpList = Arrays.asList(cp.split(File.pathSeparator));
        LOG.info("Starting the dependency analysis. The number of detected jar files is {}.",cpList.size());
        DependencyAnalysis.analyzeClass(CrashProperties.getInstance().getStackTrace().getTargetClass(),Arrays.asList(cp.split(File.pathSeparator)));
        LOG.info("Analysing dependencies done!");
    }
    private static ExceptionInInitializerError getInitializerError(ExecutionResult execResult) {
        for (Throwable t : execResult.getAllThrownExceptions()) {
            if (t instanceof ExceptionInInitializerError) {
                ExceptionInInitializerError exceptionInInitializerError = (ExceptionInInitializerError)t;
                return exceptionInInitializerError;
            }
        }
        return null;
    }
    private static boolean hasThrownInitializerError(ExecutionResult execResult) {
        for (Throwable t : execResult.getAllThrownExceptions()) {
            if (t instanceof ExceptionInInitializerError) {
                return true;
            }
        }
        return false;
    }

    private static DefaultTestCase generateTestForLoadingClass(String targetClass) {

        DefaultTestCase test = new DefaultTestCase();
        StringPrimitiveStatement firstStatement = new StringPrimitiveStatement(test, targetClass);
        VariableReference string0 = test.addStatement(firstStatement);

        try{

            Method currentThreadMethod = Thread.class.getMethod("currentThread");
            Statement currentThreadStmt = new MethodStatement(test,
                    new GenericMethod(currentThreadMethod, currentThreadMethod.getDeclaringClass()), null,
                    Collections.emptyList());
            VariableReference currentThreadVar = test.addStatement(currentThreadStmt);

            Method getContextClassLoaderMethod = Thread.class.getMethod("getContextClassLoader");
            Statement getContextClassLoaderStmt = new MethodStatement(test,
                    new GenericMethod(getContextClassLoaderMethod, getContextClassLoaderMethod.getDeclaringClass()),
                    currentThreadVar, Collections.emptyList());
            VariableReference contextClassLoaderVar = test.addStatement(getContextClassLoaderStmt);

			Method loadClassMethod = ClassLoader.class.getMethod("loadClass", String.class);
			Statement loadClassStmt = new MethodStatement(test,
					new GenericMethod(loadClassMethod, loadClassMethod.getDeclaringClass()), contextClassLoaderVar,
					Collections.singletonList(string0));
			test.addStatement(loadClassStmt);

            BooleanPrimitiveStatement stmt1 = new BooleanPrimitiveStatement(test, true);
            VariableReference boolean0 = test.addStatement(stmt1);

            Method forNameMethod = Class.class.getMethod("forName",String.class, boolean.class, ClassLoader.class);
            Statement forNameStmt = new MethodStatement(test,
                    new GenericMethod(forNameMethod, forNameMethod.getDeclaringClass()), null,
                    Arrays.asList(string0, boolean0, contextClassLoaderVar));
            test.addStatement(forNameStmt);
        }catch(Exception e){
            LOG.error("! Error in loading the target class:");
            e.printStackTrace();
        }
        return test;
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
