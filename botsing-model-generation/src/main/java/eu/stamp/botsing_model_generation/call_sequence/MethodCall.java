package eu.stamp.botsing_model_generation.call_sequence;

import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.testcase.statements.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;


public class MethodCall{
    private String[] params;
    private String methodName;
    private static final Logger LOG = LoggerFactory.getLogger(MethodCall.class);

    public MethodCall(Statement statement){
        if (statement.getAccessibleObject().isMethod())
            methodName = statement.getAccessibleObject().getName();
        else if (statement.getAccessibleObject().isConstructor())
            methodName =  "<init>";

        Type[] types = statement.getAccessibleObject().getGenericParameterTypes();
        params = new String[types.length];
        for (int i=0;i<types.length;i++)
            params[i]=types[i].getTypeName();
    }

    public MethodCall (BytecodeInstruction byteCode){

        methodName = byteCode.getCalledMethodName();

        org.objectweb.asm.Type[] argTypes = org.objectweb.asm.Type.getArgumentTypes(byteCode.getMethodCallDescriptor());
        params = new String[argTypes.length];
        for(int i=0;i<argTypes.length;i++) {
            params[i] = argTypes[i].getClassName();
        }
    }

    public String getMethodName(){
        return methodName;
    }

    public String[] getParams(){
        return params;
    }
}
