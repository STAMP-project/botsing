package eu.stamp.cling.coverage.branch;

import org.evosuite.coverage.branch.Branch;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.evosuite.shaded.org.mockito.Mockito;

public class BranchPairTest {

    Branch branch1;
    Branch branch2;
    BytecodeInstruction callSite;


    @Before
    public void before(){
        branch1 = Mockito.mock(Branch.class);
        branch2 = Mockito.mock(Branch.class);

        callSite = Mockito.mock(BytecodeInstruction.class);
    }

    @Test
    public void testGetterSetter(){
        BranchPair branchPair = new BranchPair(branch1,branch2,callSite);

        Assert.assertEquals(branch1,branchPair.getFirstBranch());
        Assert.assertEquals(branch2,branchPair.getSecondBranch());
        Assert.assertEquals(callSite,branchPair.getCallSite());
        Assert.assertFalse(branchPair.isDependent());
        try {
            branchPair.getExpression();
            Assert.fail("IllegalArgumentException is expected!");
        }catch (IllegalArgumentException e){
            // do nothing
        }
    }

    @Test
    public void testGetterSetterWithExpression(){
        BranchPair branchPair = new BranchPair(branch1,branch2,callSite,true);

        Assert.assertEquals(branch1,branchPair.getFirstBranch());
        Assert.assertEquals(branch2,branchPair.getSecondBranch());
        Assert.assertEquals(callSite,branchPair.getCallSite());
        Assert.assertTrue(branchPair.getExpression());
        Assert.assertTrue(branchPair.isDependent());
    }

    @Test
    public void testEquals(){
        BranchPair branchPair1 = new BranchPair(branch1,branch2,callSite);
        BranchPair branchPair2 = new BranchPair(branch1,branch2,callSite,true);

        Assert.assertTrue(branchPair1.equals(branchPair2));
        Assert.assertFalse(branchPair1.equals(null));
        Assert.assertFalse(branchPair1.equals(""));
    }
}
