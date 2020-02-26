package eu.stamp.cbc.testsuite.execution;

import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import eu.stamp.botsing.commons.instrumentation.InstrumentingClassLoader;
import eu.stamp.cbc.extraction.ExecutionTracePool;
import eu.stamp.cbc.extraction.TestCaseRunListener;
import org.evosuite.Properties;
import org.evosuite.runtime.classhandling.ClassResetter;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testcase.execution.ExecutionTrace;
import org.evosuite.testcase.execution.ExecutionTracer;
import org.evosuite.testcase.execution.TestCaseExecutor;
import org.evosuite.testsuite.TestSuiteSerialization;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
public class Executor {

    private static final Logger LOG = LoggerFactory.getLogger(Executor.class);

    private String givenTestSuite;
    final ClassLoader classLoader = BotsingTestGenerationContext.getInstance().getClassLoaderForSUT();
    JUnitCore runner= new JUnitCore();

    private String callerClass;
    private String calleeClass;


    public Executor(String givenTest, String caller, String callee){
        this.givenTestSuite = givenTest;
        this.callerClass = caller;
        this.calleeClass = callee;

        // set thread checker in execution tracer
        ExecutionTracer.setCheckCallerThread(false);

        // Add listener
        prepareJUnitRunner();

        try {
            // instrument target classes
            classLoader.loadClass(caller);
            classLoader.loadClass(callee);
        } catch (final ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void execute(Set<TestFitnessFunction> goalsSet){

        if (Properties.CTG_SEEDS_FILE_IN != null) {
            List<TestChromosome> previousSuite = new ArrayList<TestChromosome>();
            previousSuite.addAll(TestSuiteSerialization.loadTests(Properties.CTG_SEEDS_FILE_IN));

            int counter=0;
            for (TestChromosome prev: previousSuite){
                prev.getTestCase().removeAssertions();
                ExecutionResult execRes = TestCaseExecutor.runTest(prev.getTestCase());
                ExecutionTrace capturedTrace = execRes.getTrace();
                ExecutionTracePool.getInstance().registerNewCoverageData("Test"+counter,capturedTrace);
                counter++;
                LOG.info("Value: {}",execRes);
            }
        }else{
                // Set the classLoader for class reset to avoid static initialization problem
                ClassResetter.getInstance().setClassLoader(classLoader);
                try {

                // reset loaded classes for static initialization problem
                Set<String> loadedClasses= ((InstrumentingClassLoader) classLoader).getLoadedClasses();
                for (String loadedClassName: loadedClasses){
                    if (!loadedClassName.contains("$") && (callerClass.contains(loadedClassName))){
                        ClassResetter.getInstance().reset(loadedClassName);
                    }
                }
                // Run test
                final Class<?> junitClass = classLoader.loadClass(givenTestSuite);
                Result result = runner.run(junitClass);
                LOG.info("Result: {}/{} failure", result.getFailureCount(), result.getRunCount());

            } catch (final ClassNotFoundException e) {
                LOG.warn("Test suite {} not found", givenTestSuite);
            }
        }
    }

    private void prepareJUnitRunner() {
        // Add Listener (For now we are using EvoSuite listener)
        final TestCaseRunListener listener = new TestCaseRunListener();
        runner.addListener(listener);
    }
}
