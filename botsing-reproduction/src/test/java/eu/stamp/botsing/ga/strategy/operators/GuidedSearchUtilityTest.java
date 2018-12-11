package eu.stamp.botsing.ga.strategy.operators;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.evosuite.Properties;
import org.evosuite.TestGenerationContext;
import org.evosuite.ga.ConstructionFailedException;
import org.evosuite.graphs.cfg.ActualControlFlowGraph;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.RawControlFlowGraph;
import org.evosuite.testcase.DefaultTestCase;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFactory;
import org.evosuite.testcase.statements.ConstructorStatement;
import org.evosuite.testcase.statements.MethodStatement;
import org.evosuite.testcase.statements.Statement;
import org.evosuite.testcase.statements.numeric.BooleanPrimitiveStatement;
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

import eu.stamp.botsing.StackTrace;

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
        GenericClass clazz = new GenericClass(sut);

        DefaultTestCase test = new DefaultTestCase();
        GenericConstructor gc = new GenericConstructor(clazz.getRawClass().getConstructors()[0], clazz);

        TestFactory testFactory = TestFactory.getInstance();
        VariableReference callee = testFactory.addConstructor(test, gc, 0, 0);
        VariableReference intVar = test.addStatement(new IntPrimitiveStatement(test, 10));

        Method m = clazz.getRawClass().getMethod("reverse", new Class<?>[] { int.class});
        GenericMethod method = new GenericMethod(m, sut);
        MethodStatement ms = new MethodStatement(test, method, callee, Arrays.asList(new VariableReference[] {intVar}));
        test.addStatement(ms);

        GenericMethod method2 = new GenericMethod(m, sut);
        MethodStatement ms2 = new MethodStatement(test, method2, callee, Arrays.asList(new VariableReference[] {intVar}));
        test.addStatement(ms2);

        return test;
    }

    private StackTrace initializeStackTrace(){
        StackTrace stackTrace = Mockito.mock(StackTrace.class);
        Mockito.doReturn("java.lang.Integer").when(stackTrace).getTargetClass();
        Mockito.doReturn("reverse").when(stackTrace).getTargetMethod();
        Mockito.doReturn(1).when(stackTrace).getTargetLine();
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

        Set calls = utility.getPublicCalls(trace, instructions);
        assertTrue(calls.contains("reverse"));
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
        Set calls = utility.getPublicCalls(trace, instructions);
        assertFalse(calls.contains("reverse"));
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
        Set calls = utility.getPublicCalls(trace, instructions);
        assertTrue(calls.size()==1);

        Iterator<String> iterator = calls.iterator();
        assertTrue(iterator.next().equalsIgnoreCase("java.lang.Integer"));
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
        Set calls = utility.getPublicCalls(trace, instructions);

        TestCase tc = new DefaultTestCase();
        tc.addStatement(new BooleanPrimitiveStatement(tc, false));
        TestChromosome ch = new TestChromosome();
        ch.setTestCase(tc);

        boolean flag = utility.includesPublicCall(ch);
        assertFalse(flag);
    }

    @Test
    public void testIncludesPublicCall()  {
        BytecodeInstruction stmt = Mockito.mock(BytecodeInstruction.class);
        Mockito.doReturn("java.lang.Integer").when(stmt).getClassName();
        Mockito.doReturn("reverse()").when(stmt).getMethodName();
        Mockito.doReturn(1).when(stmt).getLineNumber();

        List<BytecodeInstruction> instructions = new ArrayList<>();
        instructions.add(stmt);

        ActualControlFlowGraph actualCFG = mockActualControlFlowGraph(stmt, true);
        Mockito.doReturn(actualCFG).when(stmt).getActualCFG();

        GuidedSearchUtility utility = new GuidedSearchUtility();
        Set calls = utility.getPublicCalls(trace, instructions);
        boolean flag = utility.includesPublicCall(chromosome);
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
        Set calls = utility.getPublicCalls(trace, instructions);
        boolean flag = utility.includesPublicCall(new TestChromosome());
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
    public void testIsCall2Constructor(){
        String className = "java.lang.Integer";
        ConstructorStatement statement = null;
        TestCase tc = chromosome.getTestCase();
        for (int i=0; i<tc.size(); i++){
            Statement stmt = tc.getStatement(i);
            if (stmt instanceof ConstructorStatement) {
                statement = (ConstructorStatement) stmt;
            }
        }
        GuidedSearchUtility utility = new GuidedSearchUtility();
        boolean flag = utility.isCall2Constructor("java.lang.Integer", statement);
        assertTrue(flag);
    }

}