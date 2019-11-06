package eu.stamp.cling;

import ch.qos.logback.classic.Level;
import org.evosuite.Properties;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;

public class ClingTest {

    private static String[] targetClasses = {
            "eu.stamp.botsing.coupling.Caller", // caller
            "eu.stamp.botsing.coupling.Callee"}; // callee

    @Before
    public void initialize() {
        Properties.RANDOM_SEED = (long) 1;
        ch.qos.logback.classic.Logger root =
                (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
    }


    @Test
    public void tectCover4outOf6(){
        Cling cling = new Cling();

        // setup of the directory with file *class and file *.log
        String user_dir = System.getProperty("user.dir"); // the current directory is the module
        // <b>botsing-reproduction</b>
        File file = new File(user_dir);
        String base_dir = Paths.get(file.getParent(), "botsing-examples").toString();

        File outputDir = Paths.get(user_dir, "target", "class-integration-tests").toFile();

        //run Cling
        String[] prop = {
                "-" + CommandLineParameters.TARGET_CLASSES,
                getTargetClasses(),
                "-" + CommandLineParameters.PROJECT_CP_OPT,
                Paths.get(base_dir, "target", "classes").toString() + System.getProperty("path.separator"),
                "-" + CommandLineParameters.D_OPT + "test_dir=" + outputDir.getAbsolutePath(),
                "-" + CommandLineParameters.D_OPT + "sandbox=true",
                "-" + CommandLineParameters.D_OPT + "search_budget=10",
                "-"+CommandLineParameters.FITNESS_FUNCTION,
                "Branch_Pairs",
        };

        cling.parseCommandLine(prop);
    }


    private String getTargetClasses() {
        String separator = System.getProperty("path.separator");
        String classes = "";
        for (String className: targetClasses){
            classes+= (className + separator);
        }
        return classes;
    }
}
