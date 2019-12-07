package eu.stamp.cbc.testsuite.execution;

import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import eu.stamp.cbc.extraction.TestCaseRunListener;
import org.evosuite.testcase.execution.ExecutionTracer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Executor {

    private static final Logger LOG = LoggerFactory.getLogger(Executor.class);

    private String givenTestSuite;
    final ClassLoader classLoader = BotsingTestGenerationContext.getInstance().getClassLoaderForSUT();
    final JUnitCore runner= new JUnitCore();


    public Executor(String givenTest, String caller, String callee){
        this.givenTestSuite = givenTest;

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

    public void execute(){
        ExecutionTracer.enable();
        try {
            // load the given test
        final Class<?> junitClass = classLoader.loadClass(givenTestSuite);
        // Run ...
        Result result = runner.run(junitClass);
        LOG.info("Result: {}/{} failure", result.getFailureCount(), result.getRunCount());
        } catch (final ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void prepareJUnitRunner() {
        // Add Listener (For now we are using EvoSuite listener)
        final TestCaseRunListener listener = new TestCaseRunListener();
        runner.addListener(listener);
    }
}
