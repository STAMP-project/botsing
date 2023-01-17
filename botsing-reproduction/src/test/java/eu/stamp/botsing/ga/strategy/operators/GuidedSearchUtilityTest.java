package eu.stamp.botsing.ga.strategy.operators;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.*;

import org.evosuite.Properties;
import org.evosuite.TestGenerationContext;
import org.evosuite.ga.ConstructionFailedException;
import org.evosuite.graphs.cfg.ActualControlFlowGraph;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.BytecodeInstructionPool;
import org.evosuite.graphs.cfg.RawControlFlowGraph;
import org.evosuite.testcase.DefaultTestCase;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFactory;
import org.evosuite.testcase.statements.MethodStatement;
import org.evosuite.testcase.statements.numeric.BooleanPrimitiveStatement;
import org.evosuite.testcase.statements.numeric.IntPrimitiveStatement;
import org.evosuite.testcase.variable.VariableReference;
import org.evosuite.utils.generic.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.evosuite.shaded.org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.stamp.botsing.StackTrace;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;

public class GuidedSearchUtilityTest {

    private static final Logger LOG = LoggerFactory.getLogger(GuidedSearchUtilityTest.class);

    @Rule
    public TestRule watcher = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            LOG.info(String.format("Starting test: %s()...",
                    description.getMethodName()));
        }
    };

    private TestChromosome chromosome;
    private StackTrace trace;

    @Before
    public void init() throws ConstructionFailedException, NoSuchMethodException, ClassNotFoundException {
        Properties.RANDOM_SEED =(long) 1;

        TestCase tc = initializeTestCase();
        this.chromosome = new TestChromosome();
        this.chromosome.setTestCase(tc);

        this.trace = initializeStackTrace();

    }

    private TestCase initializeTestCase() throws ConstructionFailedException, NoSuchMethodException, ClassNotFoundException {
        Class<?> sut = TestGenerationContext.getInstance().getClassLoaderForSUT().loadClass("java.lang.Integer");
        GenericClass clazz = new GenericClassImpl(sut);

        DefaultTestCase test = new DefaultTestCase();
        GenericConstructor gc = new GenericConstructor(clazz.getRawClass().getConstructors()[0], clazz);

        TestFactory testFactory = TestFactory.getInstance();
        VariableReference callee = testFactory.addConstructor(test, gc, 0, 0);
        VariableReference intVar = test.addStatement(new IntPrimitiveStatement(test, 10));

        Method m = clazz.getRawClass().getMethod("reverse", int.class);
        GenericMethod method = new GenericMethod(m, sut);
        MethodStatement ms = new MethodStatement(test, method, callee, Arrays.asList(intVar));
        test.addStatement(ms);

        GenericMethod method2 = new GenericMethod(m, sut);
        MethodStatement ms2 = new MethodStatement(test, method2, callee, Arrays.asList(intVar));
        test.addStatement(ms2);

        return test;
    }

    private StackTrace initializeStackTrace(){
        StackTrace stackTrace = Mockito.mock(StackTrace.class);
        Mockito.doReturn("java.lang.Integer").when(stackTrace).getTargetClass();
        Mockito.doReturn("reverse").when(stackTrace).getTargetMethod();
        Mockito.doReturn(1).when(stackTrace).getTargetLine();
        Mockito.doReturn(1).when(stackTrace).getTargetFrameLevel();
        return stackTrace;
    }

    @Test
    public void testGetPublicCalls_trueCase() {
        GuidedSearchUtility utility = new GuidedSearchUtility();

        BytecodeInstruction stmt = Mockito.mock(BytecodeInstruction.class);
        Mockito.doReturn("java.lang.Integer").when(stmt).getClassName();
        Mockito.doReturn("reverse()").when(stmt).getMethodName();
        Mockito.doReturn(1).when(stmt).getLineNumber();

        List<BytecodeInstruction> instructions = new ArrayList<>();
        instructions.add(stmt);

        ActualControlFlowGraph actualCFG = mockActualControlFlowGraph(stmt, true);
        Mockito.doReturn(actualCFG).when(stmt).getActualCFG();

        utility.getPublicCalls(trace.getTargetClass(),trace.getTargetLine(), instructions);
        assertEquals(1,utility.publicCallsBC.size());
        assertEquals (((BytecodeInstruction)utility.publicCallsBC.toArray()[0]).getClassName(),"java.lang.Integer");
        assertEquals (((BytecodeInstruction)utility.publicCallsBC.toArray()[0]).getMethodName(),"reverse()");
        assertEquals (((BytecodeInstruction)utility.publicCallsBC.toArray()[0]).getLineNumber(),1);
    }

    @Test
    public void testGetPublicCalls_falseCase() {
        BytecodeInstruction stmt = Mockito.mock(BytecodeInstruction.class);
        Mockito.doReturn("java.lang.Integer").when(stmt).getClassName();
        Mockito.doReturn("reverse()").when(stmt).getMethodName();
        Mockito.doReturn(1).when(stmt).getLineNumber();

        List<BytecodeInstruction> instructions = new ArrayList<>();
        instructions.add(stmt);

        ActualControlFlowGraph actualCFG = mockActualControlFlowGraph(stmt, false);
        Mockito.doReturn(actualCFG).when(stmt).getActualCFG();

        assertFalse(actualCFG.isPublicMethod());

        GuidedSearchUtility utility = Mockito.spy(new GuidedSearchUtility());
        Mockito.doNothing().when(utility).searchForNonPrivateMethods(Mockito.any(BytecodeInstruction.class), Mockito.anyString());
        utility.getPublicCalls(trace.getTargetClass(),trace.getTargetLine(), instructions);
        assertEquals(0,utility.publicCallsBC.size());
    }

    @Test
    public void testGetPublicCalls_constructor() {
        BytecodeInstruction stmt = Mockito.mock(BytecodeInstruction.class);
        Mockito.doReturn("java.lang.Integer").when(stmt).getClassName();
        Mockito.doReturn("Integer()").when(stmt).getMethodName();
        Mockito.doReturn(1).when(stmt).getLineNumber();

        List<BytecodeInstruction> instructions = new ArrayList<>();
        instructions.add(stmt);

        ActualControlFlowGraph actualCFG = mockActualControlFlowGraph(stmt, true);
        Mockito.doReturn(actualCFG).when(stmt).getActualCFG();

        GuidedSearchUtility utility = Mockito.spy(new GuidedSearchUtility());
        Mockito.doNothing().when(utility).searchForNonPrivateMethods(Mockito.any(BytecodeInstruction.class), Mockito.anyString());
        utility.getPublicCalls(trace.getTargetClass(),trace.getTargetLine(), instructions);
        assertEquals(1,utility.publicCallsBC.size());

        Iterator<BytecodeInstruction> iterator = utility.publicCallsBC.iterator();
        assertEquals("java.lang.Integer",iterator.next().getClassName());
    }

    @Test
    public void testIncludesPublicCall_falseCase()  {
        BytecodeInstruction stmt = Mockito.mock(BytecodeInstruction.class);
        Mockito.doReturn("java.lang.Integer").when(stmt).getClassName();
        Mockito.doReturn("reverse()").when(stmt).getMethodName();
        Mockito.doReturn(1).when(stmt).getLineNumber();

        List<BytecodeInstruction> instructions = new ArrayList<>();
        instructions.add(stmt);

        ActualControlFlowGraph actualCFG = mockActualControlFlowGraph(stmt, true);
        Mockito.doReturn(actualCFG).when(stmt).getActualCFG();

        GuidedSearchUtility utility = new GuidedSearchUtility();
        TestCase tc = new DefaultTestCase();
        tc.addStatement(new BooleanPrimitiveStatement(tc, false));
        TestChromosome ch = new TestChromosome();
        ch.setTestCase(tc);

        GenericAccessibleObject geObj = Mockito.mock(GenericAccessibleObject.class);
        Set<GenericAccessibleObject> publicCalls = new HashSet<>();
        publicCalls.add(geObj);

        boolean flag = utility.includesPublicCall(ch,publicCalls);
        assertFalse(flag);
    }

    @Test
    public void testIncludesPublicCall()  {
        BytecodeInstruction stmt = Mockito.mock(BytecodeInstruction.class);
        Mockito.doReturn("java.lang.Integer").when(stmt).getClassName();
        Mockito.doReturn("reverse()").when(stmt).getMethodName();
        Mockito.doReturn(1).when(stmt).getLineNumber();
        Mockito.doReturn(1).when(stmt).getLineNumber();

        List<BytecodeInstruction> instructions = new ArrayList<>();
        instructions.add(stmt);

        ActualControlFlowGraph actualCFG = mockActualControlFlowGraph(stmt, true);
        Mockito.doReturn(actualCFG).when(stmt).getActualCFG();

        GuidedSearchUtility utility = new GuidedSearchUtility();

        Set<GenericAccessibleObject> publicCalls = new HashSet<>();
        publicCalls.add(chromosome.getTestCase().getStatement(1).getAccessibleObject());


        boolean flag = utility.includesPublicCall(chromosome,publicCalls);
        assertTrue(flag);
    }

    @Test
    public void testIncludesPublicCall_emptyTest()  {
        BytecodeInstruction stmt = Mockito.mock(BytecodeInstruction.class);
        Mockito.doReturn("java.lang.Integer").when(stmt).getClassName();
        Mockito.doReturn("reverse()").when(stmt).getMethodName();
        Mockito.doReturn(1).when(stmt).getLineNumber();

        List<BytecodeInstruction> instructions = new ArrayList<>();
        instructions.add(stmt);

        ActualControlFlowGraph actualCFG = mockActualControlFlowGraph(stmt, true);
        Mockito.doReturn(actualCFG).when(stmt).getActualCFG();

        GuidedSearchUtility utility = new GuidedSearchUtility();
        utility.getPublicCalls(trace.getTargetClass(),trace.getTargetLine(), instructions);

        GenericAccessibleObject geObj = Mockito.mock(GenericAccessibleObject.class);
        Set<GenericAccessibleObject> publicCalls = new HashSet<>();
        publicCalls.add(geObj);

        boolean flag = utility.includesPublicCall(new TestChromosome(),publicCalls);
        assertFalse(flag);
    }

    @Test
    public void testSearchForNonPrivateMethods_noPublicCaller(){
        BytecodeInstruction stmt = Mockito.mock(BytecodeInstruction.class);
        Mockito.doReturn("java.lang.Integer").when(stmt).getClassName();
        Mockito.doReturn("reverse()").when(stmt).getMethodName();
        Mockito.doReturn(1).when(stmt).getLineNumber();

        List<BytecodeInstruction> instructions = new ArrayList<>();
        instructions.add(stmt);

        ActualControlFlowGraph actualCFG = mockActualControlFlowGraph(stmt, true);
        Mockito.doReturn(actualCFG).when(stmt).getActualCFG();

        RawControlFlowGraph rawCFG = Mockito.mock(RawControlFlowGraph.class);
        Mockito.doReturn(rawCFG).when(stmt).getRawCFG();
        Mockito.doReturn(new ArrayList<>()).when(rawCFG).determineMethodCalls();

        GuidedSearchUtility utility = new GuidedSearchUtility();
        utility.searchForNonPrivateMethods(instructions, stmt);
    }

    @Test
    public void testSearchForNonPrivateMethods_withPublicCaller(){
        BytecodeInstruction stmt = Mockito.mock(BytecodeInstruction.class);
        Mockito.doReturn("java.lang.Integer").when(stmt).getClassName();
        Mockito.doReturn("reverse()").when(stmt).getMethodName();
        Mockito.doReturn(1).when(stmt).getLineNumber();

        List<BytecodeInstruction> instructions = new ArrayList<>();
        instructions.add(stmt);

        ActualControlFlowGraph actualCFG = mockActualControlFlowGraph(stmt, true);
        Mockito.doReturn(actualCFG).when(stmt).getActualCFG();
        RawControlFlowGraph rawCFG = mockRawControlFlowGraph(stmt, "reverse");
        Mockito.doReturn(rawCFG).when(stmt).getRawCFG();

        GuidedSearchUtility utility = new GuidedSearchUtility();
        utility.searchForNonPrivateMethods(instructions, stmt);
    }

    private ActualControlFlowGraph mockActualControlFlowGraph(BytecodeInstruction stmt, boolean isPublic){
        ActualControlFlowGraph actualCFG = Mockito.mock(ActualControlFlowGraph.class);
        Mockito.doReturn(isPublic).when(actualCFG).isPublicMethod();
        Mockito.doReturn((isPublic)?1:2).when(actualCFG).getMethodAccess();
        return actualCFG;
    }

    private RawControlFlowGraph mockRawControlFlowGraph(BytecodeInstruction stmt, String methodName){
        RawControlFlowGraph rawCFG = Mockito.mock(RawControlFlowGraph.class);
        Mockito.doReturn(rawCFG).when(stmt).getRawCFG();

        List<BytecodeInstruction> callers = new ArrayList<>();
        BytecodeInstruction caller = Mockito.mock(BytecodeInstruction.class);

        ActualControlFlowGraph actualCFG = mockActualControlFlowGraph(caller, true);
        Mockito.doReturn(actualCFG).when(caller).getActualCFG();
        Mockito.doReturn(methodName).when(caller).getCalledMethod();
        callers.add(caller);
        Mockito.doReturn(callers).when(rawCFG).determineMethodCalls();

        return rawCFG;
    }

    @Test
    public void testGeneralCollector() throws FileNotFoundException {
        BytecodeInstruction stmt = Mockito.mock(BytecodeInstruction.class);
        Mockito.doReturn("java.lang.Integer").when(stmt).getClassName();
        Mockito.doReturn("reverse()").when(stmt).getMethodName();
        Mockito.doReturn(20).when(stmt).getLineNumber();

        ActualControlFlowGraph actualCFG = mockActualControlFlowGraph(stmt, true);
        Mockito.doReturn(actualCFG).when(stmt).getActualCFG();

        BytecodeInstructionPool.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT()).registerInstruction(stmt);


        BufferedReader givenStackTrace = new BufferedReader(new StringReader("java.lang.IllegalArgumentException:\n" +
                "\tat java.lang.Integer.bitCount(Integer.java:10)\n" +
                "\tat java.lang.Integer.reverse(Integer.java:20)"));
        StackTrace trace= Mockito.spy(new StackTrace());
        Mockito.doReturn(givenStackTrace).when(trace).readFromFile(anyString());
        trace.setup("", 2);
        GuidedSearchUtility utility = new GuidedSearchUtility();
        utility.collectPublicCalls(trace);
    }

}