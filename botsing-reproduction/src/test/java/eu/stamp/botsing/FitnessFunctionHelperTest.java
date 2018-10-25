package eu.stamp.botsing;

import eu.stamp.botsing.fitnessfunction.FitnessFunctionHelper;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FitnessFunctionHelperTest {

    private static final Logger LOG = LoggerFactory.getLogger(FitnessFunctionHelperTest.class);

    @Rule
    public TestRule watcher = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            LOG.info(String.format("Starting test: %s()...",
                    description.getMethodName()));
        }
    };

    private FitnessFunctionHelper fitnessFunctionHelper = new FitnessFunctionHelper();

    @Test
    public void testIsConstructor() {
        BytecodeInstruction bi = Mockito.mock(BytecodeInstruction.class);
        Mockito.when(bi.getMethodName()).thenReturn("methodA()");
        Mockito.when(bi.getClassName()).thenReturn("eu.stamp.ClassA");
        assertFalse(fitnessFunctionHelper.isConstructor(bi));

        Mockito.when(bi.getMethodName()).thenReturn("ClassA()");
        Mockito.when(bi.getClassName()).thenReturn("eu.stamp.ClassA");
        assertTrue(fitnessFunctionHelper.isConstructor(bi));


        Mockito.when(bi.getMethodName()).thenReturn("methodA()");
        Mockito.when(bi.getClassName()).thenReturn("");
        assertFalse(fitnessFunctionHelper.isConstructor(bi));
    }
}