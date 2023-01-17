package eu.stamp.botsing.commons.graphs.cfg;

import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.RawControlFlowGraph;
import static org.junit.Assert.*;

import org.junit.Test;
import org.evosuite.shaded.org.mockito.Mockito;

import java.util.HashSet;

import java.util.Set;

public class BotsingRawControlFlowGraphTest{

    private String className = "java.lang.Integer";
    private GraphTestingUtils testingUtils = new GraphTestingUtils();
    @Test
    public void testClone() {

        BotsingRawControlFlowGraph rawInterProceduralGraph = new BotsingRawControlFlowGraph(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT(),"testClass","testMethod",0);
        RawControlFlowGraph realRCFG = new BotsingRawControlFlowGraph(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT(),"testClass","testMethod",0);
        RawControlFlowGraph rcfg = Mockito.spy(realRCFG);

        // Add vertices
        BytecodeInstruction stmt1 = testingUtils.mockNewStatement(className,"reverse()");
        BytecodeInstruction stmt2 = testingUtils.mockNewStatement(className,"sum()");
        rcfg.addVertex(stmt1);
        rcfg.addVertex(stmt2);

        // Add edges
        BytecodeInstruction[][] edges = {{stmt1,stmt2},{stmt2,stmt1}};
        testingUtils.addMockedEdge(edges,rcfg);

        rawInterProceduralGraph.clone(rcfg);

        // vertex assertions
        assertEquals(2,rawInterProceduralGraph.vertexSet().size());
        assertTrue(rcfg.vertexSet().equals(rawInterProceduralGraph.vertexSet()));

        // edge assertions
        assertEquals(2,rcfg.edgeSet().size());
        assertTrue(rawInterProceduralGraph.containsEdge(stmt1,stmt2));
        assertTrue(rawInterProceduralGraph.containsEdge(stmt2,stmt1));

    }

    @Test
    public void testAddInterProceduralEdge(){
        BytecodeInstruction cfg1stmt1 = testingUtils.mockNewStatement(className,"reverse()");
        BytecodeInstruction cfg1stmt2 = testingUtils.mockNewStatement(className,"reverse()");
        BytecodeInstruction cfg1stmt3 = testingUtils.mockNewStatement(className,"reverse()");

        BytecodeInstruction cfg2stmt1 = testingUtils.mockNewStatement(className,"sum()");
        BytecodeInstruction cfg2stmt2 = testingUtils.mockNewStatement(className,"sum()");
        BytecodeInstruction cfg2stmt3 = testingUtils.mockNewStatement(className,"sum()");
        BytecodeInstruction cfg2stmt4 = testingUtils.mockNewStatement(className,"sum()");


        // Creating rcfg1
        RawControlFlowGraph realRCFG1 = new BotsingRawControlFlowGraph(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT(),"testClass","testMethod",0);
        RawControlFlowGraph rcfg1 = Mockito.spy(realRCFG1);
        rcfg1.addVertex(cfg1stmt1);
        rcfg1.addVertex(cfg1stmt2);
        rcfg1.addVertex(cfg1stmt3);

        BytecodeInstruction[][] edges = {{cfg1stmt1,cfg1stmt2},{cfg1stmt2,cfg1stmt3}};
        testingUtils.addMockedEdge(edges,rcfg1);

        // Creating rcfg2
        RawControlFlowGraph realRCFG2 = new BotsingRawControlFlowGraph(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT(),"testClass","testMethod",0);
        RawControlFlowGraph rcfg2 = Mockito.spy(realRCFG2);
        rcfg1.addVertex(cfg2stmt1);
        rcfg1.addVertex(cfg2stmt2);
        rcfg1.addVertex(cfg2stmt3);
        rcfg1.addVertex(cfg2stmt4);

        BytecodeInstruction[][] edges2 = {{cfg2stmt1,cfg2stmt2},{cfg2stmt1,cfg2stmt3},{cfg2stmt1,cfg2stmt4}};
        testingUtils.addMockedEdge(edges2,rcfg2);

        BotsingRawControlFlowGraph rawInterProceduralGraph = new BotsingRawControlFlowGraph(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT(),"testClass","testMethod",0);
        rawInterProceduralGraph.clone(rcfg1);
        rawInterProceduralGraph.clone(rcfg2);

        Set<BytecodeInstruction> exitpoints = new HashSet<>();
        exitpoints.add(cfg2stmt2);
        Mockito.doReturn(true).when(cfg2stmt3).isThrow();
        exitpoints.add(cfg2stmt3);
        exitpoints.add(cfg2stmt4);
        rawInterProceduralGraph.addInterProceduralEdge(cfg1stmt2,cfg2stmt1,exitpoints);

        assertEquals(7,rawInterProceduralGraph.edgeCount());
        assertFalse(rawInterProceduralGraph.containsEdge(cfg1stmt2,cfg1stmt3));
        assertTrue(rawInterProceduralGraph.containsEdge(cfg2stmt2,cfg1stmt3));
        assertFalse(rawInterProceduralGraph.containsEdge(cfg2stmt3,cfg1stmt3));
        assertTrue(rawInterProceduralGraph.containsEdge(cfg2stmt4,cfg1stmt3));
    }


    @Test
    public void testAddInterProceduralEdgeWithoutExitPoints(){
        BytecodeInstruction cfg1stmt1 = testingUtils.mockNewStatement(className,"reverse()");
        BytecodeInstruction cfg1stmt2 = testingUtils.mockNewStatement(className,"reverse()");
        BytecodeInstruction cfg1stmt3 = testingUtils.mockNewStatement(className,"reverse()");

        BytecodeInstruction cfg2stmt1 = testingUtils.mockNewStatement(className,"sum()");
        BytecodeInstruction cfg2stmt2 = testingUtils.mockNewStatement(className,"sum()");
        BytecodeInstruction cfg2stmt3 = testingUtils.mockNewStatement(className,"sum()");
        BytecodeInstruction cfg2stmt4 = testingUtils.mockNewStatement(className,"sum()");


        // Creating rcfg1
        RawControlFlowGraph realRCFG1 = new BotsingRawControlFlowGraph(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT(),"testClass","testMethod",0);
        RawControlFlowGraph rcfg1 = Mockito.spy(realRCFG1);
        rcfg1.addVertex(cfg1stmt1);
        rcfg1.addVertex(cfg1stmt2);
        rcfg1.addVertex(cfg1stmt3);

        BytecodeInstruction[][] edges = {{cfg1stmt1,cfg1stmt2},{cfg1stmt2,cfg1stmt3}};
        testingUtils.addMockedEdge(edges,rcfg1);

        // Creating rcfg2
        RawControlFlowGraph realRCFG2 = new BotsingRawControlFlowGraph(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT(),"testClass","testMethod",0);
        RawControlFlowGraph rcfg2 = Mockito.spy(realRCFG2);
        rcfg1.addVertex(cfg2stmt1);
        rcfg1.addVertex(cfg2stmt2);
        rcfg1.addVertex(cfg2stmt3);
        rcfg1.addVertex(cfg2stmt4);

        BytecodeInstruction[][] edges2 = {{cfg2stmt1,cfg2stmt2},{cfg2stmt1,cfg2stmt3},{cfg2stmt1,cfg2stmt4}};
        testingUtils.addMockedEdge(edges2,rcfg2);

        BotsingRawControlFlowGraph rawInterProceduralGraph = new BotsingRawControlFlowGraph(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT(),"testClass","testMethod",0);
        rawInterProceduralGraph.clone(rcfg1);
        rawInterProceduralGraph.clone(rcfg2);

        Set<BytecodeInstruction> exitpoints = new HashSet<>();
        exitpoints.add(cfg2stmt2);
        Mockito.doReturn(true).when(cfg2stmt3).isThrow();
        exitpoints.add(cfg2stmt3);
        exitpoints.add(cfg2stmt4);

        try{
            rawInterProceduralGraph.addInterProceduralEdge(cfg1stmt2,cfg2stmt1,new HashSet<>());
        }catch(IllegalArgumentException e){
            assertTrue(e.getMessage().contains("method sum() does not have any exit point"));
        }
    }


    @Test
    public void testNullBasicBlock(){
        RawControlFlowGraph realRCFG1 = new BotsingRawControlFlowGraph(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT(),"testClass","testMethod",0);
        try{
            realRCFG1.determineBasicBlockFor(null);
        }catch(IllegalArgumentException e){
            assertTrue(e.getMessage().contains("The given bytecode instruction is null"));
        }
    }

    @Test
    public void testBasicBlock(){
        RawControlFlowGraph realRCFG1 = new BotsingRawControlFlowGraph(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT(),"testClass","testMethod",0);
        RawControlFlowGraph rcfg1 = Mockito.spy(realRCFG1);
        BytecodeInstruction mainStmnt = testingUtils.mockNewStatement(className,"reverse()");
        BytecodeInstruction childStmnt = testingUtils.mockNewStatement(className,"reverse()");
        BytecodeInstruction parentStmnt = testingUtils.mockNewStatement(className,"reverse()");
        Mockito.doReturn(1).when(rcfg1).outDegreeOf(mainStmnt);

        Set<BytecodeInstruction> childSet = new HashSet<>();
        childSet.add(childStmnt);
        Mockito.doReturn(childSet).when(rcfg1).getChildren(mainStmnt);

        Mockito.doReturn(1).when(rcfg1).inDegreeOf(mainStmnt);

        Set<BytecodeInstruction> parentSet = new HashSet<>();
        parentSet.add(parentStmnt);
        Mockito.doReturn(parentSet).when(rcfg1).getParents(mainStmnt);

        rcfg1.determineBasicBlockFor(mainStmnt);
    }


}
