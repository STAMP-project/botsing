package eu.stamp.botsing.commons.coverage.branch;

import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import org.evosuite.coverage.ControlFlowDistance;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.BytecodeInstructionPool;
import org.evosuite.testcase.execution.ExecutionResult;
import org.junit.Assert;
import org.junit.Test;
import org.evosuite.shaded.org.mockito.Mockito;

public class IntegrationTestingBranchCoverageGoalTest {

    @Test
    public void test(){
        String className = "org.example.Class";
        String methodName = "method0";
        int lineNumber = 1;

        BytecodeInstruction instruction = Mockito.mock(BytecodeInstruction.class);
        Mockito.doReturn(className).when(instruction).getClassName();
        Mockito.doReturn(methodName).when(instruction).getMethodName();
        Mockito.doReturn(lineNumber).when(instruction).getLineNumber();


        BytecodeInstructionPool pool = BytecodeInstructionPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
        pool.registerInstruction(instruction);

        IntegrationTestingBranchCoverageGoal coverageGoal = new IntegrationTestingBranchCoverageGoal(className,methodName);

        ExecutionResult result = Mockito.mock(ExecutionResult.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(result.getTrace().getCoveredMethods().contains(className + "." + methodName)).thenReturn(true);

        ControlFlowDistance resultDistance = coverageGoal.getDistance(result);
        Assert.assertEquals(0, resultDistance.getApproachLevel());
        Assert.assertEquals(0, resultDistance.getBranchDistance(),0);
    }
}
