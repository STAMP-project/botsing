package eu.stamp.cling.coverage.branch;

import org.evosuite.coverage.branch.Branch;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.evosuite.shaded.org.mockito.Mockito;

public class BranchPairPoolTest {

    Branch branch1;
    Branch branch2;
    Branch branch3;
    BytecodeInstruction callSite;
    BytecodeInstruction callSite2;
    String methodName = "methodName";


    @Before
    public void before(){
        branch1 = Mockito.mock(Branch.class);
        branch2 = Mockito.mock(Branch.class);
        callSite = Mockito.mock(BytecodeInstruction.class);
        Mockito.when(callSite.getMethodName()).thenReturn(methodName);
        callSite2 = Mockito.mock(BytecodeInstruction.class);
        Mockito.when(callSite2.getMethodName()).thenReturn(methodName+"_2");
        branch3 = Mockito.mock(Branch.class);

        BranchPairPool.getInstance().pool.clear();
        BranchPairPool.getInstance().addPair(branch1,branch2,callSite);
    }

    @Test
    public void testGetterAndSetter(){
        BranchPairPool.getInstance().addPair(branch1,branch2,callSite,true);

        Assert.assertEquals(1, BranchPairPool.getInstance().getBranchPairs().size());
        BranchPairPool.getInstance().addPair(branch1,branch2,callSite2);
        Assert.assertEquals(1, BranchPairPool.getInstance().getBranchPairs().size());

        BranchPairPool.getInstance().addPair(branch1,branch3,callSite2);
        Assert.assertEquals(2, BranchPairPool.getInstance().getBranchPairs().size());
    }

    @Test
    public void testGetSetOfMethodsWithCallSite(){
        BranchPairPool.getInstance().addPair(branch1,branch3,callSite2);
        Assert.assertEquals(2, BranchPairPool.getInstance().getSetOfMethodsWithCallSite().size());
        Assert.assertTrue(BranchPairPool.getInstance().getSetOfMethodsWithCallSite().contains(methodName));
        Assert.assertTrue(BranchPairPool.getInstance().getSetOfMethodsWithCallSite().contains(methodName+"_2"));
    }
}
