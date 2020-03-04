package eu.stamp.cbc.calculator;

import eu.stamp.botsing.commons.SetupUtility;
import eu.stamp.cling.IntegrationTestingProperties;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;

import static eu.stamp.botsing.commons.SetupUtility.getCompatibleCP;

public class CoupledBranchesTest {
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
    public void test0(){
        CoupledBranches.calculate(testSuite,callerClass,calleeClass);
    }
}
