package eu.stamp.botsing.model.generation.callsequence;

import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

public class CallSequencePoolManagerTest {

    @Test
    public void testSavePool(){


        BytecodeInstruction bc =  Mockito.mock(BytecodeInstruction.class);
        BytecodeInstruction fakeBC =  Mockito.mock(BytecodeInstruction.class);
        Mockito.when(bc.explain()).thenReturn("bc, mocked for unit testing");
        Mockito.when(bc.getCalledMethodsClass()).thenReturn("Class2");
        Mockito.when(bc.toString()).thenReturn("Class2");
        Mockito.when(bc.isConstructorInvocation()).thenReturn(false);
        Mockito.when(bc.isCallToStaticMethod()).thenReturn(false);
        Mockito.when(bc.getInstructionId()).thenReturn((int) Math.random());
        Mockito.when(bc.getMethodCallDescriptor()).thenReturn("(Ljava/lang/String;)V");
        Mockito.when(bc.getSourceOfMethodInvocationInstruction()).thenReturn(fakeBC);
        Mockito.when(bc.getSourceOfMethodInvocationInstruction().getVariableName()).thenReturn("var0");

        // mock callSequence
        List<MethodCall> callSequence = new ArrayList<MethodCall>();
        callSequence.add(new MethodCall(bc));


        CallSequencesPoolManager.getInstance().addSequence("classA",callSequence);

        CallSequencesPoolManager.getInstance().savePool("");
    }
}
