package eu.stamp.botsing.ga.strategy.operators;

import org.evosuite.Properties;
import org.evosuite.TestGenerationContext;
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
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import static org.junit.Assert.*;

public class GuidedMutationTest {

    @Test
    public void testMutation() throws NoSuchMethodException, ConstructionFailedException, ClassNotFoundException {
        TestCase testCase = getIntTest(20);
        TestChromosome chromosome = new TestChromosome();
        chromosome.setTestCase(testCase);

        Properties.TARGET_CLASS = "java.lang.Integer";

        TestChromosome clone = (TestChromosome) chromosome.clone();

        GuidedMutation mutation = new GuidedMutation();
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