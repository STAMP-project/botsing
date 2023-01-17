package eu.stamp.cling.coverage.branch;

import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import org.evosuite.Properties;
import org.evosuite.coverage.branch.Branch;
import org.evosuite.coverage.branch.BranchCoverageTestFitness;
import org.evosuite.coverage.branch.BranchPool;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.BytecodeInstructionPool;
import org.junit.Assert;
import org.junit.Test;
import org.evosuite.shaded.org.mockito.Mockito;

import java.util.List;

public class IntegrationTestingBranchCoverageFactoryTest {

    String className = "ClassA";
    String methodName = "method0";
    int lineNumber = 1;

    @Test
    public void testNullRootBranch(){
        try{
            IntegrationTestingBranchCoverageFactory.createRootBranchTestFitness(null);
            Assert.fail("IllegalArgumentException is expected!");
        }catch (IllegalArgumentException e){
            // do Nothing
        }
    }

    @Test
    public void testRootBranch(){


        BytecodeInstruction instruction = Mockito.mock(BytecodeInstruction.class);
        Mockito.when(instruction.getClassName()).thenReturn(className);
        Mockito.when(instruction.getMethodName()).thenReturn(methodName);
        Mockito.when(instruction.getLineNumber()).thenReturn(lineNumber);

        BytecodeInstructionPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).registerInstruction(instruction);
        BranchCoverageTestFitness testFitness = IntegrationTestingBranchCoverageFactory.createRootBranchTestFitness(instruction);

        Assert.assertEquals(className, testFitness.getClassName());
        Assert.assertEquals(methodName, testFitness.getMethod());
        Assert.assertEquals(null, testFitness.getBranch());
    }

    @Test
    public void testEvoSuitecreateBranchCoverageTestFitness(){
        Branch branch = Mockito.mock(Branch.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(branch.getClassName()).thenReturn(className);
        Mockito.when(branch.getMethodName()).thenReturn(methodName);
        Mockito.when(branch.getInstruction().getLineNumber()).thenReturn(lineNumber);
        boolean expression = true;

        BranchCoverageTestFitness testFitness = IntegrationTestingBranchCoverageFactory.EvoSuitecreateBranchCoverageTestFitness(branch,expression);

        Assert.assertEquals(className, testFitness.getClassName());
        Assert.assertEquals(methodName, testFitness.getMethod());
        Assert.assertEquals(branch, testFitness.getBranch());
    }

    @Test
    public void testComputeCoverageGoals(){
        Properties.TARGET_CLASS=className;
        Properties.TARGET_METHOD=methodName;
        BranchPool branchPool = BranchPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
        BytecodeInstruction instruction = Mockito.mock(BytecodeInstruction.class);
        Mockito.when(instruction.getClassName()).thenReturn(className);
        Mockito.when(instruction.getMethodName()).thenReturn(methodName);
        Mockito.when(instruction.getLineNumber()).thenReturn(lineNumber);
        Mockito.when(instruction.isActualBranch()).thenReturn(true);
        Mockito.when(instruction.isBranch()).thenReturn(true);

        branchPool.registerAsBranch(instruction);

        IntegrationTestingBranchCoverageFactory factory = new IntegrationTestingBranchCoverageFactory();
        List<BranchCoverageTestFitness> goals = factory.getCoverageGoals();
        Assert.assertEquals(2, goals.size());
    }

}
