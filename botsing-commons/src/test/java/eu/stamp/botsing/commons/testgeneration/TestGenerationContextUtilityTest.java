package eu.stamp.botsing.commons.testgeneration;

import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.BytecodeInstructionPool;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class TestGenerationContextUtilityTest {

    BytecodeInstructionPool bytecodePool = BytecodeInstructionPool.getInstance( BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());

    String className = "eu.stamp.SomeClass";
    String methodName = "eu.stamp.SomeMethod";
    int lineNumber = 1;

    @Test
    public void test_getTestGenerationContextClassLoader(){
        ClassLoader classLoader_NoIntegration = TestGenerationContextUtility.getTestGenerationContextClassLoader(false);
        Assert.assertEquals("org.evosuite.instrumentation.InstrumentingClassLoader", classLoader_NoIntegration.getClass().getName());
        ClassLoader classLoader_Integration = TestGenerationContextUtility.getTestGenerationContextClassLoader(true);
        Assert.assertEquals("eu.stamp.botsing.commons.instrumentation.InstrumentingClassLoader", classLoader_Integration.getClass().getName());
    }


    @Test
    public void test_derivingMethodFromBytecode(){

        BytecodeInstruction inst1 = Mockito.mock(BytecodeInstruction.class);

        Mockito.when(inst1.getClassName()).thenReturn(className);
        Mockito.when(inst1.getMethodName()).thenReturn(methodName);
        Mockito.when(inst1.getLineNumber()).thenReturn(lineNumber);

        bytecodePool.registerInstruction(inst1);


        String returnedMethodName = TestGenerationContextUtility.derivingMethodFromBytecode(true, className,lineNumber);
        Assert.assertEquals(methodName, returnedMethodName);
    }


    @Test
    public void test_wrongLineNumber_derivingMethodFromBytecode(){
        bytecodePool.clear(className);

        BytecodeInstruction inst1 = Mockito.mock(BytecodeInstruction.class);

        Mockito.when(inst1.getClassName()).thenReturn(className);
        Mockito.when(inst1.getMethodName()).thenReturn(methodName);
        Mockito.when(inst1.getLineNumber()).thenReturn(2);

        bytecodePool.registerInstruction(inst1);


        String returnedMethodName = TestGenerationContextUtility.derivingMethodFromBytecode(true, className,lineNumber);
        Assert.assertEquals(null, returnedMethodName);
    }


    @Test
    public void test_nullHandling_derivingMethodFromBytecode(){


        bytecodePool.clear(className);

        String returnedMethodName = TestGenerationContextUtility.derivingMethodFromBytecode(true, className,lineNumber);
        Assert.assertEquals(null, returnedMethodName);
    }
}
