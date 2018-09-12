package eu.stamp.botsing.fitnessfunction.testcase.factories;

import org.evosuite.graphs.cfg.BytecodeInstruction;

public class FitnessFunctionHelper {

    public static boolean isConstructor(BytecodeInstruction targetInstruction){
        String methodName = targetInstruction.getMethodName();
        methodName = methodName.substring(0, methodName.indexOf('('));
        String classPath = targetInstruction.getClassName();
        System.out.println(methodName+" "+classPath);
        int lastOccurrence = classPath.lastIndexOf(".");
        if (lastOccurrence == -1){
            return false;
        }
        String className = classPath.substring(lastOccurrence+1);
        if(className.equals(methodName)){
            return true;
        }

        return false;

    }
}
