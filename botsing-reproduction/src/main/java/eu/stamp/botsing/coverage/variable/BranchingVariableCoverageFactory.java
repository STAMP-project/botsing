package eu.stamp.botsing.coverage.variable;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.commons.testgeneration.TestGenerationContextUtility;
import eu.stamp.botsing.coverage.CoverageUtility;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.BytecodeInstructionPool;
import org.evosuite.testsuite.AbstractFitnessFactory;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import javax.annotation.Resource;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.*;

import static eu.stamp.botsing.coverage.variable.BranchingVariableCoverageTestFitness.getTestFitness;
import static eu.stamp.botsing.coverage.variable.VariableCondition.*;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;
import static org.objectweb.asm.Type.DOUBLE;
import static org.objectweb.asm.Type.FLOAT;
import static org.objectweb.asm.Type.LONG;

public class BranchingVariableCoverageFactory extends AbstractFitnessFactory<BranchingVariableCoverageTestFitness> {
    private Map<String, Map<Integer, Set<String>>> loggedVariables = new HashMap<>();

    @Resource
    protected CoverageUtility utility = new CoverageUtility();

    @Override
    public List<BranchingVariableCoverageTestFitness> getCoverageGoals() {
        List<BranchingVariableCoverageTestFitness> goals = new ArrayList<>();
        List<Executable> executables = utility.getStackTraceExecutables();
        BytecodeInstructionPool instructionPool =
                BytecodeInstructionPool.getInstance(TestGenerationContextUtility.getTestGenerationContextClassLoader(CrashProperties.integrationTesting));
        for (Executable executable : executables) {
            String className = executable.getDeclaringClass().getName();
            String methodName = executable instanceof Constructor ?
                    "<init>" + Type.getConstructorDescriptor((Constructor<?>) executable) : executable
                    .getName() + Type.getMethodDescriptor((Method) executable);
            MethodNode methodNode = instructionPool.getMethodNode(className, methodName);
            List<LocalVariableNode> localVariables = methodNode.localVariables;
            localVariables.sort(Comparator.comparingInt(o -> o.index));
            List<BytecodeInstruction> instructions = instructionPool.getInstructionsIn(className, methodName);
            goals.addAll(detectGoals(className, methodName, instructions, localVariables));
        }
        return goals;
    }

    /**
     * Given the instruction list of a method in a class, and its related local variable bytecode nodes, return a {@link
     * Set} of {@link org.evosuite.coverage.branch.BranchCoverageTestFitness} as goals.
     *
     * @param className      The Name of the class, separated by dots.
     * @param methodName     The full name of the method, with descriptors.
     * @param instructions   The {@link List} of {@link BytecodeInstruction}s of the method without any
     *                       instrumentation.
     * @param localVariables The {@link List} of {@link LocalVariableNode}s of the method, sorted according to their
     *                       {@link LocalVariableNode#index indexes}.
     */
    Set<BranchingVariableCoverageTestFitness> detectGoals(String className, String methodName,
                                                          List<BytecodeInstruction> instructions,
                                                          List<LocalVariableNode> localVariables) {
        Set<BranchingVariableCoverageTestFitness> goals = new HashSet<>();
        int currentLine = 0;
        for (int i = 0; i < instructions.size(); i++) {
            AbstractInsnNode instruction = instructions.get(i).getASMNode();
            if (instruction instanceof LineNumberNode) {
                currentLine = ((LineNumberNode) instruction).line;
            } else {
                int opcode = instruction.getOpcode();
                if (opcode == DCMPL || opcode == DCMPG || opcode == FCMPL || opcode == FCMPG || opcode == LCMP || opcode >= IF_ICMPEQ && opcode <= IF_ICMPLE || opcode == IF_ACMPEQ || opcode == IF_ACMPNE) {
                    goals.addAll(detectVariables(className, methodName, currentLine, instructions, localVariables, i,
                            2));
                } else if (opcode >= IFEQ && opcode <= IFLE || opcode == IFNULL || opcode == IFNONNULL) {
                    goals.addAll(detectVariables(className, methodName, currentLine, instructions, localVariables, i,
                            1));
                }
            }
        }
        return goals;
    }

    /**
     * Looking at a specific instruction, which performs a comparision for branching condition, of a method, retrieve a
     * {@link Set} of {@link org.evosuite.coverage.branch.BranchCoverageTestFitness} as goals.
     *
     * @param className      The Name of the class, separated by dots.
     * @param methodName     The full name of the method, with descriptors.
     * @param instructions   The {@link List} of {@link BytecodeInstruction}s of the method without any
     *                       instrumentation.
     * @param localVariables The {@link List} of {@link LocalVariableNode}s of the method, sorted according to their
     *                       {@link LocalVariableNode#index indexes}.
     * @param lineNumber     The line number of the instruction in the source file.
     * @param index          The index of the branching instruction in the instruction list.
     * @param numOfVariables The number of variables compared with the instruction.
     */
    Set<BranchingVariableCoverageTestFitness> detectVariables(String className, String methodName, int lineNumber,
                                                              List<BytecodeInstruction> instructions,
                                                              List<LocalVariableNode> localVariables, int index,
                                                              int numOfVariables) {
        Set<BranchingVariableCoverageTestFitness> goals = new HashSet<>();
        int count = 0;
        while (count < numOfVariables) {
            AbstractInsnNode current = instructions.get(index--).getASMNode();
            switch (current.getOpcode()) {
                // region The variable is a local variable, we log it into the ExecutionTracer
                case ILOAD:
                case LLOAD:
                case FLOAD:
                case DLOAD:
                case ALOAD:
                    LocalVariableNode variable = localVariables.get(((VarInsnNode) current).var);
                    if (!isRegistered(className, lineNumber, variable.name)) {
                        goals.addAll(createGoals(className, methodName, lineNumber, variable));
                        registerVariable(className, lineNumber, variable.name);
                    }
                    // endregion
                    // region The variable is a field of the object or a static field of the class
                case GETSTATIC:
                case GETFIELD:
                    // todo For now we only increase the counter, but maybe they should be treated differently as
                    // they may be related to crashes
                    // endregion
                    // region The variable is a return value from a method call
                case INVOKEVIRTUAL:
                case INVOKESPECIAL:
                case INVOKESTATIC:
                case INVOKEINTERFACE:
                case INVOKEDYNAMIC:
                    // todo For now we only increase the counter, but maybe they should be treated differently as
                    // they may be related to crashes
                    // todo For now we haven't considered the arguments of the method call. They should be excluded
                    // as well.
                    // endregion
                case NEW: // todo New object maybe should be logged as well?
                    // region The variable is a constant, we don't care about it, just increase the counter.
                case ACONST_NULL:
                case ICONST_M1:
                case ICONST_0:
                case ICONST_1:
                case ICONST_2:
                case ICONST_3:
                case ICONST_4:
                case ICONST_5:
                case LCONST_0:
                case LCONST_1:
                case FCONST_0:
                case FCONST_1:
                case FCONST_2:
                case DCONST_0:
                case DCONST_1:
                case BIPUSH:
                case SIPUSH:
                case LDC:
                    // endregion
                    // region The variable is a result of a previous comparision, increase the counter and ignore it.
                case LCMP:
                case FCMPL:
                case FCMPG:
                case DCMPL:
                case DCMPG:
                    // endregion
                    count++;
                    // region We don't care about all the other instructions.
                default:
                    break;
                // endregion
            }
        }
        return goals;
    }

    Set<BranchingVariableCoverageTestFitness> createGoals(String className, String methodName, int lineNumber,
                                                          LocalVariableNode variable) {
        Set<BranchingVariableCoverageTestFitness> goals = new HashSet<>();
        String variableName = variable.name;
        String variableDesc = variable.desc;
        Type variableType = Type.getType(variableDesc);
        int variableSort = variableType.getSort();

        if (variableSort == OBJECT) {
            goals.add(getTestFitness(className, methodName, lineNumber, variableName, variableType, REF_NULL));
            if (variableType.equals(Type.getType(String.class))) {
                goals.add(getTestFitness(className, methodName, lineNumber, variableName, variableType, STRING_EMPTY));
                goals.add(getTestFitness(className, methodName, lineNumber, variableName, variableType,
                        STRING_NONEMPTY));
            } else if (variableType.equals(Type.getType(Boolean.class))) {
                variableSort = BOOLEAN;
            } else if (variableType.equals(Type.getType(Character.class))) {
                variableSort = CHAR;
            } else if (variableType.equals(Type.getType(Byte.class))) {
                variableSort = BYTE;
            } else if (variableType.equals(Type.getType(Short.class))) {
                variableSort = SHORT;
            } else if (variableType.equals(Type.getType(Integer.class))) {
                variableSort = INT;
            } else if (variableType.equals(Type.getType(Float.class))) {
                variableSort = FLOAT;
            } else if (variableType.equals(Type.getType(Long.class))) {
                variableSort = LONG;
            } else if (variableType.equals(Type.getType(Double.class))) {
                variableSort = DOUBLE;
            } else {
                try {
                    String dotClassName = variableDesc.substring(1, variableDesc.length() - 1).replace('/', '.');
                    if (List.class.isAssignableFrom(Class.forName(dotClassName))) {
                        goals.add(getTestFitness(className, methodName, lineNumber, variableName, variableType,
                                LIST_EMPTY));
                        goals.add(getTestFitness(className, methodName, lineNumber, variableName, variableType,
                                LIST_NONEMPTY));
                    } else if (Set.class.isAssignableFrom(Class.forName(dotClassName))) {
                        goals.add(getTestFitness(className, methodName, lineNumber, variableName, variableType,
                                SET_EMPTY));
                        goals.add(getTestFitness(className, methodName, lineNumber, variableName, variableType,
                                SET_NONEMPTY));
                    } else if (Map.class.isAssignableFrom(Class.forName(dotClassName))) {
                        goals.add(getTestFitness(className, methodName, lineNumber, variableName, variableType,
                                MAP_EMPTY));
                        goals.add(getTestFitness(className, methodName, lineNumber, variableName, variableType,
                                MAP_NONEMPTY));
                    } else {
                        goals.add(getTestFitness(className, methodName, lineNumber, variableName, variableType,
                                REF_NONNULL));
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        if (variableSort == ARRAY) {
            goals.add(getTestFitness(className, methodName, lineNumber, variableName, variableType, REF_NULL));
            goals.add(getTestFitness(className, methodName, lineNumber, variableName, variableType, ARRAY_EMPTY));
            goals.add(getTestFitness(className, methodName, lineNumber, variableName, variableType, ARRAY_NONEMPTY));
        } else if (variableSort >= BYTE && variableSort <= DOUBLE) {
            goals.add(getTestFitness(className, methodName, lineNumber, variableName, variableType, NUM_NEGATIVE));
            goals.add(getTestFitness(className, methodName, lineNumber, variableName, variableType, NUM_ZERO));
            goals.add(getTestFitness(className, methodName, lineNumber, variableName, variableType, NUM_POSITIVE));
        } else if (variableSort == BOOLEAN) {
            goals.add(getTestFitness(className, methodName, lineNumber, variableName, variableType, BOOL_TRUE));
            goals.add(getTestFitness(className, methodName, lineNumber, variableName, variableType, BOOL_FALSE));
        } else if (variableSort == CHAR) {
            goals.add(getTestFitness(className, methodName, lineNumber, variableName, variableType, CHAR_ALPHA));
            goals.add(getTestFitness(className, methodName, lineNumber, variableName, variableType, CHAR_DIGIT));
            goals.add(getTestFitness(className, methodName, lineNumber, variableName, variableType, CHAR_OTHER));
        }
        return goals;
    }

    void registerVariable(String className, int lineNumber, String variableName) {
        if (!loggedVariables.containsKey(className)) {
            loggedVariables.put(className, new HashMap<>());
        }
        if (!loggedVariables.get(className).containsKey(lineNumber)) {
            loggedVariables.get(className).put(lineNumber, new HashSet<>());
        }
        loggedVariables.get(className).get(lineNumber).add(variableName);
    }

    boolean isRegistered(String className, int lineNumber, String variableName) {
        if (loggedVariables.containsKey(className)) {
            if (loggedVariables.get(className).containsKey(lineNumber)) {
                return loggedVariables.get(className).get(lineNumber).contains(variableName);
            }
        }
        return false;
    }
}
