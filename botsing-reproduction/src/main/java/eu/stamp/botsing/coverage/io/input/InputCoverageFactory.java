package eu.stamp.botsing.coverage.io.input;

import eu.stamp.botsing.coverage.CoverageUtility;
import org.apache.commons.lang3.ClassUtils;
import org.evosuite.assertion.Inspector;
import org.evosuite.assertion.InspectorManager;
import org.evosuite.coverage.io.input.InputCoverageTestFitness;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

import static org.evosuite.coverage.io.IOCoverageConstants.*;

public class InputCoverageFactory extends org.evosuite.coverage.io.input.InputCoverageFactory {
    private static final Logger LOG = LoggerFactory.getLogger(InputCoverageFactory.class);

    @Resource
    protected CoverageUtility utility = new CoverageUtility();

    @Override
    public List<InputCoverageTestFitness> getCoverageGoals() {
        List<InputCoverageTestFitness> goals = new ArrayList<>();
        List<Method> stackTraceMethods = utility.getStackTraceMethods();
        List<Constructor> stackTraceConstructors = utility.getStackTraceConstructors();

        for (Constructor constructor : stackTraceConstructors) {
            String className = constructor.getDeclaringClass().getName();
            String methodName = "<init>" + Type.getConstructorDescriptor(constructor);
            Class<?>[] argumentClasses = constructor.getParameterTypes();
            LOG.info("Adding input goals for constructor {}.{}", className, methodName);
            detectGoals(className, methodName, argumentClasses, goals);
        }

        for (Method method : stackTraceMethods) {
            String className = method.getDeclaringClass().getName();
            String methodName = method.getName() + Type.getMethodDescriptor(method);
            Class<?>[] argumentClasses = method.getParameterTypes();
            LOG.info("Adding input goals for method {}.{}", className, methodName);
            detectGoals(className, methodName, argumentClasses, goals);
        }

        return goals;
    }

    protected void detectGoals(String className, String methodName, Class<?>[] argumentClasses,
                               List<InputCoverageTestFitness> goals) {
        for (int i = 0; i < argumentClasses.length; i++) {
            Type argType = Type.getType(argumentClasses[i]);

            int typeSort = argType.getSort();
            if (typeSort == Type.OBJECT) {
                Class<?> typeClass = argumentClasses[i];
                if (ClassUtils.isPrimitiveWrapper(typeClass)) {
                    typeSort = Type.getType(ClassUtils.wrapperToPrimitive(typeClass)).getSort();
                    goals.add(createGoal(className, methodName, i, argType, REF_NULL));
                }
            }

            switch (typeSort) {
                case Type.BOOLEAN:
                    goals.add(createGoal(className, methodName, i, argType, BOOL_TRUE));
                    goals.add(createGoal(className, methodName, i, argType, BOOL_FALSE));
                    break;
                case Type.CHAR:
                    goals.add(createGoal(className, methodName, i, argType, CHAR_ALPHA));
                    goals.add(createGoal(className, methodName, i, argType, CHAR_DIGIT));
                    goals.add(createGoal(className, methodName, i, argType, CHAR_OTHER));
                    break;
                case Type.BYTE:
                case Type.SHORT:
                case Type.INT:
                case Type.FLOAT:
                case Type.LONG:
                case Type.DOUBLE:
                    goals.add(createGoal(className, methodName, i, argType, NUM_NEGATIVE));
                    goals.add(createGoal(className, methodName, i, argType, NUM_ZERO));
                    goals.add(createGoal(className, methodName, i, argType, NUM_POSITIVE));
                    break;
                case Type.ARRAY:
                    goals.add(createGoal(className, methodName, i, argType, REF_NULL));
                    goals.add(createGoal(className, methodName, i, argType, ARRAY_EMPTY));
                    goals.add(createGoal(className, methodName, i, argType, ARRAY_NONEMPTY));
                    break;
                case Type.OBJECT:
                    goals.add(createGoal(className, methodName, i, argType, REF_NULL));
                    if (argType.getClassName().equals("java.lang.String")) {
                        goals.add(createGoal(className, methodName, i, argType, STRING_EMPTY));
                        goals.add(createGoal(className, methodName, i, argType, STRING_NONEMPTY));
                    } else if (List.class.isAssignableFrom(argumentClasses[i])) {
                        goals.add(createGoal(className, methodName, i, argType, LIST_EMPTY));
                        goals.add(createGoal(className, methodName, i, argType, LIST_NONEMPTY));
                    } else if (Set.class.isAssignableFrom(argumentClasses[i])) {
                        goals.add(createGoal(className, methodName, i, argType, SET_EMPTY));
                        goals.add(createGoal(className, methodName, i, argType, SET_NONEMPTY));
                    } else if (Map.class.isAssignableFrom(argumentClasses[i])) {
                        goals.add(createGoal(className, methodName, i, argType, MAP_EMPTY));
                        goals.add(createGoal(className, methodName, i, argType, MAP_NONEMPTY));
                        // TODO: Collection.class?
                    } else {
                        boolean observerGoalsAdded = false;
                        Class<?> paramClazz = argumentClasses[i];
                        for (Inspector inspector : InspectorManager.getInstance().getInspectors(paramClazz)) {
                            String insp = inspector.getMethodCall() + Type.getMethodDescriptor(inspector.getMethod());
                            Type t = Type.getReturnType(inspector.getMethod());
                            if (t.getSort() == Type.BOOLEAN) {
                                goals.add(createGoal(className, methodName, i, argType, REF_NONNULL + ":" + argType.getClassName() + ":" + insp + ":" + BOOL_TRUE));
                                goals.add(createGoal(className, methodName, i, argType, REF_NONNULL + ":" + argType.getClassName() + ":" + insp + ":" + BOOL_FALSE));
                                observerGoalsAdded = true;
                            } else if (Arrays.asList(new Integer[]{Type.BYTE, Type.SHORT, Type.INT, Type.FLOAT, Type.LONG, Type.DOUBLE}).contains(t.getSort())) {
                                goals.add(createGoal(className, methodName, i, argType, REF_NONNULL + ":" + argType.getClassName() + ":" + insp + ":" + NUM_NEGATIVE));
                                goals.add(createGoal(className, methodName, i, argType, REF_NONNULL + ":" + argType.getClassName() + ":" + insp + ":" + NUM_ZERO));
                                goals.add(createGoal(className, methodName, i, argType, REF_NONNULL + ":" + argType.getClassName() + ":" + insp + ":" + NUM_POSITIVE));
                                observerGoalsAdded = true;
                            }
                        }
                        if (!observerGoalsAdded) {
                            goals.add(createGoal(className, methodName, i, argType, REF_NONNULL));
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
