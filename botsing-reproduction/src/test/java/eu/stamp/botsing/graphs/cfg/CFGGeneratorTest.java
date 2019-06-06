package eu.stamp.botsing.graphs.cfg;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.StackTrace;
import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import eu.stamp.botsing.commons.graphs.cfg.BotsingRawControlFlowGraph;
import org.evosuite.graphs.GraphPool;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.RawControlFlowGraph;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.util.*;

public class CFGGeneratorTest {
    CFGGenerator CUT = Mockito.spy(CFGGenerator.class);
    private String selectedmethod = "numberOfLeadingZeros";
    List<Class> instrumentedClasses = new ArrayList<>();
    private GraphTestingUtils testingUtils = new GraphTestingUtils();
    @Before
    public void resetCrashes(){
        CrashProperties.getInstance().clearStackTraceList();
    }

    @Test
    public void testNullInstumentedClassInGenerateInterProceduralCFG() throws FileNotFoundException {
        BufferedReader givenStackTrace = new BufferedReader(new StringReader("java.lang.IllegalArgumentException:\n" +
                "\tat eu.stamp.ClassA.method2(ClassA.java:10)\n" +
                "\tat eu.stamp.ClassB.method1(ClassB.java:20)"));
        StackTrace target = Mockito.spy(new StackTrace());
        Mockito.doReturn(givenStackTrace).when(target).readFromFile(anyString());
        target.setup("", 2);
        CrashProperties.getInstance().setupStackTrace(target);
        try {
            CUT.generateInterProceduralCFG();
        }catch(IllegalArgumentException e){
            assertTrue(e.getMessage().contains("There is no instrumented classes"));
        }

    }

    @Test
    public void testCollectCFGS_EmptyGraphPool() {
        instrumentedClasses.add(Integer.class);
        // test with empty graphPool
        CUT.collectCFGS(instrumentedClasses);
    }




    @Test
    public void testCollectCFGS_AddingCFG(){
        instrumentedClasses.add(Integer.class);
        RawControlFlowGraph realRCFG1 = new RawControlFlowGraph(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT(),Integer.class.getName(),selectedmethod,0);
        RawControlFlowGraph rcfg1 = Mockito.spy(realRCFG1);
        BytecodeInstruction stmnt = testingUtils.mockNewStatement(Integer.class.getName(),selectedmethod,1400);
        Set<BytecodeInstruction> stmntList = new HashSet<>();
        stmntList.add(stmnt);

        GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).registerRawCFG(rcfg1);
        CUT.collectCFGS(instrumentedClasses);
        // Assertions
        assertEquals(1 ,CUT.cfgs.get(Integer.class.getName()).size());
        assertEquals(selectedmethod, CUT.cfgs.get(Integer.class.getName()).get(0).getMethodName());
    }


    @Test
    public void testCollectCFGS_AddFrameCFG()  throws FileNotFoundException{
        instrumentedClasses.add(Integer.class);
        RawControlFlowGraph realRCFG1 = new RawControlFlowGraph(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT(),Integer.class.getName(),selectedmethod,0);
        RawControlFlowGraph rcfg1 = Mockito.spy(realRCFG1);
        BytecodeInstruction stmnt = testingUtils.mockNewStatement(Integer.class.getName(),selectedmethod,1400);
        Set<BytecodeInstruction> stmntList = new HashSet<>();
        stmntList.add(stmnt);

        GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).registerRawCFG(rcfg1);
        CUT.collectCFGS(instrumentedClasses);
        BufferedReader givenStackTrace = new BufferedReader(new StringReader("java.lang.IllegalArgumentException:\n" +
                "\tat java.lang.Integer.numberOfLeadingZeros(Integer.java:1400)"));
        StackTrace target = Mockito.spy(new StackTrace());
        Mockito.doReturn(givenStackTrace).when(target).readFromFile(anyString());
        target.setup("", 1);
        CrashProperties.getInstance().setupStackTrace(target);
        CUT.generateRawGraph(0);
        Mockito.doReturn(stmntList).when(rcfg1).vertexSet();
        CUT.generateRawGraph(0);
        // Assertions
        assertEquals(1,CUT.frameCFGs.size());
        assertEquals(rcfg1,CUT.frameCFGs.get(0).getRcfg());
    }


    @Test
    public void testCollectCFGS_FirstFrameCFGAnalysis() throws FileNotFoundException{
        instrumentedClasses.add(Integer.class);
        RawControlFlowGraph realRCFG1 = new RawControlFlowGraph(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT(),Integer.class.getName(),selectedmethod,0);
        RawControlFlowGraph rcfg1 = Mockito.spy(realRCFG1);
        RawControlFlowGraph realRCFG2 = new RawControlFlowGraph(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT(),Integer.class.getName(),"numberOfTrailingZeros",0);
        RawControlFlowGraph rcfg2 = Mockito.spy(realRCFG2);
        BytecodeInstruction stmnt2 = testingUtils.mockNewStatement(Integer.class.getName(),"numberOfTrailingZeros",1425);
        List<BytecodeInstruction> stmntList2 = new ArrayList<>();
        stmntList2.add(stmnt2);
        BytecodeInstruction stmnt = testingUtils.mockNewStatement(Integer.class.getName(),selectedmethod,1400);
        Set<BytecodeInstruction> stmntList = new HashSet<>();
        stmntList.add(stmnt);
        GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).registerRawCFG(rcfg1);
        GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).registerRawCFG(rcfg2);

        BufferedReader givenStackTrace = new BufferedReader(new StringReader("java.lang.IllegalArgumentException:\n" +
                "\tat java.lang.Integer.numberOfLeadingZeros(Integer.java:1400)\n" +
                "\tat java.lang.Integer.numberOfTrailingZeros(Integer.java:1425)"));
        StackTrace target = Mockito.spy(new StackTrace());
        Mockito.doReturn(givenStackTrace).when(target).readFromFile(anyString());
        target.setup("", 2);
        CrashProperties.getInstance().setupStackTrace(target);
        CUT.collectCFGS(instrumentedClasses);
        Mockito.doReturn(stmntList).when(rcfg1).vertexSet();
        CUT.generateRawGraph(0);
        assertEquals(1,CUT.frameCFGs.size());

    }

    @Test
    public void testCollectCFGS_SecondFrameEstimation() throws FileNotFoundException{
        instrumentedClasses.add(Integer.class);
        RawControlFlowGraph realRCFG1 = new RawControlFlowGraph(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT(),Integer.class.getName(),selectedmethod,0);
        RawControlFlowGraph rcfg1 = Mockito.spy(realRCFG1);
        RawControlFlowGraph realRCFG2 = new RawControlFlowGraph(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT(),Integer.class.getName(),"numberOfTrailingZeros",0);
        RawControlFlowGraph rcfg2 = Mockito.spy(realRCFG2);

        BytecodeInstruction stmnt2 = testingUtils.mockNewStatement(Integer.class.getName(),"numberOfTrailingZeros",1425);
        List<BytecodeInstruction> stmntList2 = new ArrayList<>();
        stmntList2.add(stmnt2);
        BytecodeInstruction stmnt = testingUtils.mockNewStatement(Integer.class.getName(),selectedmethod,1400);
        Mockito.doReturn(selectedmethod).when(stmnt).getCalledMethod();
        Set<BytecodeInstruction> stmntList = new HashSet<>();
        stmntList.add(stmnt);
        Mockito.doReturn(new ArrayList<>(Arrays.asList(stmnt))).when(rcfg2).determineMethodCallsToOwnClass();
        GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).registerRawCFG(rcfg1);
        GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).registerRawCFG(rcfg2);

        BufferedReader givenStackTrace = new BufferedReader(new StringReader("java.lang.IllegalArgumentException:\n" +
                "\tat java.lang.Integer.numberOfLeadingZeros(Integer.java:1400)\n" +
                "\tat java.lang.Integer.numberOfTrailingZeros(Integer.java:1425)"));
        StackTrace target = Mockito.spy(new StackTrace());
        Mockito.doReturn(givenStackTrace).when(target).readFromFile(anyString());
        target.setup("", 2);
        CrashProperties.getInstance().setupStackTrace(target);
        CUT.collectCFGS(instrumentedClasses);
        Mockito.doReturn(stmntList).when(rcfg1).vertexSet();
        CUT.generateRawGraph(0);
        assertEquals(2,CUT.frameCFGs.size());
        assertEquals("numberOfTrailingZeros",CUT.frameCFGs.get(1).getRcfg().getMethodName());
    }

    @Test
    public void testCollectCFGS_SecondFrameCFGAnalysis() throws FileNotFoundException{
        instrumentedClasses.add(Integer.class);
        RawControlFlowGraph realRCFG1 = new RawControlFlowGraph(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT(),Integer.class.getName(),selectedmethod,0);
        RawControlFlowGraph rcfg1 = Mockito.spy(realRCFG1);
        RawControlFlowGraph realRCFG2 = new RawControlFlowGraph(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT(),Integer.class.getName(),"numberOfTrailingZeros",0);
        RawControlFlowGraph rcfg2 = Mockito.spy(realRCFG2);
        BytecodeInstruction stmnt2 = testingUtils.mockNewStatement(Integer.class.getName(),"numberOfTrailingZeros",1425);
        Mockito.doReturn(selectedmethod).when(stmnt2).getCalledMethod();
        List<BytecodeInstruction> stmntList2 = new ArrayList<>();
        stmntList2.add(stmnt2);
        BytecodeInstruction stmnt = testingUtils.mockNewStatement(Integer.class.getName(),selectedmethod,1400);
        Set<BytecodeInstruction> stmntList = new HashSet<>();
        stmntList.add(stmnt);
        GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).registerRawCFG(rcfg1);
        GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).registerRawCFG(rcfg2);

        BufferedReader givenStackTrace = new BufferedReader(new StringReader("java.lang.IllegalArgumentException:\n" +
                "\tat java.lang.Integer.numberOfLeadingZeros(Integer.java:1400)\n" +
                "\tat java.lang.Integer.numberOfTrailingZeros(Integer.java:1425)"));
        StackTrace target = Mockito.spy(new StackTrace());
        Mockito.doReturn(givenStackTrace).when(target).readFromFile(anyString());
        target.setup("", 2);
        CrashProperties.getInstance().setupStackTrace(target);
        CUT.collectCFGS(instrumentedClasses);
        Mockito.doReturn(stmntList).when(rcfg1).vertexSet();
        Mockito.doReturn(stmntList2).when(rcfg2).determineMethodCalls();
        Mockito.doReturn(stmntList).when(rcfg2).determineExitPoints();
        BotsingRawControlFlowGraph actualBotsingRawControlFlowGraph = new BotsingRawControlFlowGraph(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT(),"","",0);
        BotsingRawControlFlowGraph interproceduralGraph = Mockito.spy(actualBotsingRawControlFlowGraph);
        Mockito.doNothing().when(interproceduralGraph).addInterProceduralEdge(any(),any(),anySet());
        Mockito.doReturn(interproceduralGraph).when(CUT).makeBotsingRawControlFlowGraphObject(anyInt());
        CUT.generateRawGraph(0);
        assertEquals(2,CUT.frameCFGs.size());
    }


    @Test
    public void testIsNotInDomain(){
        instrumentedClasses.add(Integer.class);
        RawControlFlowGraph realRCFG1 = new RawControlFlowGraph(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT(),Integer.class.getName(),selectedmethod,0);
        RawControlFlowGraph rcfg1 = Mockito.spy(realRCFG1);
        BytecodeInstruction stmnt = testingUtils.mockNewStatement(Integer.class.getName(),selectedmethod,100);
        BytecodeInstruction stmnt2 = testingUtils.mockNewStatement(Integer.class.getName(),selectedmethod,102);
        rcfg1.addVertex(stmnt);
        rcfg1.addVertex(stmnt2);

        GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).registerRawCFG(rcfg1);
        CUT.collectCFGS(instrumentedClasses);
        assertFalse(CUT.isNotInDomain(Integer.class.getName(),selectedmethod,101));
        assertTrue(CUT.isNotInDomain(Integer.class.getName(),selectedmethod,99));
    }


}
