package eu.stamp.botsing_model_generation.sourcecode;

import eu.stamp.botsing_model_generation.BotsingTestGenerationContext;
import eu.stamp.botsing_model_generation.analysis.sourcecode.StaticAnalyser;
import org.evosuite.graphs.GraphPool;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.RawControlFlowGraph;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StaticAnalyserTest {

    @Test
    public void testAnalyse_bc_is_constructor(){

        // mock current bytecodeInstruction
        BytecodeInstruction bc =  Mockito.mock(BytecodeInstruction.class);
        Mockito.when(bc.explain()).thenReturn("bc, mocked for unit testing");
        Mockito.when(bc.getCalledMethodsClass()).thenReturn("Class2");
        Mockito.when(bc.toString()).thenReturn("Class2");
        Mockito.when(bc.isConstructorInvocation()).thenReturn(true);
        Mockito.when(bc.getInstructionId()).thenReturn((int) Math.random());
        Mockito.when(bc.getMethodCallDescriptor()).thenReturn("(Ljava/lang/String;)V");


        // mock next bytecodeInstruction
        BytecodeInstruction next =  Mockito.mock(BytecodeInstruction.class);
        Mockito.when(next.getInstructionType()).thenReturn("ASTORE");
        Mockito.when(next.getVariableName()).thenReturn("var0");

        // mock callSequence
        List<BytecodeInstruction> callSequence = new ArrayList<BytecodeInstruction>();
        callSequence.add(bc);



        // mock raw cfg
        RawControlFlowGraph rcfg = Mockito.mock(RawControlFlowGraph.class);
        Mockito.when(rcfg.getClassName()).thenReturn("Class1");
        Mockito.when(rcfg.getMethodName()).thenReturn("methodA()");
        Mockito.when(rcfg.determineMethodCalls()).thenReturn(callSequence);
        Mockito.when(rcfg.getInstruction(ArgumentMatchers.anyInt())).thenReturn(next);



        GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).registerRawCFG(rcfg);



        List<String> interestingClasses = new ArrayList<>();
        interestingClasses.add("Class1");
        StaticAnalyser staticAnalyser =  new StaticAnalyser();
        staticAnalyser.analyse(interestingClasses);
    }

    @Test
    public void testAnalyse_bc_is_static_method_call(){

        // mock current bytecodeInstruction
        BytecodeInstruction bc =  Mockito.mock(BytecodeInstruction.class);
        Mockito.when(bc.explain()).thenReturn("bc, mocked for unit testing");
        Mockito.when(bc.getCalledMethodsClass()).thenReturn("Class2");
        Mockito.when(bc.toString()).thenReturn("Class2");
        Mockito.when(bc.isConstructorInvocation()).thenReturn(false);
        Mockito.when(bc.isCallToStaticMethod()).thenReturn(true);
        Mockito.when(bc.getInstructionId()).thenReturn((int) Math.random());
        Mockito.when(bc.getMethodCallDescriptor()).thenReturn("(Ljava/lang/String;)V");


        // mock next bytecodeInstruction
        BytecodeInstruction next =  Mockito.mock(BytecodeInstruction.class);
        Mockito.when(next.getInstructionType()).thenReturn("ALOAD");
        Mockito.when(next.getVariableName()).thenReturn("var0");

        // mock callSequence
        List<BytecodeInstruction> callSequence = new ArrayList<BytecodeInstruction>();
        callSequence.add(bc);



        // mock raw cfg
        RawControlFlowGraph rcfg = Mockito.mock(RawControlFlowGraph.class);
        Mockito.when(rcfg.getClassName()).thenReturn("Class1");
        Mockito.when(rcfg.getMethodName()).thenReturn("methodA()");
        Mockito.when(rcfg.determineMethodCalls()).thenReturn(callSequence);
        Mockito.when(rcfg.getInstruction(ArgumentMatchers.anyInt())).thenReturn(next);



        GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).registerRawCFG(rcfg);



        List<String> interestingClasses = new ArrayList<>();
        interestingClasses.add("Class1");
        StaticAnalyser staticAnalyser =  new StaticAnalyser();
        staticAnalyser.analyse(interestingClasses);
    }


    @Test
    public void testAnalyse_bc_is_regular_method_call(){
        testAnalyse_bc_is_constructor();
        // mock current bytecodeInstruction
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




        // mock next bytecodeInstruction
        BytecodeInstruction next =  Mockito.mock(BytecodeInstruction.class);
        Mockito.when(next.getInstructionType()).thenReturn("ALOAD");
        Mockito.when(next.getVariableName()).thenReturn("var0");

        // mock callSequence
        List<BytecodeInstruction> callSequence = new ArrayList<BytecodeInstruction>();
        callSequence.add(bc);



        // mock raw cfg
        RawControlFlowGraph rcfg = Mockito.mock(RawControlFlowGraph.class);
        Mockito.when(rcfg.getClassName()).thenReturn("Class1");
        Mockito.when(rcfg.getMethodName()).thenReturn("methodA()");
        Mockito.when(rcfg.determineMethodCalls()).thenReturn(callSequence);
        Mockito.when(rcfg.getInstruction(ArgumentMatchers.anyInt())).thenReturn(next);



        GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).registerRawCFG(rcfg);



        List<String> interestingClasses = new ArrayList<>();
        interestingClasses.add("Class1");
        StaticAnalyser staticAnalyser =  new StaticAnalyser();
        staticAnalyser.analyse(interestingClasses);
    }
}
