package eu.stamp.botsing.commons.coverage.branch;

import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import org.evosuite.coverage.ControlFlowDistance;
import org.evosuite.coverage.branch.Branch;
import org.evosuite.graphs.GraphPool;
import org.evosuite.graphs.cfg.ActualControlFlowGraph;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.ControlDependency;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testcase.execution.MethodCall;
import org.evosuite.testcase.execution.TestCaseExecutor;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.*;

public class InterProceduralControlFlowDistanceCalculatorTest {

    @Test(expected = IllegalArgumentException.class)
    public void testNullResult(){
        ControlFlowDistance cfd = InterProceduralControlFlowDistanceCalculator.getDistance(null,null, true, "ClassA","method1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullClassName(){
        ExecutionResult result = Mockito.mock(ExecutionResult.class);
        ControlFlowDistance cfd = InterProceduralControlFlowDistanceCalculator.getDistance(result,null, true, null,"method1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullmethodName(){
        ExecutionResult result = Mockito.mock(ExecutionResult.class);
        ControlFlowDistance cfd = InterProceduralControlFlowDistanceCalculator.getDistance(result,null, true, "ClassA",null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRootFalseValue(){
        ExecutionResult result = Mockito.mock(ExecutionResult.class);
        ControlFlowDistance cfd = InterProceduralControlFlowDistanceCalculator.getDistance(result,null, false, "ClassA","method1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalBranchClassName(){
        ExecutionResult result = Mockito.mock(ExecutionResult.class);
        Branch branch = Mockito.mock(Branch.class);
        Mockito.doReturn("ClassB").when(branch).getClassName();
        Mockito.doReturn("method1").when(branch).getMethodName();
        ControlFlowDistance cfd = InterProceduralControlFlowDistanceCalculator.getDistance(result,branch, false, "ClassA","method1");
    }


    @Test(expected = IllegalArgumentException.class)
    public void testIllegalBranchMethodName(){
        ExecutionResult result = Mockito.mock(ExecutionResult.class);
        Branch branch = Mockito.mock(Branch.class);
        Mockito.doReturn("ClassA").when(branch).getClassName();
        Mockito.doReturn("method2").when(branch).getMethodName();
        ControlFlowDistance cfd = InterProceduralControlFlowDistanceCalculator.getDistance(result,branch, false, "ClassA","method1");
    }


    @Test
    public void testTimeOutResult(){

        // Mock test case
        TestCase tc = Mockito.mock(TestCase.class);
        Mockito.doReturn(5).when(tc).size();
        // Mock result
        ExecutionResult result = Mockito.spy(new ExecutionResult(tc));
        Mockito.doReturn(true).when(result).isThereAnExceptionAtPosition(5);
        Mockito.doReturn(new TestCaseExecutor.TimeoutExceeded()).when(result).getExceptionThrownAtPosition(5);

        // Mock branch
        Branch branch = Mockito.mock(Branch.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.doReturn("ClassA").when(branch).getClassName();
        Mockito.doReturn("method1").when(branch).getMethodName();
        Mockito.when(branch.getInstruction().getActualCFG().getDiameter()).thenReturn(10);

        ControlFlowDistance cfd = InterProceduralControlFlowDistanceCalculator.getDistance(result,branch, false, "ClassA","method1");

        Assert.assertEquals(cfd.approachLevel,12);
        assert(cfd.branchDistance == 0);
    }


    @Test
    public void testRootDistanceCalculator(){
        ExecutionResult result = Mockito.mock(ExecutionResult.class,Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(result.getTrace().getCoveredMethods().contains(ArgumentMatchers.anyString())).thenReturn(true);
        ControlFlowDistance cfd = InterProceduralControlFlowDistanceCalculator.getDistance(result,null, true, "ClassA","method1");
        Assert.assertEquals(cfd.approachLevel,0);
        assert(cfd.branchDistance == 0);
    }

    @Test
    public void testCoveredByTrueBranches(){
        ExecutionResult result = Mockito.mock(ExecutionResult.class,Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(result.getTrace().getCoveredTrueBranches().contains(ArgumentMatchers.anyInt())).thenReturn(true);

        Branch branch = Mockito.mock(Branch.class);
        Mockito.doReturn("ClassA").when(branch).getClassName();
        Mockito.doReturn("method1").when(branch).getMethodName();

        ControlFlowDistance cfd = InterProceduralControlFlowDistanceCalculator.getDistance(result,branch, true, "ClassA","method1");
        Assert.assertEquals(cfd.approachLevel,0);
        assert(cfd.branchDistance == 0);
    }



    @Test
    public void testCoveredByFalseBranches(){
        ExecutionResult result = Mockito.mock(ExecutionResult.class,Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(result.getTrace().getCoveredFalseBranches().contains(ArgumentMatchers.anyInt())).thenReturn(true);

        Branch branch = Mockito.mock(Branch.class);
        Mockito.doReturn("ClassA").when(branch).getClassName();
        Mockito.doReturn("method1").when(branch).getMethodName();

        ControlFlowDistance cfd = InterProceduralControlFlowDistanceCalculator.getDistance(result,branch, false, "ClassA","method1");
        Assert.assertEquals(cfd.approachLevel,0);
        assert(cfd.branchDistance == 0);
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalNullBranchInNonRootMethod(){
        InterProceduralControlFlowDistanceCalculator.getNonRootDistance(null,null,true);
    }


    @Test
    public void testNonRootCalculator_NullCall(){
        List<MethodCall> calls = new ArrayList<>();
        calls.add(null);

        ExecutionResult result = Mockito.mock(ExecutionResult.class,Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(result.getTrace().getCoveredTrueBranches().contains(ArgumentMatchers.anyInt())).thenReturn(false);
        Mockito.when(result.getTrace().getMethodCalls()).thenReturn(calls);

        Branch branch = Mockito.mock(Branch.class,Mockito.RETURNS_DEEP_STUBS);
        Mockito.doReturn("ClassA").when(branch).getClassName();
        Mockito.doReturn("method1").when(branch).getMethodName();
        Mockito.when(branch.getInstruction().getActualCFG().getDiameter()).thenReturn(10);

        // Set cfg
        ActualControlFlowGraph cfg = Mockito.mock(ActualControlFlowGraph.class);
        Mockito.doReturn("IntegrationTestingGraph").when(cfg).getClassName();
        Mockito.doReturn("methodsIntegration").when(cfg).getMethodName();
        Mockito.doReturn(10).when(cfg).getDiameter();
        GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).registerActualCFG(cfg);
        try {
            ControlFlowDistance cfd = InterProceduralControlFlowDistanceCalculator.getDistance(result,branch, true, "ClassA","method1");
        }catch (NullPointerException e){
            assert (e.getMessage().equals("null given MethodCall"));
        }
    }


    @Test
    public void testNonRootCalculator_TruePath(){
        List<MethodCall> calls = new ArrayList<>();
        MethodCall call0 = new MethodCall("ClassA", "method1", 12, 1, 1);
        call0.branchTrace = Arrays.asList(new Integer[]{1, 2, 10});
        call0.trueDistanceTrace = Arrays.asList(new Double[]{1.0, 1.0,0.5});
        call0.falseDistanceTrace = Arrays.asList(new Double[]{1.0, 1.0,0.6});
        calls.add(call0);

        ExecutionResult result = Mockito.mock(ExecutionResult.class,Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(result.getTrace().getCoveredTrueBranches().contains(ArgumentMatchers.anyInt())).thenReturn(false);
        Mockito.when(result.getTrace().getMethodCalls()).thenReturn(calls);

        Branch branch = Mockito.mock(Branch.class,Mockito.RETURNS_DEEP_STUBS);
        Mockito.doReturn("ClassA").when(branch).getClassName();
        Mockito.doReturn("method1").when(branch).getMethodName();
        Mockito.when(branch.getInstruction().getActualCFG().getDiameter()).thenReturn(10);
        Mockito.when(branch.getActualBranchId()).thenReturn(10);

        // Set cfg
        ActualControlFlowGraph cfg = Mockito.mock(ActualControlFlowGraph.class);
        Mockito.doReturn("IntegrationTestingGraph").when(cfg).getClassName();
        Mockito.doReturn("methodsIntegration").when(cfg).getMethodName();
        Mockito.doReturn(10).when(cfg).getDiameter();
        GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).registerActualCFG(cfg);

        ControlFlowDistance cfd = InterProceduralControlFlowDistanceCalculator.getDistance(result,branch, true, "ClassA","method1");
        assert(cfd.getBranchDistance() == 0.5);
        assert(cfd.getApproachLevel() == 0.0);
    }


    @Test
    public void testNonRootCalculator_FalsePath(){

        // Make list of method calls
        List<MethodCall> calls = new ArrayList<>();
        MethodCall call0 = new MethodCall("ClassA", "method1", 12, 1, 1);
        call0.branchTrace = Arrays.asList(new Integer[]{1, 2, 10});
        call0.trueDistanceTrace = Arrays.asList(new Double[]{1.0, 1.0,0.5});
        call0.falseDistanceTrace = Arrays.asList(new Double[]{1.0, 1.0,0.6});
        calls.add(call0);
        // Mock result
        ExecutionResult result = Mockito.mock(ExecutionResult.class,Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(result.getTrace().getCoveredTrueBranches().contains(ArgumentMatchers.anyInt())).thenReturn(false);
        Mockito.when(result.getTrace().getMethodCalls()).thenReturn(calls);
        //Mock branch
        Branch branch = Mockito.mock(Branch.class,Mockito.RETURNS_DEEP_STUBS);
        Mockito.doReturn("ClassA").when(branch).getClassName();
        Mockito.doReturn("method1").when(branch).getMethodName();
        Mockito.when(branch.getInstruction().getActualCFG().getDiameter()).thenReturn(10);
        Mockito.when(branch.getActualBranchId()).thenReturn(10);

        // Set cfg
        ActualControlFlowGraph cfg = Mockito.mock(ActualControlFlowGraph.class);
        Mockito.doReturn("IntegrationTestingGraph").when(cfg).getClassName();
        Mockito.doReturn("methodsIntegration").when(cfg).getMethodName();
        Mockito.doReturn(10).when(cfg).getDiameter();
        GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).registerActualCFG(cfg);

        ControlFlowDistance cfd = InterProceduralControlFlowDistanceCalculator.getDistance(result,branch, false, "ClassA","method1");
        assert(cfd.getBranchDistance() == 0.6);
        assert(cfd.getApproachLevel() == 0.0);
    }


    @Test
    public void testNonRootCalculator_loop(){

        // Make list of method calls
        List<MethodCall> calls = new ArrayList<>();
        MethodCall call0 = new MethodCall("ClassA", "method1", 12, 1, 1);
        call0.branchTrace = Arrays.asList(new Integer[]{1, 2, 10});
        call0.trueDistanceTrace = Arrays.asList(new Double[]{1.0, 1.0,0.5});
        call0.falseDistanceTrace = Arrays.asList(new Double[]{1.0, 1.0,0.6});
        calls.add(call0);
        // Mock result
        ExecutionResult result = Mockito.mock(ExecutionResult.class,Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(result.getTrace().getCoveredTrueBranches().contains(ArgumentMatchers.anyInt())).thenReturn(false);
        Mockito.when(result.getTrace().getMethodCalls()).thenReturn(calls);
        // Mock BCInstruction
        BytecodeInstruction bc1 = Mockito.mock(BytecodeInstruction.class,Mockito.RETURNS_DEEP_STUBS);
        BytecodeInstruction bc2 = Mockito.mock(BytecodeInstruction.class);
        ControlDependency cd1 = Mockito.mock(ControlDependency.class,Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(cd1.getBranch().getInstruction()).thenReturn(bc1);
        ControlDependency cd2 = Mockito.mock(ControlDependency.class,Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(cd2.getBranch().getInstruction()).thenReturn(bc2);
        Mockito.when(cd2.getBranchExpressionValue()).thenReturn(true);
        Set<ControlDependency> cds = new HashSet<>();
        cds.add(cd1);
        cds.add(cd2);
        Mockito.when(bc1.getControlDependencies()).thenReturn(cds);
        Mockito.when(bc1.getActualCFG().getDiameter()).thenReturn(10);
        // Mock branch
        Branch branch = Mockito.mock(Branch.class,Mockito.RETURNS_DEEP_STUBS);
        Mockito.doReturn("ClassA").when(branch).getClassName();
        Mockito.doReturn("method1").when(branch).getMethodName();
        Mockito.when(branch.getInstruction()).thenReturn(bc1);
        Mockito.when(branch.getActualBranchId()).thenReturn(13);

// Set cfg
        ActualControlFlowGraph cfg = Mockito.mock(ActualControlFlowGraph.class);
        Mockito.doReturn("IntegrationTestingGraph").when(cfg).getClassName();
        Mockito.doReturn("methodsIntegration").when(cfg).getMethodName();
        Mockito.doReturn(10).when(cfg).getDiameter();
        GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).registerActualCFG(cfg);

        ControlFlowDistance cfd = InterProceduralControlFlowDistanceCalculator.getDistance(result,branch, true, "ClassA","method1");

        assert(cfd.getBranchDistance() == 0.0);
        assert(cfd.getApproachLevel() == 2.0);
    }

    @Test(expected = IllegalStateException.class)
    public void testPreCheck_nullBranch(){
        // Make list of method calls
        List<MethodCall> calls = new ArrayList<>();
        MethodCall call0 = new MethodCall("ClassA", "method1", 12, 1, 1);
        call0.branchTrace = Arrays.asList(new Integer[]{1, 2, 10});
        call0.trueDistanceTrace = Arrays.asList(new Double[]{1.0, 1.0,0.5});
        call0.falseDistanceTrace = Arrays.asList(new Double[]{1.0, 1.0,0.6});

        InterProceduralControlFlowDistanceCalculator.nonRootDistancePrechecks(call0,null);
    }

}
