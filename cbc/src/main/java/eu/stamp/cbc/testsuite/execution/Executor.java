package eu.stamp.cbc.testsuite.execution;

import eu.stamp.cbc.extraction.ClassLoader;
import eu.stamp.cbc.extraction.TestCaseRunListener;
import org.evosuite.Properties;
import org.evosuite.testcarver.capture.FieldRegistry;
import org.evosuite.testcarver.extraction.CarvingRunListener;
import org.evosuite.testcase.execution.ExecutionTracer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Executor {

    private static final Logger LOG = LoggerFactory.getLogger(Executor.class);
    final JUnitCore runner= new JUnitCore();


    public Executor(String givenTest){
        Properties.SELECTED_JUNIT = givenTest;
        prepareJUnitRunner();

        final ClassLoader classLoader = new ClassLoader();

        try {
            // instrument target class
            //instrument caller
            classLoader.loadClass("org.apache.commons.lang3.text.translate.UnicodeEscaper");
            //instrument callee
            classLoader.loadClass("org.apache.commons.lang3.text.translate.CharSequenceTranslator");
//            classLoader.loadClass("org.apache.commons.lang3.text.translate.UnicodeEscaper_ESTest_scaffolding");

//            final Class<?> junitClass = this.getClass().getClassLoader().loadClass(givenTest);

//            classLoader.getResource("/Users/pooria/IntelliJ\\ Projects/botsing/cbc/src/test/java/eu/stamp/cbc/local/evosuite-standalone-runtime-1.0.6.jar");

            ExecutionTracer.enable();
            final Class<?> junitClass = classLoader.loadClass(givenTest);
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
