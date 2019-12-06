package eu.stamp.cbc.testsuite.execution;

import eu.stamp.cbc.extraction.ClassLoader;
import eu.stamp.cbc.extraction.TestCaseRunListener;
import org.evosuite.Properties;
import org.evosuite.testcase.execution.ExecutionTracer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Executor {

    private static final Logger LOG = LoggerFactory.getLogger(Executor.class);

    private String givenTestSuite;
    final ClassLoader classLoader = new ClassLoader();
    final JUnitCore runner= new JUnitCore();


    public Executor(String givenTest, String caller, String callee){
        Properties.SELECTED_JUNIT = givenTest;
        this.givenTestSuite = givenTest;
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
        final Class<?> junitClass = classLoader.loadClass(givenTestSuite);
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
