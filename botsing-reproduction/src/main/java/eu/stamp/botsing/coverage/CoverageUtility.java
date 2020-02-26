package eu.stamp.botsing.coverage;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.StackTrace;
import eu.stamp.botsing.commons.testgeneration.TestGenerationContextUtility;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.BytecodeInstructionPool;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CoverageUtility {
    private Set<Method> interestingMethods = new HashSet<>();
    private Set<Constructor> interestingConstructors = new HashSet<>();

    public CoverageUtility() {
        int numberOfStackTraces = CrashProperties.getInstance().getCrashesSize();
        for (int crashIndex = 0; crashIndex < numberOfStackTraces; crashIndex++) {
            StackTrace crash = CrashProperties.getInstance().getStackTrace(crashIndex);
            for (StackTraceElement element : crash.getFrames()) {
                String className = element.getClassName();
                int lineNumber = element.getLineNumber();
                String methodName =
                        TestGenerationContextUtility.derivingMethodFromBytecode(CrashProperties.integrationTesting,
                                className, lineNumber);
                ClassLoader contextClassLoader =
                        TestGenerationContextUtility.getTestGenerationContextClassLoader(CrashProperties.integrationTesting);
                BytecodeInstruction bcInst = BytecodeInstructionPool.getInstance(contextClassLoader)
                        .getFirstInstructionAtLineNumber(className, methodName, lineNumber);
                try {
                    Class<?> clazz = Class.forName(className, true, contextClassLoader);
                    if (isConstructor(bcInst)) {
                        interestingConstructors.add(getConstructor(clazz, methodName));
                    } else {
                        interestingMethods.add(getMethod(clazz, methodName));
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean isConstructor(BytecodeInstruction instruction) {
        return instruction.getMethodName().startsWith("<init>");
    }

    private static Method getMethod(Class<?> clazz, String methodName) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (methodName.equals(method.getName() + Type.getMethodDescriptor(method))) {
                return method;
            }
        }
        throw new IllegalStateException("Method not found");
    }

    private static Constructor getConstructor(Class<?> clazz, String methodName) {
        for (Constructor constructor : clazz.getDeclaredConstructors()) {
            if (methodName.contains(Type.getConstructorDescriptor(constructor))) {
                return constructor;
            }
        }
        throw new IllegalStateException("Constructor does not found!");
    }

    public List<Method> getStackTraceMethods() {
        return new ArrayList<>(interestingMethods);
    }

    public List<Constructor> getStackTraceConstructors() {
        return new ArrayList<>(interestingConstructors);
    }

    public List<Executable> getStackTraceExecutables() {
        List<Executable> list = new ArrayList<>();
        list.addAll(interestingMethods);
        list.addAll(interestingConstructors);
        return list;
    }
}
