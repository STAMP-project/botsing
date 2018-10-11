package eu.stamp.botsing;

import eu.stamp.botsing.fitnessfunction.FitnessFunctionHelper;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FitnessFunctionHelperTest {

    @Test
    public void testIsConstructor(){
        BytecodeInstruction bi = Mockito.mock(BytecodeInstruction.class);
        Mockito.when(bi.getMethodName()).thenReturn("methodA()");
        Mockito.when(bi.getClassName()).thenReturn("eu.stamp.ClassA");
        assertFalse(FitnessFunctionHelper.isConstructor(bi));

        Mockito.when(bi.getMethodName()).thenReturn("ClassA()");
        Mockito.when(bi.getClassName()).thenReturn("eu.stamp.ClassA");
        assertTrue(FitnessFunctionHelper.isConstructor(bi));


        Mockito.when(bi.getMethodName()).thenReturn("methodA()");
        Mockito.when(bi.getClassName()).thenReturn("");
        assertFalse(FitnessFunctionHelper.isConstructor(bi));
    }
}
