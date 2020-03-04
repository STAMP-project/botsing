package eu.stamp.cbc;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;

public class CBCTest {
    private final String callerClass = "eu.stamp.botsing.coupling.Caller";
    private final String calleeClass = "eu.stamp.botsing.coupling.Callee";
    private final String testSuite = "eu.stamp.botsing.coupling.CallerTest";
    private String cp;

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
        cp = projectCP+File.pathSeparator+testCP;
    }

    @Test
    public void testParameterChecker(){
        CoupledBranchesCalculator main = new CoupledBranchesCalculator();

                String[] prop = {
                "-project_cp",
                cp,
                "-caller",
                callerClass,
                "-callee",
                calleeClass,
        };
        main.parseCommandLine(prop);
    }

    @Test
    public void testNormalRun(){
        CoupledBranchesCalculator main = new CoupledBranchesCalculator();

                String[] prop = {
                "-project_cp",
                cp,
                "-test_suite",
                testSuite,
                "-caller",
                callerClass,
                "-callee",
                calleeClass,
        };

        main.parseCommandLine(prop);
    }

    @Test
    public void testEvoSuiteParameters(){
        CoupledBranchesCalculator main = new CoupledBranchesCalculator();

        String[] prop = {
                "-project_cp",
                cp,
                "-test_suite",
                testSuite,
                "-caller",
                callerClass,
                "-callee",
                calleeClass,
                "-Dctg_seeds_file_in="+Paths.get("parent").toString()
        };

        main.parseCommandLine(prop);
    }


    @Test
    public void testHelp(){
        CoupledBranchesCalculator main = new CoupledBranchesCalculator();

        String[] prop = {
                "-help"
        };

        main.parseCommandLine(prop);
    }
}
