package eu.stamp.cbc.extraction;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ExecutionTracePoolTest {
    ExecutionTracePool tracePool = ExecutionTracePool.getInstance();
    @Before
    public void clearPool(){
        tracePool.clear();
    }

    @Test
    public void testEmptyPool(){
        try {
            tracePool.getExecutionTrace("");
            Assert.fail("IllegalArgumentException is expected!");
        }catch (IllegalArgumentException e){
            assert (e.getMessage().equals("Coverage data of test "+""+" is not available!"));
        }
    }

    @Test
    public void registerTwoTraceForOneTestCase(){
        tracePool.registerNewCoverageData("test0",null);
        try {
        tracePool.registerNewCoverageData("test0",null);
            Assert.fail("IllegalStateException is expected!");
        }catch (IllegalStateException e){
            assert (e.getMessage().equals("Coverage data for test"+"test0"+"is already available."));
        }
    }
}
