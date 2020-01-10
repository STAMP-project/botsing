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

import static eu.stamp.botsing.coverage.variable.BranchingVariableDiversityObjective.getTestFitness;
import static eu.stamp.botsing.coverage.variable.DiversityObjective.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BranchingVariableDiversityObjectiveTest {
    @Spy
    BranchingVariableDiversityObjective objective;

    @Test
    public void testGetters() {
        String className = "className";
        String methodName = "methodName";
        int lineNumber = 11;
        String variableName = "variableName";
        Type variableType = Type.getType("[I");
        DiversityObjective condition = ARRAY_EMPTY;
        BranchingVariableDiversityObjective testFitness = getTestFitness(className, methodName, lineNumber,
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
        DiversityObjective condition = ARRAY_EMPTY;
        BranchingVariableDiversityObjective testFitness = getTestFitness(className, methodName, lineNumber,
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

        BranchingVariableDiversityObjective testFitness = BranchingVariableDiversityObjective.getTestFitness(classA,
                method1, line11, variable1, type1, ARRAY_EMPTY);

        assert !testFitness.equals(null);
        assert !testFitness.equals("Hello World!");

        BranchingVariableDiversityObjective refCopy = testFitness;
        assert testFitness.equals(refCopy);

        BranchingVariableDiversityObjective testFitnessClassB =
                BranchingVariableDiversityObjective.getTestFitness(classB, method1, line11, variable1, type1,
                        ARRAY_EMPTY);
        assert !testFitness.equals(testFitnessClassB);
        assert testFitness.compareTo(testFitnessClassB) < 0;

        BranchingVariableDiversityObjective testFitnessLine12 =
                BranchingVariableDiversityObjective.getTestFitness(classA, method1, line12, variable1, type1,
                        ARRAY_EMPTY);
        assert !testFitness.equals(testFitnessLine12);
        assert testFitness.compareTo(testFitnessLine12) < 0;

        BranchingVariableDiversityObjective testFitnessVariable2 =
                BranchingVariableDiversityObjective.getTestFitness(classA, method1, line11, variable2, type1,
                        ARRAY_EMPTY);
        assert !testFitness.equals(testFitnessVariable2);
        assert testFitness.compareTo(testFitnessVariable2) < 0;

        BranchingVariableDiversityObjective testFitnessNonEmpty =
                BranchingVariableDiversityObjective.getTestFitness(classA, method1, line11, variable1, type1,
                        ARRAY_NONEMPTY);
        assert !testFitness.equals(testFitnessNonEmpty);
        assert testFitness.compareTo(testFitnessNonEmpty) < 0;

        // Technically this situation won't exist. Because same line number enforces it is from the same method.
        BranchingVariableDiversityObjective testFitnessMethod2 =
                BranchingVariableDiversityObjective.getTestFitness(classA, method2, line11, variable1, type1,
                        ARRAY_EMPTY);
        assert testFitness.equals(testFitnessMethod2);
        assert testFitness.compareTo(testFitnessMethod2) == 0;

        // Technically this situation won't exist. Because same variable name enforces it is of the same type.
        BranchingVariableDiversityObjective testFitnessType2 =
                BranchingVariableDiversityObjective.getTestFitness(classA, method1, line11, variable1, type2,
                        ARRAY_EMPTY);
        assert testFitness.equals(testFitnessType2);
        assert testFitness.compareTo(testFitnessType2) == 0;
    }

    @Test
    public void testCalculateDistance() {
        objective.condition = REF_NULL;
        assert objective.calculateDistance(null) == 0;
        assert objective.calculateDistance(new Object()) == 1;

        objective.condition = REF_NONNULL;
        assert objective.calculateDistance(null) == 1;
        assert objective.calculateDistance(new Object()) == 0;

        objective.condition = NUM_NEGATIVE;
        assert objective.calculateDistance(-1) == 0;
        assert objective.calculateDistance(0) == 1;
        assert objective.calculateDistance(1) == 2;

        objective.condition = NUM_ZERO;
        assert objective.calculateDistance(-1) == 1;
        assert objective.calculateDistance(0) == 0;
        assert objective.calculateDistance(1) == 1;

        objective.condition = NUM_POSITIVE;
        assert objective.calculateDistance(-1) == 2;
        assert objective.calculateDistance(0) == 1;
        assert objective.calculateDistance(1) == 0;

        objective.condition = CHAR_ALPHA;
        assert objective.calculateDistance((char) ('B' - 2)) == 1;
        assert objective.calculateDistance((char) ('y' + 2)) == 1;
        assert objective.calculateDistance((char) ('Y' + 2)) == 1;
        assert objective.calculateDistance('Y') == 0;

        objective.condition = CHAR_DIGIT;
        assert objective.calculateDistance((char) ('1' - 2)) == 1;
        assert objective.calculateDistance((char) ('8' + 2)) == 1;
        assert objective.calculateDistance('8') == 0;

        objective.condition = CHAR_OTHER;
        assert objective.calculateDistance('1') == 2;
        assert objective.calculateDistance('B') == 2;
        assert objective.calculateDistance('b') == 2;
        assert objective.calculateDistance((char) ('b' - 2)) == 0;

        objective.condition = BOOL_TRUE;
        assert objective.calculateDistance(true) == 0;
        assert objective.calculateDistance(false) == 1;

        objective.condition = BOOL_FALSE;
        assert objective.calculateDistance(true) == 1;
        assert objective.calculateDistance(false) == 0;

        int[] emptyArray = {};
        int[] arrayOfSizeTwo = {1, 2};
        objective.condition = ARRAY_EMPTY;
        assert objective.calculateDistance(emptyArray) == 0;
        assert objective.calculateDistance(arrayOfSizeTwo) == 2;

        objective.condition = ARRAY_NONEMPTY;
        assert objective.calculateDistance(emptyArray) == 1;
        assert objective.calculateDistance(arrayOfSizeTwo) == 0;

        String emptyString = "";
        String stringOfSizeFive = "Hello";
        objective.condition = STRING_EMPTY;
        assert objective.calculateDistance(emptyString) == 0;
        assert objective.calculateDistance(stringOfSizeFive) == 5;

        objective.condition = STRING_NONEMPTY;
        assert objective.calculateDistance(emptyString) == 1;
        assert objective.calculateDistance(stringOfSizeFive) == 0;

        List<Object> emptyList = new ArrayList<>();
        List<Object> listOfSizeTwo = Arrays.asList(new Object(), new Object());
        objective.condition = LIST_EMPTY;
        assert objective.calculateDistance(emptyList) == 0;
        assert objective.calculateDistance(listOfSizeTwo) == 2;

        objective.condition = LIST_NONEMPTY;
        assert objective.calculateDistance(emptyList) == 1;
        assert objective.calculateDistance(listOfSizeTwo) == 0;

        Set<Object> emptySet = new HashSet<>();
        Set<Object> setOfSizeTwo = new HashSet<>(listOfSizeTwo);
        objective.condition = SET_EMPTY;
        assert objective.calculateDistance(emptySet) == 0;
        assert objective.calculateDistance(setOfSizeTwo) == 2;

        objective.condition = SET_NONEMPTY;
        assert objective.calculateDistance(emptySet) == 1;
        assert objective.calculateDistance(setOfSizeTwo) == 0;

        Map<Object, Object> emptyMap = new HashMap<>();
        Map<Object, Object> mapOfSizeTwo = new HashMap<>();
        mapOfSizeTwo.put(1, "Hello");
        mapOfSizeTwo.put(2, "World");
        objective.condition = MAP_EMPTY;
        assert objective.calculateDistance(emptyMap) == 0;
        assert objective.calculateDistance(mapOfSizeTwo) == 2;

        objective.condition = MAP_NONEMPTY;
        assert objective.calculateDistance(emptyMap) == 1;
        assert objective.calculateDistance(mapOfSizeTwo) == 0;
    }

    @Test
    public void testGetFitness_emptyTrace() {
        TestChromosome individual = spy(TestChromosome.class);
        ExecutionResult result = mock(ExecutionResult.class);
        ExecutionTrace trace = mock(ExecutionTrace.class);
        when(result.getTrace()).thenReturn(trace);

        assertThat(objective.getFitness(individual, result)).isEqualTo(1);
        assertThat(individual.getFitness(objective)).isEqualTo(1);
        assertThat(individual.getTestCase().getCoveredGoals()).isEmpty();
    }

    @Test
    public void testGetFitness_shouldUpdateIndividualWhenCovered() {
        String className = "classA";
        int lineNumber = 11;
        String variableName = "Integer";
        Integer variable = 11;

        objective.className = className;
        objective.lineNumber = lineNumber;
        objective.variableName = variableName;
        objective.variableType = Type.getType(variable.getClass());
        objective.condition = NUM_POSITIVE;

        TestChromosome individual = spy(TestChromosome.class);
        ExecutionResult result = mock(ExecutionResult.class);
        ExecutionTrace trace = spy(ExecutionTrace.class);
        when(result.getTrace()).thenReturn(trace);

        Map<Integer, Map<String, Object>> branchingVariables = new HashMap<>();
        branchingVariables.put(lineNumber, new HashMap<>());
        branchingVariables.get(lineNumber).put(variableName, variable);
        when(trace.getBranchingVariables(anyString())).thenReturn(branchingVariables);

        assertThat(objective.getFitness(individual, result)).isEqualTo(0);
        assertThat(individual.getFitness(objective)).isEqualTo(0);
        assertThat(individual.getTestCase().getCoveredGoals()).containsOnly(objective);
    }

    @Test
    public void testGetFitness_GoalNotCovered() {
        String className = "classA";
        int lineNumber = 11;
        String variableName = "Integer";
        Integer variable = 0;

        objective.className = className;
        objective.lineNumber = lineNumber;
        objective.variableName = variableName;
        objective.variableType = Type.getType(variable.getClass());
        objective.condition = NUM_POSITIVE;

        TestChromosome individual = spy(TestChromosome.class);
        ExecutionResult result = mock(ExecutionResult.class);
        ExecutionTrace trace = spy(ExecutionTrace.class);
        when(result.getTrace()).thenReturn(trace);

        Map<Integer, Map<String, Object>> branchingVariables = new HashMap<>();
        branchingVariables.put(lineNumber, new HashMap<>());
        branchingVariables.get(lineNumber).put(variableName, variable);
        when(trace.getBranchingVariables(anyString())).thenReturn(branchingVariables);

        assertThat(objective.getFitness(individual, result)).isEqualTo(1);
        assertThat(individual.getFitness(objective)).isEqualTo(1);
        assertThat(individual.getTestCase().getCoveredGoals()).isEmpty();
    }
}