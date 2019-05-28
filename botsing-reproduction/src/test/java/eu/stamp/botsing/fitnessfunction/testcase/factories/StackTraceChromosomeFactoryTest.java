package eu.stamp.botsing.fitnessfunction.testcase.factories;

import ch.qos.logback.classic.Level;
import eu.stamp.botsing.ga.strategy.operators.GuidedSearchUtility;
import org.evosuite.Properties;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.setup.TestCluster;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.utils.generic.GenericAccessibleObject;
import org.evosuite.utils.generic.GenericClass;
import org.evosuite.utils.generic.GenericConstructor;
import org.evosuite.utils.generic.GenericMethod;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class StackTraceChromosomeFactoryTest {

    private static final Logger LOG = LoggerFactory.getLogger(StackTraceChromosomeFactoryTest.class);

    @Rule
    public TestRule watcher = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            LOG.info(String.format("Starting test: %s()...",
                    description.getMethodName()));
        }
    };

    @Before
    public void initialize() {
        Properties.RANDOM_SEED = (long) 1;
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
    }


    @Test
    public void testChromosomeMethod() throws NoSuchMethodException {
        eu.stamp.botsing.StackTrace trace = Mockito.mock(eu.stamp.botsing.StackTrace.class);
        Mockito.when(trace.getTargetClass()).thenReturn("ClassA");
        Mockito.when(trace.getTargetLine()).thenReturn(12);

        Object obj = new String();
        Class<?>[] classes = new Class<?>[1];
        classes[0] = String.class;
        GenericClass gc = Mockito.mock(GenericClass.class);
        Mockito.when(gc.hasWildcardOrTypeVariables()).thenReturn(false);
        Method m = obj.getClass().getMethod("equals", Object.class);
        GenericMethod call = Mockito.mock(GenericMethod.class);
        Mockito.when(call.getName()).thenReturn("equals");
        Mockito.when(call.getOwnerClass()).thenReturn(gc);
        Mockito.when(call.isMethod()).thenReturn(true);
        Mockito.when(call.getOwnerType()).thenReturn(String.class);
        Mockito.when(call.getMethod()).thenReturn(m);
        Mockito.when(call.getParameterTypes()).thenReturn(m.getParameterTypes());
        Mockito.when(call.getReturnType()).thenReturn(Boolean.TYPE);
        TestCluster.getInstance().addTestCall(call);

        GuidedSearchUtility utility = Mockito.mock(GuidedSearchUtility.class);
        BytecodeInstruction bc = Mockito.mock(BytecodeInstruction.class);
        Mockito.when(bc.getClassName()).thenReturn(Integer.class.getName());
        Mockito.when(bc.getMethodName()).thenReturn("doubleValue()D");
        Mockito.when(utility.collectPublicCalls(Mockito.any())).thenReturn(bc);
        StackTraceChromosomeFactory rm = new StackTraceChromosomeFactory(trace, utility);
        TestChromosome generatedChromosome = rm.getChromosome();
        assertFalse(generatedChromosome.getTestCase().isEmpty());
        assertTrue(generatedChromosome.getTestCase().isValid());
//        assertEquals();
        assertEquals(rm.getPublicCalls().size(),1);
        assert (rm.getPublicCalls().toArray()[0] instanceof GenericMethod);
        assertEquals (((GenericAccessibleObject)rm.getPublicCalls().toArray()[0]).getName(),"doubleValue");



        Mockito.when(bc.getMethodName()).thenReturn("<init>(I)V");
        Mockito.when(utility.collectPublicCalls(Mockito.any())).thenReturn(bc);
        rm = new StackTraceChromosomeFactory(trace, utility);
        assertEquals(rm.getPublicCalls().size(),1);
        assert (rm.getPublicCalls().toArray()[0] instanceof GenericConstructor);


    }

}
