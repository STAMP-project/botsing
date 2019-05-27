package eu.stamp.botsing.coverage.io.output;

import eu.stamp.botsing.coverage.io.IOCoverageUtility;
import org.apache.commons.lang3.ClassUtils;
import org.evosuite.assertion.Inspector;
import org.evosuite.assertion.InspectorManager;
import org.evosuite.coverage.io.output.OutputCoverageTestFitness;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.evosuite.coverage.io.IOCoverageConstants.*;
import static org.evosuite.coverage.io.IOCoverageConstants.NUM_POSITIVE;
import static org.evosuite.coverage.io.IOCoverageConstants.REF_NONNULL;

public class OutputCoverageFactory extends org.evosuite.coverage.io.output.OutputCoverageFactory {
    private static final Logger LOG = LoggerFactory.getLogger(OutputCoverageFactory.class);
    @Resource
    IOCoverageUtility utility = new IOCoverageUtility();

    @Override
    public List<OutputCoverageTestFitness> getCoverageGoals() {
        List<OutputCoverageTestFitness> goals = new ArrayList<>();
        List<Method> stackTraceMethods = utility.getStackTraceMethods();


        for(Method method: stackTraceMethods){
            detectGoals(method,goals);
        }

        return goals;
    }

    protected void detectGoals(Method method,  List<OutputCoverageTestFitness> goals) {
        String className = method.getDeclaringClass().getName();
        String methodName = method.getName()+Type.getMethodDescriptor(method);
        Type returnType = Type.getReturnType(method);

        LOG.info("Adding output goals for method " + className + "." + methodName);

        int typeSort = returnType.getSort();
        if(typeSort == Type.OBJECT) {
            Class<?> typeClass = method.getReturnType();
            if(ClassUtils.isPrimitiveWrapper(typeClass)) {
                typeSort = Type.getType(ClassUtils.wrapperToPrimitive(typeClass)).getSort();
                goals.add(createGoal(className, methodName, returnType, REF_NULL));
            }
        }

        switch (typeSort) {
            case Type.BOOLEAN:
                goals.add(createGoal(className, methodName, returnType, BOOL_TRUE));
                goals.add(createGoal(className, methodName, returnType, BOOL_FALSE));
                break;
            case Type.CHAR:
                goals.add(createGoal(className, methodName, returnType, CHAR_ALPHA));
                goals.add(createGoal(className, methodName, returnType, CHAR_DIGIT));
                goals.add(createGoal(className, methodName, returnType, CHAR_OTHER));
                break;
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
            case Type.FLOAT:
            case Type.LONG:
            case Type.DOUBLE:
                goals.add(createGoal(className, methodName, returnType, NUM_NEGATIVE));
                goals.add(createGoal(className, methodName, returnType, NUM_ZERO));
                goals.add(createGoal(className, methodName, returnType, NUM_POSITIVE));
                break;
            case Type.ARRAY:
                goals.add(createGoal(className, methodName, returnType, REF_NULL));
                goals.add(createGoal(className, methodName, returnType, ARRAY_EMPTY));
                goals.add(createGoal(className, methodName, returnType, ARRAY_NONEMPTY));
                break;
            case Type.OBJECT:
                goals.add(createGoal(className, methodName, returnType, REF_NULL));
                //goals.add(new OutputCoverageTestFitness(new OutputCoverageGoal(className, methodName, returnType.toString(), REF_NONNULL)));
                if (returnType.getClassName().equals("java.lang.String")) {
                    goals.add(createGoal(className, methodName, returnType, STRING_EMPTY));
                    goals.add(createGoal(className, methodName, returnType, STRING_NONEMPTY));
                    break;
                }
                boolean observerGoalsAdded = false;
                Class<?> returnClazz = method.getReturnType();
                for(Inspector inspector : InspectorManager.getInstance().getInspectors(returnClazz)) {
                    String insp = inspector.getMethodCall() + Type.getMethodDescriptor(inspector.getMethod());
                    Type t = Type.getReturnType(inspector.getMethod());
                    if (t.getSort() == Type.BOOLEAN) {
                        goals.add(createGoal(className, methodName, returnType, REF_NONNULL + ":" + returnType.getClassName() + ":" + insp + ":" + BOOL_TRUE));
                        goals.add(createGoal(className, methodName, returnType, REF_NONNULL + ":" + returnType.getClassName() + ":" + insp + ":" + BOOL_FALSE));
                        observerGoalsAdded = true;
                    } else if (Arrays.asList(new Integer[]{Type.BYTE, Type.SHORT, Type.INT, Type.FLOAT, Type.LONG, Type.DOUBLE}).contains(t.getSort())) {
                        goals.add(createGoal(className, methodName, returnType, REF_NONNULL + ":" + returnType.getClassName() + ":" + insp + ":" + NUM_NEGATIVE));
                        goals.add(createGoal(className, methodName, returnType, REF_NONNULL + ":" + returnType.getClassName() + ":" + insp + ":" + NUM_ZERO));
                        goals.add(createGoal(className, methodName, returnType, REF_NONNULL + ":" + returnType.getClassName() + ":" + insp + ":" + NUM_POSITIVE));
                        observerGoalsAdded = true;
                    }
                }
                if (!observerGoalsAdded){
                    goals.add(createGoal(className, methodName, returnType, REF_NONNULL));
                }
                break;
            default:
                break;
        }
    }

}
