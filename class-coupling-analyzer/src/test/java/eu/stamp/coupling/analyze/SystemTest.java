package eu.stamp.coupling.analyze;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertThat;

import static org.hamcrest.io.FileMatchers.anExistingDirectory;

public class SystemTest {

    String projectCP;

    Path outDir;

    ClassCouplingAnalyzer main = new ClassCouplingAnalyzer();


    @Before
    public void before(){
        // setup of the directory with file *class and file *.log
        String user_dir = System.getProperty("user.dir"); // the current directory is the module
        // <b>botsing-reproduction</b>
        File file = new File(user_dir);
        String base_dir = Paths.get(file.getParent(), "botsing-examples").toString(); // the crash to replicate is
        // Prepare projectCP
        projectCP = Paths.get(base_dir, "target", "classes").toString();

        outDir = Paths.get(file.getParent(),"class-coupling-analyzer","system-level-output");

    }

    @Test
    public void testMethodCalls_withoutTargetClass(){
        String[] prop = {
                "-project_prefix",
                "eu.stamp.botsing.coupling",
                "-project_cp",
                projectCP,
                "-out_dir",
                outDir.toString()

        };

        main.parseCommandLine(prop);

        assertThat(outDir.toFile(), anExistingDirectory());
    }

    @Test
    public void testMethodCalls_withTargetClass(){

        String[] prop = {
                "-project_prefix",
                "eu.stamp.botsing.coupling",
                "-project_cp",
                projectCP,
                "-target_class",
                "eu.stamp.botsing.coupling.Callee",
                "-out_dir",
                outDir.toString()
        };

        main.parseCommandLine(prop);

        assertThat(outDir.toFile(), anExistingDirectory());
    }

    @Test
    public void testPoly_withoutTargetClass(){

        String[] prop = {
                "-project_prefix",
                "eu.stamp.botsing.poly",
                "-project_cp",
                projectCP,
                "-out_dir",
                outDir.toString()
        };

        main.parseCommandLine(prop);

        assertThat(outDir.toFile(), anExistingDirectory());
    }

    @Test
    public void testPoly_withTargetClass(){

        String[] prop = {
                "-project_prefix",
                "eu.stamp.botsing.poly",
                "-project_cp",
                projectCP,
                "-target_class",
                "eu.stamp.botsing.poly.SubClass",
                "-out_dir",
                outDir.toString()
        };

        main.parseCommandLine(prop);

        assertThat(outDir.toFile(), anExistingDirectory());
    }

    @After
    public void removeOutputDir(){
        outDir.toFile().delete();
    }
}
