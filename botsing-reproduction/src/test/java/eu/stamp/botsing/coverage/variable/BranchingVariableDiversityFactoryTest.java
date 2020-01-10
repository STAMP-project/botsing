package eu.stamp.botsing.coverage.variable;

import eu.stamp.botsing.coverage.CoverageUtility;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static eu.stamp.botsing.coverage.variable.BranchingVariableDiversityObjective.getTestFitness;
import static eu.stamp.botsing.coverage.variable.DiversityObjective.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.objectweb.asm.Opcodes.*;

@RunWith(MockitoJUnitRunner.class)
public class BranchingVariableDiversityFactoryTest {
    @Spy
    private CoverageUtility utility;

    @Mock
    List<LocalVariableNode> variables;

    @InjectMocks
    BranchingVariableDiversityFactory factory = new BranchingVariableDiversityFactory();

    @Test
    public void testEmptyExecutables() {
        Mockito.when(utility.getStackTraceExecutables()).thenReturn(new ArrayList<>());
        List<BranchingVariableDiversityObjective> goals = factory.getCoverageGoals();
        assert (goals.isEmpty());
    }

    @Test
    public void testDetectGoals_compareTwoInts() {
        LineNumberNode lineNumberNode = mock(LineNumberNode.class);
        lineNumberNode.line = 11;
        BytecodeInstruction lineNumber = mock(BytecodeInstruction.class);
        when(lineNumber.getASMNode()).thenReturn(lineNumberNode);

        AbstractInsnNode constNode = mock(AbstractInsnNode.class);
        when(constNode.getOpcode()).thenReturn(LDC);
        BytecodeInstruction constInstruction = mock(BytecodeInstruction.class);
        when(constInstruction.getASMNode()).thenReturn(constNode);

        VarInsnNode varInsnNode = mock(VarInsnNode.class);
        when(varInsnNode.getOpcode()).thenReturn(ILOAD);
        varInsnNode.var = 0;
        BytecodeInstruction varInstruction = mock(BytecodeInstruction.class);
        when(varInstruction.getASMNode()).thenReturn(varInsnNode);

        JumpInsnNode compareNode = mock(JumpInsnNode.class);
        when(compareNode.getOpcode()).thenReturn(IF_ICMPGT);
        BytecodeInstruction compareInstruction = mock(BytecodeInstruction.class);
        when(compareInstruction.getASMNode()).thenReturn(compareNode);

        LocalVariableNode variableNode = mock(LocalVariableNode.class);
        variableNode.name = "variable";
        variableNode.desc = "I";
        when(variables.get(0)).thenReturn(variableNode);

        List<BytecodeInstruction> instructions = new ArrayList<>();
        instructions.add(lineNumber);
        instructions.add(constInstruction);
        instructions.add(varInstruction);
        instructions.add(compareInstruction);

        Set<BranchingVariableDiversityObjective> goals = factory.detectGoals("classA", "method1", instructions,
                variables);

        assertThat(goals).size().isEqualTo(3);
    }

    @Test
    public void testDetectGoals_compareOneIntAgainstZero() {
        LineNumberNode lineNumberNode = mock(LineNumberNode.class);
        lineNumberNode.line = 11;
        BytecodeInstruction lineNumber = mock(BytecodeInstruction.class);
        when(lineNumber.getASMNode()).thenReturn(lineNumberNode);

        VarInsnNode varInsnNode = mock(VarInsnNode.class);
        when(varInsnNode.getOpcode()).thenReturn(ILOAD);
        varInsnNode.var = 0;
        BytecodeInstruction varInstruction = mock(BytecodeInstruction.class);
        when(varInstruction.getASMNode()).thenReturn(varInsnNode);

        JumpInsnNode compareNode = mock(JumpInsnNode.class);
        when(compareNode.getOpcode()).thenReturn(IFEQ);
        BytecodeInstruction compareInstruction = mock(BytecodeInstruction.class);
        when(compareInstruction.getASMNode()).thenReturn(compareNode);

        LocalVariableNode variableNode = mock(LocalVariableNode.class);
        variableNode.name = "variable";
        variableNode.desc = "I";
        when(variables.get(0)).thenReturn(variableNode);

        List<BytecodeInstruction> instructions = new ArrayList<>();
        instructions.add(lineNumber);
        instructions.add(varInstruction);
        instructions.add(compareInstruction);

        Set<BranchingVariableDiversityObjective> goals = factory.detectGoals("classA", "method1", instructions,
                variables);

        assertThat(goals).size().isEqualTo(3);
    }

    @Test
    public void testDetectVariables() {
        AbstractInsnNode constNode = mock(AbstractInsnNode.class);
        when(constNode.getOpcode()).thenReturn(LDC);
        BytecodeInstruction constInstruction = mock(BytecodeInstruction.class);
        when(constInstruction.getASMNode()).thenReturn(constNode);

        VarInsnNode varInsnNode = mock(VarInsnNode.class);
        when(varInsnNode.getOpcode()).thenReturn(ILOAD);
        varInsnNode.var = 0;
        BytecodeInstruction varInstruction = mock(BytecodeInstruction.class);
        when(varInstruction.getASMNode()).thenReturn(varInsnNode);

        List<BytecodeInstruction> instructions = new ArrayList<>();
        instructions.add(constInstruction);
        instructions.add(varInstruction);

        LocalVariableNode variableNode = mock(LocalVariableNode.class);
        variableNode.name = "variable";
        variableNode.desc = "I";
        when(variables.get(0)).thenReturn(variableNode);

        Set<BranchingVariableDiversityObjective> goals = factory.detectVariables("classA", "method1", 11,
                instructions, variables, 1, 2);

        assertThat(goals).size().isEqualTo(3);
    }

    @Test
    public void testCreateGoals_booleanPrimitive() {
        // Given a primitive boolean variable
        String className = "classA";
        String methodName = "method1";
        int lineNumber = 11;
        LocalVariableNode variable = mock(LocalVariableNode.class);
        variable.name = "booleanPrimitive";
        variable.desc = "Z";

        Set<BranchingVariableDiversityObjective> goals = factory.createGoals(className, methodName, lineNumber,
                variable);

        // Expect 2 goals:
        Set<BranchingVariableDiversityObjective> expected = new HashSet<>();
        expected.add(getTestFitness(className, methodName, lineNumber, variable.name, Type.getType(variable.desc),
                BOOL_FALSE));
        expected.add(getTestFitness(className, methodName, lineNumber, variable.name, Type.getType(variable.desc),
                BOOL_TRUE));

        assertThat(goals).containsOnlyElementsOf(expected);
    }

    @Test
    public void testCreateGoals_BooleanObject() {
        // Given a boolean object
        String className = "classA";
        String methodName = "method1";
        int lineNumber = 11;
        LocalVariableNode variable = mock(LocalVariableNode.class);
        variable.name = "BooleanObject";
        variable.desc = "Ljava/lang/Boolean;";

        Set<BranchingVariableDiversityObjective> goals = factory.createGoals(className, methodName, lineNumber,
                variable);

        // Expect 3 goals
        Set<BranchingVariableDiversityObjective> expected = new HashSet<>();
        expected.add(getTestFitness(className, methodName, lineNumber, variable.name, Type.getType(variable.desc),
                REF_NULL));
        expected.add(getTestFitness(className, methodName, lineNumber, variable.name, Type.getType(variable.desc),
                BOOL_FALSE));
        expected.add(getTestFitness(className, methodName, lineNumber, variable.name, Type.getType(variable.desc),
                BOOL_TRUE));

        assertThat(goals).containsOnlyElementsOf(expected);
    }

    @Test
    public void testCreateGoals_charPrimitive() {
        // Given a primitive char variable
        String className = "classA";
        String methodName = "method1";
        int lineNumber = 11;
        LocalVariableNode variable = mock(LocalVariableNode.class);
        variable.name = "charPrimitive";
        variable.desc = "C";

        Set<BranchingVariableDiversityObjective> goals = factory.createGoals(className, methodName, lineNumber,
                variable);

        // Expect 3 goals:
        Set<BranchingVariableDiversityObjective> expected = new HashSet<>();
        expected.add(getTestFitness(className, methodName, lineNumber, variable.name, Type.getType(variable.desc),
                CHAR_ALPHA));
        expected.add(getTestFitness(className, methodName, lineNumber, variable.name, Type.getType(variable.desc),
                CHAR_DIGIT));
        expected.add(getTestFitness(className, methodName, lineNumber, variable.name, Type.getType(variable.desc),
                CHAR_OTHER));

        assertThat(goals).containsOnlyElementsOf(expected);
    }

    @Test
    public void testCreateGoals_CharacterObject() {
        // Given a boolean object
        String className = "classA";
        String methodName = "method1";
        int lineNumber = 11;
        LocalVariableNode variable = mock(LocalVariableNode.class);
        variable.name = "CharacterObject";
        variable.desc = "Ljava/lang/Character;";

        Set<BranchingVariableDiversityObjective> goals = factory.createGoals(className, methodName, lineNumber,
                variable);

        // Expect 4 goals
        Set<BranchingVariableDiversityObjective> expected = new HashSet<>();
        expected.add(getTestFitness(className, methodName, lineNumber, variable.name, Type.getType(variable.desc),
                REF_NULL));
        expected.add(getTestFitness(className, methodName, lineNumber, variable.name, Type.getType(variable.desc),
                CHAR_ALPHA));
        expected.add(getTestFitness(className, methodName, lineNumber, variable.name, Type.getType(variable.desc),
                CHAR_DIGIT));
        expected.add(getTestFitness(className, methodName, lineNumber, variable.name, Type.getType(variable.desc),
                CHAR_OTHER));

        assertThat(goals).containsOnlyElementsOf(expected);
    }

    @Test
    public void testCreateGoals_numericPrimitive() {
        // Given a primitive char variable
        String className = "classA";
        String methodName = "method1";
        int lineNumber = 11;
        LocalVariableNode variable = mock(LocalVariableNode.class);
        variable.name = "numericPrimitive";
        String[] descriptions = {"B", "S", "I", "F", "J", "D"};
        for (String desc : descriptions) {
            variable.desc = desc;

            Set<BranchingVariableDiversityObjective> goals = factory.createGoals(className, methodName, lineNumber,
                    variable);

            // Expect 3 goals:
            Set<BranchingVariableDiversityObjective> expected = new HashSet<>();
            expected.add(getTestFitness(className, methodName, lineNumber, variable.name, Type.getType(variable.desc)
                    , NUM_NEGATIVE));
            expected.add(getTestFitness(className, methodName, lineNumber, variable.name, Type.getType(variable.desc)
                    , NUM_ZERO));
            expected.add(getTestFitness(className, methodName, lineNumber, variable.name, Type.getType(variable.desc)
                    , NUM_POSITIVE));

            assertThat(goals).containsOnlyElementsOf(expected);
        }
    }

    @Test
    public void testCreateGoals_numericObject() {
        // Given a primitive char variable
        String className = "classA";
        String methodName = "method1";
        int lineNumber = 11;
        LocalVariableNode variable = mock(LocalVariableNode.class);
        variable.name = "numericPrimitive";
        String[] descriptions = {"Ljava/lang/Byte;", "Ljava/lang/Short;", "Ljava/lang/Integer;", "Ljava/lang/Float;",
                "Ljava/lang/Long;", "Ljava/lang/Double;"};
        for (String desc : descriptions) {
            variable.desc = desc;

            Set<BranchingVariableDiversityObjective> goals = factory.createGoals(className, methodName, lineNumber,
                    variable);

            // Expect 4 goals:
            Set<BranchingVariableDiversityObjective> expected = new HashSet<>();
            expected.add(getTestFitness(className, methodName, lineNumber, variable.name, Type.getType(variable.desc)
                    , REF_NULL));
            expected.add(getTestFitness(className, methodName, lineNumber, variable.name, Type.getType(variable.desc)
                    , NUM_NEGATIVE));
            expected.add(getTestFitness(className, methodName, lineNumber, variable.name, Type.getType(variable.desc)
                    , NUM_ZERO));
            expected.add(getTestFitness(className, methodName, lineNumber, variable.name, Type.getType(variable.desc)
                    , NUM_POSITIVE));

            assertThat(goals).containsOnlyElementsOf(expected);
        }
    }

    @Test
    public void testCreateGoals_array() {
        // Given a array reference
        String className = "classA";
        String methodName = "method1";
        int lineNumber = 11;
        LocalVariableNode variable = mock(LocalVariableNode.class);
        variable.name = "arrayRef";
        variable.desc = "[I";

        Set<BranchingVariableDiversityObjective> goals = factory.createGoals(className, methodName, lineNumber,
                variable);

        // Expect 3 goals
        Set<BranchingVariableDiversityObjective> expected = new HashSet<>();
        expected.add(getTestFitness(className, methodName, lineNumber, variable.name, Type.getType(variable.desc),
                REF_NULL));
        expected.add(getTestFitness(className, methodName, lineNumber, variable.name, Type.getType(variable.desc),
                ARRAY_EMPTY));
        expected.add(getTestFitness(className, methodName, lineNumber, variable.name, Type.getType(variable.desc),
                ARRAY_NONEMPTY));

        assertThat(goals).containsOnlyElementsOf(expected);
    }

    @Test
    public void testCreateGoals_StringObject() {
        // Given a String reference
        String className = "classA";
        String methodName = "method1";
        int lineNumber = 11;
        LocalVariableNode variable = mock(LocalVariableNode.class);
        variable.name = "StringRef";
        variable.desc = "Ljava/lang/String;";

        Set<BranchingVariableDiversityObjective> goals = factory.createGoals(className, methodName, lineNumber,
                variable);

        // Expect 3 goals
        Set<BranchingVariableDiversityObjective> expected = new HashSet<>();
        expected.add(getTestFitness(className, methodName, lineNumber, variable.name, Type.getType(variable.desc),
                REF_NULL));
        expected.add(getTestFitness(className, methodName, lineNumber, variable.name, Type.getType(variable.desc),
                STRING_EMPTY));
        expected.add(getTestFitness(className, methodName, lineNumber, variable.name, Type.getType(variable.desc),
                STRING_NONEMPTY));

        assertThat(goals).containsOnlyElementsOf(expected);
    }

    @Test
    public void testCreateGoals_ListObject() {
        // Given a String reference
        String className = "classA";
        String methodName = "method1";
        int lineNumber = 11;
        LocalVariableNode variable = mock(LocalVariableNode.class);
        variable.name = "ListRef";
        variable.desc = "Ljava/util/ArrayList;";

        Set<BranchingVariableDiversityObjective> goals = factory.createGoals(className, methodName, lineNumber,
                variable);

        // Expect 3 goals
        Set<BranchingVariableDiversityObjective> expected = new HashSet<>();
        expected.add(getTestFitness(className, methodName, lineNumber, variable.name, Type.getType(variable.desc),
                REF_NULL));
        expected.add(getTestFitness(className, methodName, lineNumber, variable.name, Type.getType(variable.desc),
                LIST_EMPTY));
        expected.add(getTestFitness(className, methodName, lineNumber, variable.name, Type.getType(variable.desc),
                LIST_NONEMPTY));

        assertThat(goals).containsOnlyElementsOf(expected);
    }

    @Test
    public void testCreateGoals_SetObject() {
        // Given a String reference
        String className = "classA";
        String methodName = "method1";
        int lineNumber = 11;
        LocalVariableNode variable = mock(LocalVariableNode.class);
        variable.name = "SetRef";
        variable.desc = "Ljava/util/HashSet;";

        Set<BranchingVariableDiversityObjective> goals = factory.createGoals(className, methodName, lineNumber,
                variable);

        // Expect 3 goals
        Set<BranchingVariableDiversityObjective> expected = new HashSet<>();
        expected.add(getTestFitness(className, methodName, lineNumber, variable.name, Type.getType(variable.desc),
                REF_NULL));
        expected.add(getTestFitness(className, methodName, lineNumber, variable.name, Type.getType(variable.desc),
                SET_EMPTY));
        expected.add(getTestFitness(className, methodName, lineNumber, variable.name, Type.getType(variable.desc),
                SET_NONEMPTY));

        assertThat(goals).containsOnlyElementsOf(expected);
    }

    @Test
    public void testCreateGoals_MapObject() {
        // Given a String reference
        String className = "classA";
        String methodName = "method1";
        int lineNumber = 11;
        LocalVariableNode variable = mock(LocalVariableNode.class);
        variable.name = "MapRef";
        variable.desc = "Ljava/util/HashMap;";

        Set<BranchingVariableDiversityObjective> goals = factory.createGoals(className, methodName, lineNumber,
                variable);

        // Expect 3 goals
        Set<BranchingVariableDiversityObjective> expected = new HashSet<>();
        expected.add(getTestFitness(className, methodName, lineNumber, variable.name, Type.getType(variable.desc),
                REF_NULL));
        expected.add(getTestFitness(className, methodName, lineNumber, variable.name, Type.getType(variable.desc),
                MAP_EMPTY));
        expected.add(getTestFitness(className, methodName, lineNumber, variable.name, Type.getType(variable.desc),
                MAP_NONEMPTY));

        assertThat(goals).containsOnlyElementsOf(expected);
    }

    @Test
    public void testCreateGoals_Object() {
        // Given a String reference
        String className = "classA";
        String methodName = "method1";
        int lineNumber = 11;
        LocalVariableNode variable = mock(LocalVariableNode.class);
        variable.name = "ObjectRef";
        variable.desc = "Ljava/lang/Object;";

        Set<BranchingVariableDiversityObjective> goals = factory.createGoals(className, methodName, lineNumber,
                variable);

        // Expect 2 goals
        Set<BranchingVariableDiversityObjective> expected = new HashSet<>();
        expected.add(getTestFitness(className, methodName, lineNumber, variable.name, Type.getType(variable.desc),
                REF_NULL));
        expected.add(getTestFitness(className, methodName, lineNumber, variable.name, Type.getType(variable.desc),
                REF_NONNULL));

        assertThat(goals).containsOnlyElementsOf(expected);
    }

    @Test
    public void shouldLogRegisteredVariables() {
        assert (!factory.isRegistered("classA", 12, "variable"));

        // The some variable on different lines will result in different goals.
        factory.registerVariable("classA", 12, "variable");

        assert (!factory.isRegistered("classA", 11, "variable"));
        assert (factory.isRegistered("classA", 12, "variable"));
    }
}