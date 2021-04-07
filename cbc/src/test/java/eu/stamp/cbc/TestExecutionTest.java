package eu.stamp.cbc;

import eu.stamp.botsing.commons.SetupUtility;
import eu.stamp.cbc.calculator.CoupledBranches;
import eu.stamp.cbc.extraction.ExecutionTracePool;
import eu.stamp.cling.IntegrationTestingProperties;
import org.evosuite.testcase.execution.ExecutionTrace;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;

import static eu.stamp.botsing.commons.SetupUtility.getCompatibleCP;

public class TestExecutionTest {
    private final String callerClass = "eu.stamp.botsing.coupling.Caller";
    private final String calleeClass = "eu.stamp.botsing.coupling.Callee";
    private final String testSuite = "eu.stamp.botsing.coupling.CallerTest";


    @Before
    public void loadCUT(){
        // setup of the directory with file *class and file *.log
        String user_dir = System.getProperty("user.dir"); // the current directory is the module
        // <b>botsing-reproduction</b>
        File file = new File(user_dir);
        String base_dir = Paths.get(file.getParent(), "botsing-examples").toString();
        System.out.println(base_dir);
        String projectCP = Paths.get(base_dir, "target", "classes").toString();
        String testCP = Paths.get(base_dir, "target", "test-classes").toString();
        // Setup CP
        SetupUtility.setupProjectClasspath(getCompatibleCP(projectCP+File.pathSeparator+testCP));
        // Instrument Caller and Callee classes
        // 1- Set cling properties
        IntegrationTestingProperties.fitnessFunctions = new IntegrationTestingProperties.FitnessFunction[]{IntegrationTestingProperties.FitnessFunction.Branch_Pairs};
        IntegrationTestingProperties.TARGET_CLASSES = new String[]{callerClass, calleeClass};
        //2- instrument
        CoupledBranches.instrumentClasses();
    }

    @Test
    public void testExecution(){

        String testCase = testSuite+".test0";

        Executor executor =  new Executor(testSuite,callerClass,calleeClass);
        executor.execute();

        ExecutionTracePool tracePool = ExecutionTracePool.getInstance();

        assert (tracePool.getExecutedTests().size() == 1);
        assert (tracePool.getExecutedTests().contains(testCase));
        assert (tracePool.getExecutionTraces().size() == 1);

        ExecutionTrace trace = tracePool.getExecutionTrace(testCase);
        assert (trace.getCoverageData().containsKey(callerClass));
        assert (trace.getCoverageData().containsKey(calleeClass));
        assert (trace.getCoveredFalseBranches().size() == 1);
        assert (trace.getCoveredTrueBranches().size() == 3);
    }

    @Test
    public void nullTestExecution(){
        Executor executor =  new Executor(testSuite+"wrong",callerClass,calleeClass);
        executor.execute();
    }

    @Test
    public void nullCallerExecution(){
        try {
            Executor executor = new Executor(testSuite, callerClass + "Wrong", calleeClass);
            Assert.fail("RuntimeException is expected!");
        }catch (RuntimeException e){
            assert (e.getMessage().equals("java.lang.ClassNotFoundException"));
        }
    }
}
