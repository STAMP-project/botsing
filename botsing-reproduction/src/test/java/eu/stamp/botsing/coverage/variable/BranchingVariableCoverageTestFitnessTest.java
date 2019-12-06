package eu.stamp.botsing.coverage.variable;

import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testcase.execution.ExecutionTrace;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.objectweb.asm.Type;

import java.util.*;

import static eu.stamp.botsing.coverage.variable.BranchingVariableCoverageTestFitness.getTestFitness;
import static eu.stamp.botsing.coverage.variable.VariableCondition.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BranchingVariableCoverageTestFitnessTest {
    @Spy
    BranchingVariableCoverageTestFitness testFitness;

    @Test
    public void testGetters() {
        String className = "className";
        String methodName = "methodName";
        int lineNumber = 11;
        String variableName = "variableName";
        Type variableType = Type.getType("[I");
        VariableCondition condition = ARRAY_EMPTY;
        BranchingVariableCoverageTestFitness testFitness = getTestFitness(className, methodName, lineNumber,
                variableName, variableType, condition);
        assert testFitness.getTargetClass().equals(className);
        assert testFitness.getTargetMethod().equals(methodName);
        assert testFitness.getLineNumber() == lineNumber;
        assert testFitness.getVariableName().equals(variableName);
        assert testFitness.getVariableType().equals(variableType);
        assert testFitness.getCondition().equals(condition);
    }

    @Test
    public void testToString() {
        String className = "className";
        String methodName = "methodName";
        int lineNumber = 11;
        String variableName = "variableName";
        Type variableType = Type.getType("[I");
        VariableCondition condition = ARRAY_EMPTY;
        BranchingVariableCoverageTestFitness testFitness = getTestFitness(className, methodName, lineNumber,
                variableName, variableType, condition);

        assert testFitness.toString().equals(className + " @L" + lineNumber + " : " + variableName + " : " + condition);
    }

    @Test
    public void testEqualAndCompareTo() {
        String classA = "classA";
        String classB = "classB";
        String method1 = "method1";
        String method2 = "method2";
        int line11 = 11;
        int line12 = 12;
        String variable1 = "variable1";
        String variable2 = "variable2";
        Type type1 = Type.getType("[I");
        Type type2 = Type.getType("[D");

        BranchingVariableCoverageTestFitness testFitness = BranchingVariableCoverageTestFitness.getTestFitness(classA
                , method1, line11, variable1, type1, ARRAY_EMPTY);

        assert !testFitness.equals(null);
        assert !testFitness.equals("Hello World!");

        BranchingVariableCoverageTestFitness refCopy = testFitness;
        assert testFitness.equals(refCopy);

        BranchingVariableCoverageTestFitness testFitnessClassB =
                BranchingVariableCoverageTestFitness.getTestFitness(classB, method1, line11, variable1, type1,
                        ARRAY_EMPTY);
        assert !testFitness.equals(testFitnessClassB);
        assert testFitness.compareTo(testFitnessClassB) < 0;

        BranchingVariableCoverageTestFitness testFitnessLine12 =
                BranchingVariableCoverageTestFitness.getTestFitness(classA, method1, line12, variable1, type1,
                        ARRAY_EMPTY);
        assert !testFitness.equals(testFitnessLine12);
        assert testFitness.compareTo(testFitnessLine12) < 0;

        BranchingVariableCoverageTestFitness testFitnessVariable2 =
                BranchingVariableCoverageTestFitness.getTestFitness(classA, method1, line11, variable2, type1,
                        ARRAY_EMPTY);
        assert !testFitness.equals(testFitnessVariable2);
        assert testFitness.compareTo(testFitnessVariable2) < 0;

        BranchingVariableCoverageTestFitness testFitnessNonEmpty =
                BranchingVariableCoverageTestFitness.getTestFitness(classA, method1, line11, variable1, type1,
                        ARRAY_NONEMPTY);
        assert !testFitness.equals(testFitnessNonEmpty);
        assert testFitness.compareTo(testFitnessNonEmpty) < 0;

        // Technically this situation won't exist. Because same line number enforces it is from the same method.
        BranchingVariableCoverageTestFitness testFitnessMethod2 =
                BranchingVariableCoverageTestFitness.getTestFitness(classA, method2, line11, variable1, type1,
                        ARRAY_EMPTY);
        assert testFitness.equals(testFitnessMethod2);
        assert testFitness.compareTo(testFitnessMethod2) == 0;

        // Technically this situation won't exist. Because same variable name enforces it is of the same type.
        BranchingVariableCoverageTestFitness testFitnessType2 =
                BranchingVariableCoverageTestFitness.getTestFitness(classA, method1, line11, variable1, type2,
                        ARRAY_EMPTY);
        assert testFitness.equals(testFitnessType2);
        assert testFitness.compareTo(testFitnessType2) == 0;
    }

    @Test
    public void testCalculateDistance() {
        testFitness.condition = REF_NULL;
        assert testFitness.calculateDistance(null) == 0;
        assert testFitness.calculateDistance(new Object()) == 1;

        testFitness.condition = REF_NONNULL;
        assert testFitness.calculateDistance(null) == 1;
        assert testFitness.calculateDistance(new Object()) == 0;

        testFitness.condition = NUM_NEGATIVE;
        assert testFitness.calculateDistance(-1) == 0;
        assert testFitness.calculateDistance(0) == 1;
        assert testFitness.calculateDistance(1) == 2;

        testFitness.condition = NUM_ZERO;
        assert testFitness.calculateDistance(-1) == 1;
        assert testFitness.calculateDistance(0) == 0;
        assert testFitness.calculateDistance(1) == 1;

        testFitness.condition = NUM_POSITIVE;
        assert testFitness.calculateDistance(-1) == 2;
        assert testFitness.calculateDistance(0) == 1;
        assert testFitness.calculateDistance(1) == 0;

        testFitness.condition = CHAR_ALPHA;
        assert testFitness.calculateDistance((char) ('B' - 2)) == 1;
        assert testFitness.calculateDistance((char) ('y' + 2)) == 1;
        assert testFitness.calculateDistance((char) ('Y' + 2)) == 1;
        assert testFitness.calculateDistance('Y') == 0;

        testFitness.condition = CHAR_DIGIT;
        assert testFitness.calculateDistance((char) ('1' - 2)) == 1;
        assert testFitness.calculateDistance((char) ('8' + 2)) == 1;
        assert testFitness.calculateDistance('8') == 0;

        testFitness.condition = CHAR_OTHER;
        assert testFitness.calculateDistance('1') == 2;
        assert testFitness.calculateDistance('B') == 2;
        assert testFitness.calculateDistance('b') == 2;
        assert testFitness.calculateDistance((char) ('b' - 2)) == 0;

        testFitness.condition = BOOL_TRUE;
        assert testFitness.calculateDistance(true) == 0;
        assert testFitness.calculateDistance(false) == 1;

        testFitness.condition = BOOL_FALSE;
        assert testFitness.calculateDistance(true) == 1;
        assert testFitness.calculateDistance(false) == 0;

        int[] emptyArray = {};
        int[] arrayOfSizeTwo = {1, 2};
        testFitness.condition = ARRAY_EMPTY;
        assert testFitness.calculateDistance(emptyArray) == 0;
        assert testFitness.calculateDistance(arrayOfSizeTwo) == 2;

        testFitness.condition = ARRAY_NONEMPTY;
        assert testFitness.calculateDistance(emptyArray) == 1;
        assert testFitness.calculateDistance(arrayOfSizeTwo) == 0;

        String emptyString = "";
        String stringOfSizeFive = "Hello";
        testFitness.condition = STRING_EMPTY;
        assert testFitness.calculateDistance(emptyString) == 0;
        assert testFitness.calculateDistance(stringOfSizeFive) == 5;

        testFitness.condition = STRING_NONEMPTY;
        assert testFitness.calculateDistance(emptyString) == 1;
        assert testFitness.calculateDistance(stringOfSizeFive) == 0;

        List<Object> emptyList = new ArrayList<>();
        List<Object> listOfSizeTwo = Arrays.asList(new Object(), new Object());
        testFitness.condition = LIST_EMPTY;
        assert testFitness.calculateDistance(emptyList) == 0;
        assert testFitness.calculateDistance(listOfSizeTwo) == 2;

        testFitness.condition = LIST_NONEMPTY;
        assert testFitness.calculateDistance(emptyList) == 1;
        assert testFitness.calculateDistance(listOfSizeTwo) == 0;

        Set<Object> emptySet = new HashSet<>();
        Set<Object> setOfSizeTwo = new HashSet<>(listOfSizeTwo);
        testFitness.condition = SET_EMPTY;
        assert testFitness.calculateDistance(emptySet) == 0;
        assert testFitness.calculateDistance(setOfSizeTwo) == 2;

        testFitness.condition = SET_NONEMPTY;
        assert testFitness.calculateDistance(emptySet) == 1;
        assert testFitness.calculateDistance(setOfSizeTwo) == 0;

        Map<Object, Object> emptyMap = new HashMap<>();
        Map<Object, Object> mapOfSizeTwo = new HashMap<>();
        mapOfSizeTwo.put(1, "Hello");
        mapOfSizeTwo.put(2, "World");
        testFitness.condition = MAP_EMPTY;
        assert testFitness.calculateDistance(emptyMap) == 0;
        assert testFitness.calculateDistance(mapOfSizeTwo) == 2;

        testFitness.condition = MAP_NONEMPTY;
        assert testFitness.calculateDistance(emptyMap) == 1;
        assert testFitness.calculateDistance(mapOfSizeTwo) == 0;
    }

    @Test
    public void testGetFitness_emptyTrace() {
        TestChromosome individual = spy(TestChromosome.class);
        ExecutionResult result = mock(ExecutionResult.class);
        ExecutionTrace trace = mock(ExecutionTrace.class);
        when(result.getTrace()).thenReturn(trace);

        assertThat(testFitness.getFitness(individual, result)).isEqualTo(1);
        assertThat(individual.getFitness(testFitness)).isEqualTo(1);
        assertThat(individual.getTestCase().getCoveredGoals()).isEmpty();
    }

    @Test
    public void testGetFitness_shouldUpdateIndividualWhenCovered() {
        String className = "classA";
        int lineNumber = 11;
        String variableName = "Integer";
        Integer variable = 11;

        testFitness.className = className;
        testFitness.lineNumber = lineNumber;
        testFitness.variableName = variableName;
        testFitness.variableType = Type.getType(variable.getClass());
        testFitness.condition = NUM_POSITIVE;

        TestChromosome individual = spy(TestChromosome.class);
        ExecutionResult result = mock(ExecutionResult.class);
        ExecutionTrace trace = spy(ExecutionTrace.class);
        when(result.getTrace()).thenReturn(trace);

        Map<Integer, Map<String, Object>> branchingVariables = new HashMap<>();
        branchingVariables.put(lineNumber, new HashMap<>());
        branchingVariables.get(lineNumber).put(variableName, variable);
        when(trace.getBranchingVariables(anyString())).thenReturn(branchingVariables);

        assertThat(testFitness.getFitness(individual, result)).isEqualTo(0);
        assertThat(individual.getFitness(testFitness)).isEqualTo(0);
        assertThat(individual.getTestCase().getCoveredGoals()).containsOnly(testFitness);
    }

    @Test
    public void testGetFitness_GoalNotCovered() {
        String className = "classA";
        int lineNumber = 11;
        String variableName = "Integer";
        Integer variable = 0;

        testFitness.className = className;
        testFitness.lineNumber = lineNumber;
        testFitness.variableName = variableName;
        testFitness.variableType = Type.getType(variable.getClass());
        testFitness.condition = NUM_POSITIVE;

        TestChromosome individual = spy(TestChromosome.class);
        ExecutionResult result = mock(ExecutionResult.class);
        ExecutionTrace trace = spy(ExecutionTrace.class);
        when(result.getTrace()).thenReturn(trace);

        Map<Integer, Map<String, Object>> branchingVariables = new HashMap<>();
        branchingVariables.put(lineNumber, new HashMap<>());
        branchingVariables.get(lineNumber).put(variableName, variable);
        when(trace.getBranchingVariables(anyString())).thenReturn(branchingVariables);

        assertThat(testFitness.getFitness(individual, result)).isEqualTo(1);
        assertThat(individual.getFitness(testFitness)).isEqualTo(1);
        assertThat(individual.getTestCase().getCoveredGoals()).isEmpty();
    }
}