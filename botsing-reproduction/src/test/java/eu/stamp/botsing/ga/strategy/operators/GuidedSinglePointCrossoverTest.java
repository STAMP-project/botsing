package eu.stamp.botsing.ga.strategy.operators;

import org.evosuite.Properties;
import org.evosuite.TestGenerationContext;
import org.evosuite.ga.ConstructionFailedException;
import org.evosuite.testcase.DefaultTestCase;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFactory;
import org.evosuite.testcase.statements.MethodStatement;
import org.evosuite.testcase.statements.StringPrimitiveStatement;
import org.evosuite.testcase.statements.numeric.IntPrimitiveStatement;
import org.evosuite.testcase.variable.VariableReference;
import org.evosuite.utils.generic.GenericClass;
import org.evosuite.utils.generic.GenericConstructor;
import org.evosuite.utils.generic.GenericMethod;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.Assert.*;

public class GuidedSinglePointCrossoverTest {

    @Test
    public void crossOver_successful() throws NoSuchMethodException, ConstructionFailedException, ClassNotFoundException {
        TestCase tc1 = getIntTest(20,"reverse");
        TestChromosome tch1 = new TestChromosome();
        tch1.setTestCase(tc1);

        TestCase tc2 = getIntTest(50,"reverseBytes");
        TestChromosome tch2 = new TestChromosome();
        tch2.setTestCase(tc2);

        TestChromosome parent1 = (TestChromosome) tch1.clone();
        TestChromosome parent2 = (TestChromosome) tch2.clone();

        GuidedSinglePointCrossover crossover = Mockito.spy(new GuidedSinglePointCrossover());
        Mockito.doReturn(true).when(crossover).isValid(tch1);
        Mockito.doReturn(true).when(crossover).isValid(tch2);
        crossover.crossOver(tch1, tch2);

        assertNotEquals(parent1.toString(), tch1.toString());
        assertNotEquals(parent2.toString(), tch2.toString());
    }

    @Test
    public void crossOver_unsuccessful() throws NoSuchMethodException, ConstructionFailedException, ClassNotFoundException {
        TestCase tc1 = getIntTest(20, "reverse");
        TestChromosome tch1 = new TestChromosome();
        tch1.setTestCase(tc1);

        TestCase tc2 = getIntTest(50,"reverseBytes");
        TestChromosome tch2 = new TestChromosome();
        tch2.setTestCase(tc2);

        TestChromosome parent1 = (TestChromosome) tch1.clone();
        TestChromosome parent2 = (TestChromosome) tch2.clone();

        GuidedSinglePointCrossover crossover = Mockito.spy(new GuidedSinglePointCrossover());
        Mockito.doReturn(false).when(crossover).isValid(tch1);
        Mockito.doReturn(false).when(crossover).isValid(tch2);
        crossover.crossOver(tch1, tch2);

        assertEquals(parent1.toString(), tch1.toString());
        assertEquals(parent2.toString(), tch2.toString());
    }

    @Test
    public void crossOver_emptyChromosome() throws NoSuchMethodException, ConstructionFailedException, ClassNotFoundException {
        TestCase tc1 = new DefaultTestCase();
        TestChromosome tch1 = new TestChromosome();
        tch1.setTestCase(tc1);

        TestCase tc2 = new DefaultTestCase();
        TestChromosome tch2 = new TestChromosome();
        tch2.setTestCase(tc2);

        TestChromosome parent1 = (TestChromosome) tch1.clone();
        TestChromosome parent2 = (TestChromosome) tch2.clone();

        GuidedSinglePointCrossover crossover = Mockito.spy(new GuidedSinglePointCrossover());
        Mockito.doReturn(false).when(crossover).isValid(tch1);
        Mockito.doReturn(false).when(crossover).isValid(tch2);
        crossover.crossOver(tch1, tch2);

        assertEquals(parent1.toString(), tch1.toString());
        assertEquals(parent2.toString(), tch2.toString());
    }

    private TestCase getIntTest(int x, String methodName) throws NoSuchMethodException, SecurityException, ConstructionFailedException, ClassNotFoundException {
        Properties.RANDOM_SEED =(long) 1;
        Class<?> sut = TestGenerationContext.getInstance().getClassLoaderForSUT().loadClass("java.lang.Integer");
        GenericClass clazz = new GenericClass(sut);

        DefaultTestCase test = new DefaultTestCase();
        GenericConstructor gc = new GenericConstructor(clazz.getRawClass().getConstructors()[0], clazz);

        TestFactory testFactory = TestFactory.getInstance();
        VariableReference callee = testFactory.addConstructor(test, gc, 0, 0);
        VariableReference intVar = test.addStatement(new IntPrimitiveStatement(test, x));

        Method m = clazz.getRawClass().getMethod(methodName, new Class<?>[] { int.class});
        GenericMethod method = new GenericMethod(m, sut);
        MethodStatement ms = new MethodStatement(test, method, callee, Arrays.asList(new VariableReference[] {intVar}));
        test.addStatement(ms);

        return test;
    }
}