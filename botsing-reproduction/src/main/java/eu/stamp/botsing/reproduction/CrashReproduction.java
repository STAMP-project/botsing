package eu.stamp.botsing.reproduction;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.setup.botsingDependencyAnalysor;
import org.evosuite.Properties;
import org.evosuite.classpath.ClassPathHandler;
import org.evosuite.result.TestGenerationResult;
import org.evosuite.result.TestGenerationResultBuilder;
import org.evosuite.rmi.ClientServices;
import org.evosuite.rmi.service.ClientState;
import org.evosuite.runtime.LoopCounter;
import org.evosuite.runtime.sandbox.Sandbox;
import org.evosuite.strategy.TestGenerationStrategy;
import org.evosuite.testcase.DefaultTestCase;
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
import org.evosuite.utils.generic.GenericMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CrashReproduction {
    private static final Logger LOG = LoggerFactory.getLogger(CrashReproduction.class);

    public static List<List<TestGenerationResult>> execute(){
        CrashProperties crashProperties = CrashProperties.getInstance();
        List<List<TestGenerationResult>> generatedTests = new ArrayList<List<TestGenerationResult>>();


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


    private static List<TestGenerationResult> generateCrashReproductionTests(){
        List<TestGenerationResult> result= new ArrayList<>();
        ClientServices.getInstance().getClientNode().changeState(ClientState.INITIALIZATION);

        // Deactivate loop counter to make sure classes initialize properly
        LoopCounter.getInstance().setActive(false);

        //Initialize EvoSuite test executor
        TestCaseExecutor.initExecutor();

        // In the first step initialize the target class
        try{
            initializeTargetClass();
        }catch (Exception e){
            LOG.error("Error in target initialization:");
            e.printStackTrace();
        }finally {
            if (CrashProperties.getBooleanValue("reset_static_fields")) {
                configureClassReInitializer();
            }
            LoopCounter.getInstance().setActive(true);
        }

        // For seeding we should initializing the pool here


        if (!Properties.hasTargetClassBeenLoaded()) {
            // initialization failed, then build error message
            result.add(TestGenerationResultBuilder.buildErrorResult("Could not load target class"));
            return result;
        }


        TestGenerationStrategy strategy = CrashReproductionHelper.getTestGenerationFactory();
        TestSuiteChromosome testCases = strategy.generateTests();

//        postProcessTests(testCases);
//        ClientServices.getInstance().getClientNode().publishPermissionStatistics();
//        PermissionStatistics.getInstance().printStatistics(LoggingUtils.getEvoLogger());
//
//        // progressMonitor.setCurrentPhase("Writing JUnit test cases");
//        TestGenerationResult result = writeJUnitTestsAndCreateResult(testCases);
//        writeJUnitFailingTests();
//        TestCaseExecutor.pullDown();
//        /*
//         * TODO: when we will have several processes running in parallel, we ll
//         * need to handle the gathering of the statistics.
//         */
//        ClientServices.getInstance().getClientNode().changeState(ClientState.WRITING_STATISTICS);
//
//        LoggingUtils.getEvoLogger().info("* Done!");
//        LoggingUtils.getEvoLogger().info("");
//
//        return result;

        return null;
    }

    private static void configureClassReInitializer() {
        ExecutionTrace execTrace = ExecutionTracer.getExecutionTracer().getTrace();
        final List<String> initializedClasses = execTrace.getInitializedClasses();
        ClassReInitializer.getInstance().addInitializedClasses(initializedClasses);
        ClassReInitializer.getInstance().setReInitializeAllClasses(CrashProperties.getBooleanValue("reset_all_classes_during_test_generation"));
    }


    private static void initializeTargetClass() throws ClassNotFoundException {
        String cp = ClassPathHandler.getInstance().getTargetProjectClasspath();
        LOG.info("The target class path is: "+cp );
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
        botsingDependencyAnalysor.analyzeClass(CrashProperties.getInstance().getStackTrace().getTargetClass(),Arrays.asList(cp.split(File.pathSeparator)));
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
                    Arrays.<VariableReference>asList(string0, boolean0, contextClassLoaderVar));
            test.addStatement(forNameStmt);
        }catch(Exception e){
            LOG.error("! Error in loading the target class:");
            e.printStackTrace();
        }
        return test;
    }
}
