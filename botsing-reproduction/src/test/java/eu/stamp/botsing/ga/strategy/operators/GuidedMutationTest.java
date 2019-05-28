package eu.stamp.botsing.ga.strategy.operators;

import static org.junit.Assert.assertNotEquals;
import static org.mockito.ArgumentMatchers.anyString;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.StackTrace;
import org.evosuite.Properties;
import org.evosuite.TestGenerationContext;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ConstructionFailedException;
import org.evosuite.graphs.cfg.ActualControlFlowGraph;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.BytecodeInstructionPool;
import org.evosuite.testcase.DefaultTestCase;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFactory;
import org.evosuite.testcase.statements.MethodStatement;
import org.evosuite.testcase.statements.numeric.IntPrimitiveStatement;
import org.evosuite.testcase.variable.VariableReference;
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
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public void testMutation() throws NoSuchMethodException, ConstructionFailedException, ClassNotFoundException, FileNotFoundException {
        TestCase testCase = getIntTest(20);
        TestChromosome chromosome = new TestChromosome();
        chromosome.setTestCase(testCase);
        // Mock target inst
        BytecodeInstruction instruction = Mockito.mock(BytecodeInstruction.class);
        Mockito.doReturn("java.lang.Integer").when(instruction).getClassName();
        Mockito.doReturn("reverse()").when(instruction).getMethodName();
        Mockito.doReturn(1).when(instruction).getLineNumber();
        ActualControlFlowGraph acfg = Mockito.mock(ActualControlFlowGraph.class);
        Mockito.doReturn(Opcodes.ACC_PUBLIC).when(acfg).getMethodAccess();
        Mockito.doReturn(acfg).when(instruction).getActualCFG();

        // Add taget inst
        BytecodeInstructionPool.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT()).registerInstruction(instruction);
        Properties.TARGET_CLASS = "java.lang.Integer";

        // Mock the given stack trace
        BufferedReader givenStackTrace = new BufferedReader(new StringReader("java.lang.IllegalArgumentException:\n" +
                "\tat eu.stamp.ClassA.method2(ClassA.java:10)\n" +
                "\tat eu.stamp.ClassB.method1(ClassB.java:20)"));
        StackTrace target = Mockito.spy(new StackTrace());
        Mockito.doReturn(givenStackTrace).when(target).readFromFile(anyString());
        Mockito.doReturn("java.lang.Integer").when(target).getTargetClass();
        Mockito.doReturn(1).when(target).getTargetLine();
        target.setup("", 2);
        CrashProperties.getInstance().setupStackTrace(target);

        TestChromosome clone = (TestChromosome) chromosome.clone();

        GenericAccessibleObject geObj = Mockito.mock(GenericAccessibleObject.class);
        Set<GenericAccessibleObject> publicCalls = new HashSet<>();
        publicCalls.add(geObj);

        GuidedMutation mutation = Mockito.spy(new GuidedMutation());
        mutation.updatePublicCalls(publicCalls);
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

        Method m = clazz.getRawClass().getMethod("reverse", int.class);
        GenericMethod method = new GenericMethod(m, sut);
        MethodStatement ms = new MethodStatement(test, method, callee, Arrays.asList(intVar));
        test.addStatement(ms);

        return test;
    }
}