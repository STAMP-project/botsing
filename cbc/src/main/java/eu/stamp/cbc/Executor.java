package eu.stamp.cbc;

import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import eu.stamp.botsing.commons.instrumentation.InstrumentingClassLoader;
import eu.stamp.cbc.extraction.TestCaseRunListener;
import org.evosuite.runtime.classhandling.ClassResetter;
import org.evosuite.testcase.execution.ExecutionTracer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.Set;
public class Executor {

    private static final Logger LOG = LoggerFactory.getLogger(Executor.class);

    private String givenTestSuite;
    final ClassLoader classLoader = BotsingTestGenerationContext.getInstance().getClassLoaderForSUT();
    JUnitCore runner= new JUnitCore();

    private String callerClass;


    public Executor(String givenTest, String caller, String callee){
        this.givenTestSuite = givenTest;
        this.callerClass = caller;

        // set thread checker in execution tracer
        ExecutionTracer.setCheckCallerThread(false);

        // Add listener
        prepareJUnitRunner();

        try {
            // instrument target classes
            Class callerClass = classLoader.loadClass(caller);
            Class calleeClass = classLoader.loadClass(callee);
            if (callerClass == null || calleeClass == null){
                throw new ClassNotFoundException();
            }
        } catch (final ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void execute(){
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
                if (junitClass == null){
                    throw new ClassNotFoundException();
                }
                Result result = runner.run(junitClass);
                LOG.info("Result: {}/{} failure", result.getFailureCount(), result.getRunCount());

            } catch ( ClassNotFoundException e) {
                LOG.warn("Test suite {} not found", givenTestSuite);
            }
    }

    private void prepareJUnitRunner() {
        // Add Listener (For now we are using EvoSuite listener)
        final TestCaseRunListener listener = new TestCaseRunListener();
        runner.addListener(listener);
    }
}
