package eu.stamp.botsing.fitnessfunction.testcase.factories;

import ch.qos.logback.classic.Level;
import eu.stamp.botsing.StackTrace;
import eu.stamp.botsing.ga.strategy.operators.GuidedSearchUtility;
import org.evosuite.Properties;
import org.evosuite.setup.TestCluster;
import org.evosuite.testcase.TestChromosome;
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class RootMethodTestChromosomeFactoryTest {

    private static final Logger LOG = LoggerFactory.getLogger(RootMethodTestChromosomeFactoryTest.class);

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
        StackTrace trace = Mockito.mock(StackTrace.class);
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
        Set<String> publicCalls = new HashSet<>();
        publicCalls.add("byteValue");
        publicCalls.add("doubleValue");
        publicCalls.add("equal");
        Mockito.when(utility.getPublicCalls(Mockito.anyString(),Mockito.anyInt())).thenReturn(publicCalls);
        RootMethodTestChromosomeFactory rm = new RootMethodTestChromosomeFactory(trace, utility);
        TestChromosome generatedChromosome = rm.getChromosome();
        assertFalse(generatedChromosome.getTestCase().isEmpty());
        assertTrue(generatedChromosome.getTestCase().isValid());


        ///
        Object obj2 = new Boolean(true);
        Class<?>[] classes2 = new Class<?>[1];
        classes2[0] = String.class;
        GenericClass gc2 = Mockito.mock(GenericClass.class);
        Mockito.when(gc2.hasWildcardOrTypeVariables()).thenReturn(false);
        Constructor c = obj2.getClass().getConstructors()[0];
        ///

        GenericConstructor call2 = Mockito.mock(GenericConstructor.class);
        Mockito.when(call2.getName()).thenReturn(c.getName());
        Mockito.when(call2.getOwnerClass()).thenReturn(gc2);
        Mockito.when(call2.isMethod()).thenReturn(false);
        Mockito.when(call2.isConstructor()).thenReturn(true);
        Mockito.when(call2.getOwnerType()).thenReturn(String.class);
        Mockito.when(call2.getConstructor()).thenReturn(c);
        Mockito.when(call2.getRawGeneratedType()).thenReturn(c.getParameterTypes()[0]);
        Mockito.when(call2.getReturnType()).thenReturn(Boolean.TYPE);
        TestCluster.getInstance().addTestCall(call2);

        publicCalls.add(c.getName());

        rm = new RootMethodTestChromosomeFactory(trace, utility);

        generatedChromosome = rm.getChromosome();

//        fail("Should have sent Exception by now!");

    }

}
