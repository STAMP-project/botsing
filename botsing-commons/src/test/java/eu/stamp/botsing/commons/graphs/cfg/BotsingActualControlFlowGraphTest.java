package eu.stamp.botsing.commons.graphs.cfg;

import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.RawControlFlowGraph;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class BotsingActualControlFlowGraphTest {
    private String className = "java.lang.Integer";
    private GraphTestingUtils testingUtils = new GraphTestingUtils();
    @Test
    public void testSetEntryPoint(){
        RawControlFlowGraph realRCFG = new BotsingRawControlFlowGraph(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT(),"testClass","testMethod",0);
        RawControlFlowGraph rcfg = Mockito.spy(realRCFG);
        try{
            BotsingActualControlFlowGraph actualControlFlowGraph = new BotsingActualControlFlowGraph(rcfg);
        }catch(IllegalArgumentException e){
            assertTrue(e.getMessage().contains("The given entryPoint is null"));
        }

        BytecodeInstruction stmnt1 = testingUtils.mockNewStatement(className,"sum()");
        Mockito.doReturn(stmnt1).when(rcfg).determineEntryPoint();
        Mockito.doReturn(null).when(rcfg).determineExitPoints();
        try{
            BotsingActualControlFlowGraph actualControlFlowGraph = new BotsingActualControlFlowGraph(rcfg);
        }catch(IllegalArgumentException e){
            assertTrue(e.getMessage().contains("The given exitPoints set is null"));
        }


        Set<BytecodeInstruction> exitpoints = new HashSet<>();
        BytecodeInstruction stmnt2 = testingUtils.mockNewStatement(className,"sum()");

        exitpoints.add(stmnt2);
//        exitpoints.add(testingUtils.mockNewStatement(className,"sum()"));
        Mockito.doReturn(exitpoints).when(rcfg).determineExitPoints();

        try{
            BotsingActualControlFlowGraph actualControlFlowGraph = new BotsingActualControlFlowGraph(rcfg);
        }catch(IllegalArgumentException e){
            assertTrue(e.getMessage().contains("unexpected exitPoint byteCode instruction type:"));
        }
        Mockito.doReturn(true).when(stmnt2).canBeExitPoint();
        rcfg.addVertex(stmnt1);
        rcfg.addVertex(stmnt2);

        Mockito.doReturn(null).when(rcfg).determineBranches();
        Mockito.doReturn(null).when(rcfg).determineJoins();

        try{
            BotsingActualControlFlowGraph actualControlFlowGraph = new BotsingActualControlFlowGraph(rcfg);
        }catch(IllegalArgumentException e){
            assertTrue(e.getMessage().contains("The given branches set is null"));
        }


        Mockito.doReturn(exitpoints).when(rcfg).determineBranches();

        try{
            BotsingActualControlFlowGraph actualControlFlowGraph = new BotsingActualControlFlowGraph(rcfg);
        }catch(IllegalArgumentException e){
            assertTrue(e.getMessage().contains("The given joins set is null"));
        }

        Mockito.doReturn(exitpoints).when(rcfg).determineJoins();

        try{
            BotsingActualControlFlowGraph actualControlFlowGraph = new BotsingActualControlFlowGraph(rcfg);
        }catch(Exception e){
        }


    }
}
