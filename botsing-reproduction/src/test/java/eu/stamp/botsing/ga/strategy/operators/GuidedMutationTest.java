package eu.stamp.botsing.ga.strategy.operators;

import org.evosuite.Properties;
import org.evosuite.TestGenerationContext;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ConstructionFailedException;
import org.evosuite.testcase.DefaultTestCase;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFactory;
import org.evosuite.testcase.statements.ConstructorStatement;
import org.evosuite.testcase.statements.MethodStatement;
import org.evosuite.testcase.statements.StringPrimitiveStatement;
import org.evosuite.testcase.statements.numeric.IntPrimitiveStatement;
import org.evosuite.testcase.variable.VariableReference;
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

import java.lang.reflect.Method;
import java.util.Arrays;
import static org.junit.Assert.*;

public class GuidedMutationTest {

    private static final Logger LOG = LoggerFactory.getLogger(GuidedMutationTest.class);

    @Rule
    public TestRule watcher = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            LOG.info(String.format("Starting test: %s()...",
                    description.getMethodName()));
        }
    };

    @Before
    public void initialize(){
        Properties.RANDOM_SEED =(long) 1;
    }

    @Test
    public void testMutation() throws NoSuchMethodException, ConstructionFailedException, ClassNotFoundException {
        TestCase testCase = getIntTest(20);
        TestChromosome chromosome = new TestChromosome();
        chromosome.setTestCase(testCase);

        Properties.TARGET_CLASS = "java.lang.Integer";

        TestChromosome clone = (TestChromosome) chromosome.clone();

        GuidedMutation mutation = Mockito.spy(new GuidedMutation());
        Mockito.doNothing().when(mutation).insertRandomStatement(Mockito.any(Chromosome.class));
        Mockito.doNothing().when(mutation).doRandomMutation(Mockito.any(Chromosome.class));
        mutation.mutateOffspring(chromosome);

        assertNotEquals(clone, chromosome);
    }

    private TestCase getIntTest(int x) throws NoSuchMethodException, SecurityException, ConstructionFailedException, ClassNotFoundException {
        Class<?> sut = TestGenerationContext.getInstance().getClassLoaderForSUT().loadClass("java.lang.Integer");
        GenericClass clazz = new GenericClass(sut);

        DefaultTestCase test = new DefaultTestCase();
        GenericConstructor gc = new GenericConstructor(clazz.getRawClass().getConstructors()[0], clazz);

        TestFactory testFactory = TestFactory.getInstance();
        VariableReference callee = testFactory.addConstructor(test, gc, 0, 0);
        VariableReference intVar = test.addStatement(new IntPrimitiveStatement(test, x));

        Method m = clazz.getRawClass().getMethod("reverse", new Class<?>[] { int.class});
        GenericMethod method = new GenericMethod(m, sut);
        MethodStatement ms = new MethodStatement(test, method, callee, Arrays.asList(new VariableReference[] {intVar}));
        test.addStatement(ms);

        return test;
    }
}