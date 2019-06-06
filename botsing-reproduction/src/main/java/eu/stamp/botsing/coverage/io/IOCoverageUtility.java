package eu.stamp.botsing.coverage.io;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.StackTrace;
import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import eu.stamp.botsing.commons.testgeneration.TestGenerationContextUtility;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.BytecodeInstructionPool;
import org.evosuite.setup.TestClusterUtils;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IOCoverageUtility {

    private Set<Method> interestingMethods = new HashSet<>();
    private Set<Constructor> interestingConstructors = new HashSet<>();

    public IOCoverageUtility(){
        int numberOfStackTraces = CrashProperties.getInstance().getCrashesSize();
        for (int crashIndex=0; crashIndex<numberOfStackTraces;crashIndex++){
            StackTrace crash  = CrashProperties.getInstance().getStackTrace(crashIndex);
            for(StackTraceElement element: crash.getFrames()){
                String className = element.getClassName();
                int lineNumber = element.getLineNumber();
                String methodName = TestGenerationContextUtility.derivingMethodFromBytecode(CrashProperties.integrationTesting,className, lineNumber);
                BytecodeInstruction bcInst = BytecodeInstructionPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).getFirstInstructionAtLineNumber(className,methodName,lineNumber);
                try {
                    Class<?> clazz = Class.forName(className, true, BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
                    if(isConstructor(bcInst)){
                        interestingConstructors.add(this.getConstructor(clazz,methodName));
                    }else{
                        interestingMethods.add(this.getMethod(clazz,methodName, element.getMethodName()));
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Method getMethod(Class<?> clazz, String bcMethodName, String methodName) {
        for (Method method : clazz.getDeclaredMethods()) {
            if(bcMethodName.equals(methodName+Type.getMethodDescriptor(method))){
                return method;
            }
        }
        throw new IllegalStateException("Method does not found!");
    }

    private boolean isConstructor(BytecodeInstruction bcInst) {
        if(bcInst.getMethodName().startsWith("<init>")){
            return true;
        }

        return false;
    }

    private Constructor getConstructor(Class<?> clazz, String methodName) {

        Set<Constructor<?>> constructors = TestClusterUtils.getConstructors(clazz);

        for(Constructor constructor: constructors){
            if(methodName.contains(Type.getConstructorDescriptor(constructor))){
                return constructor;
            }
        }
        throw new IllegalStateException("Constructor does not found!");
    }

    public List<Method> getStackTraceMethods(){
        List<Method> methodsList = new ArrayList<>();
        methodsList.addAll(this.interestingMethods);
        return methodsList;
    }


    public List<Constructor> getStackTraceConstructors(){
        List<Constructor> constList = new ArrayList<>();
        constList.addAll(this.interestingConstructors);
        return constList;
    }


    public Type[] getConstructorArgumentTypes(Constructor constructor) {
        Class<?>[] classes = constructor.getParameterTypes();
        Type[] types = new Type[classes.length];

        for(int i = classes.length - 1; i >= 0; --i) {
            types[i] = Type.getType(classes[i]);
        }

        return types;
    }
}
