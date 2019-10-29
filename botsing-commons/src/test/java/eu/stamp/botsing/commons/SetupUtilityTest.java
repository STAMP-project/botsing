package eu.stamp.botsing.commons;

import eu.stamp.botsing.commons.instrumentation.ClassInstrumentation;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.evosuite.TestGenerationContext;
import org.evosuite.classpath.ClassPathHandler;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.Properties;

import static eu.stamp.botsing.commons.SetupUtility.setupProjectClasspath;

public class SetupUtilityTest {

    @Test
    public void testPrintHelpMessage(){
        String[] args = new String[1];
        args[0]="-project_cp";
        Options options = new Options();

        options.addOption(Option.builder("project_cp")
                .hasArg()
                .desc("classpath of the project under test and all its dependencies")
                .build());

        CommandLine commandLine = SetupUtility.parseCommands(new String[0], options,false);
        Assert.assertEquals(0,commandLine.getArgs().length);

        commandLine = SetupUtility.parseCommands(args,   options,false);
        Assert.assertEquals(null, commandLine);
        commandLine = SetupUtility.parseCommands(args,   options,true);
        Assert.assertEquals(null, commandLine);
    }


    @Test
    public void testUpdateEvoSuiteProperties(){
        Properties properties =  new Properties();
        properties.setProperty("search_budget","100");
        SetupUtility.updateEvoSuiteProperties(properties);
        Assert.assertEquals(100, org.evosuite.Properties.SEARCH_BUDGET);

        properties.setProperty("not_exist","100");
        SetupUtility.updateEvoSuiteProperties(properties);
    }


    @Test
    public void testGetCompatibleCP(){
        // setup of the directory with file *class and file *.log
        String user_dir = System.getProperty("user.dir"); // the current directory is the module
        // <b>botsing-reproduction</b>
        File file = new File(user_dir);
        String base_dir = Paths.get(file.getParent(), "botsing-examples").toString(); // the crash to replicate is
        // Prepare projectCP
        String projectCP = Paths.get(base_dir, "target", "classes").toString();
        String[] cps = SetupUtility.getCompatibleCP(projectCP);

        Assert.assertEquals(1,cps.length);
        Assert.assertEquals(projectCP,cps[0]);
    }


    @Test
    public void testSetupProjectClasspath(){
        // setup of the directory with file *class and file *.log
        String user_dir = System.getProperty("user.dir"); // the current directory is the module
        // <b>botsing-reproduction</b>
        File file = new File(user_dir);
        String base_dir = Paths.get(file.getParent(), "botsing-examples").toString(); // the crash to replicate is
        // Prepare projectCP
        String projectCP = Paths.get(base_dir, "target", "classes").toString();
        String[] cps = new String[1];

        cps[0] = projectCP;

        SetupUtility.setupProjectClasspath(cps);

        assert (ClassPathHandler.getInstance().getEvoSuiteClassPath().contains("botsing/botsing-examples/target/classes"));

        cps[0]="";

        SetupUtility.setupProjectClasspath(cps);
    }


    @Test
    public void testAnalyzeClassDependencies(){
        String targetClass = "eu.stamp.botsing.Fraction";
        // setup of the directory with file *class and file *.log
        String user_dir = System.getProperty("user.dir"); // the current directory is the module
        // <b>botsing-reproduction</b>
        File file = new File(user_dir);
        String base_dir = Paths.get(file.getParent(), "botsing-examples").toString(); // the crash to replicate is
        // Prepare projectCP
        String projectCP = Paths.get(base_dir, "target", "classes").toString();
        String[] cps = new String[1];

        cps[0] = projectCP;

        setupProjectClasspath(cps);

        ClassInstrumentation.instrumentClassByTestExecution(targetClass);
        SetupUtility.analyzeClassDependencies(targetClass);

        Assert.assertTrue(TestGenerationContext.getInstance().getClassLoaderForSUT().getLoadedClasses().contains(targetClass));



    }
}
