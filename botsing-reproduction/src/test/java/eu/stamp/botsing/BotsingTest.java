package eu.stamp.botsing;

import ch.qos.logback.classic.Level;
import org.evosuite.Properties;
import org.evosuite.result.TestGenerationResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.io.FileMatchers.anExistingDirectory;
import static org.junit.Assert.*;

/**
 * This class contains integration test cases for botsing. Each test method runs Botsing against one of the exmaple of
 * crashes in the module <b>botsing-example</b>
 */
public class BotsingTest {


    private static final Logger LOG = LoggerFactory.getLogger(BotsingTest.class);

    @Rule
    public TestRule watcher = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            LOG.info(String.format("Starting test: %s()...",
                    description.getMethodName()));
        }
    };


    @Before
    public void initialize() {
        Properties.RANDOM_SEED = (long) 1;
        ch.qos.logback.classic.Logger root =
                (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.WARN);
    }

    @Test
    public void testFractionCrash() {
        Botsing botsing = new Botsing();

        // setup of the directory with file *class and file *.log
        String user_dir = System.getProperty("user.dir"); // the current directory is the module
        // <b>botsing-reproduction</b>
        File file = new File(user_dir);
        String base_dir = Paths.get(file.getParent(), "botsing-examples").toString(); // the crash to replicate is
        // inside the module <b>botsing-examples</b>

        // Set the output directory
        File outputDir = Paths.get(user_dir, "target", "crash-reproduction-tests").toFile();

        //run Botsing
        String[] prop = {
                "-" + CommandLineParameters.CRASH_LOG_OPT,
                Paths.get(base_dir, "src", "main", "resources", "Fraction.log").toString(),
                "-" + CommandLineParameters.TARGET_FRAME_OPT,
                "" + 1,
                "-" + CommandLineParameters.PROJECT_CP_OPT,
                Paths.get(base_dir, "target", "classes").toString() + System.getProperty("path.separator"),
                "-" + CommandLineParameters.D_OPT + "test_dir=" + outputDir.getAbsolutePath(),
        };

        // Check results
        List<TestGenerationResult> results = botsing.parseCommandLine(prop);
        assertThat(results, hasSize(greaterThan(0)));

        assertThat(results.get(0).getTestGenerationStatus(), is(TestGenerationResult.Status.SUCCESS));

        // Check output directory
        assertThat(outputDir, anExistingDirectory());
        assertThat(outputDir.list(), arrayWithSize(greaterThan(0)));
    }

    @Test
    public void testPrivateFractionCrash() {
        Botsing botsing = new Botsing();

        // setup of the directory with file *class and file *.log
        String user_dir = System.getProperty("user.dir"); // the current directory is the module
        // <b>botsing-reproduction</b>
        File file = new File(user_dir);
        String base_dir = Paths.get(file.getParent(), "botsing-examples").toString(); // the crash to replicate is
        // inside the module <b>botsing-examples</b>

        // Set the output directory
        File outputDir = Paths.get(user_dir, "target", "crash-reproduction-tests").toFile();

        //run Botsing
        String[] prop = {
                "-" + CommandLineParameters.CRASH_LOG_OPT,
                Paths.get(base_dir, "src", "main", "resources", "PrivateFraction.log").toString(),
                "-" + CommandLineParameters.TARGET_FRAME_OPT,
                "" + 1,
                "-" + CommandLineParameters.PROJECT_CP_OPT,
                Paths.get(base_dir, "target", "classes").toString() + System.getProperty("path.separator"),
                "-" + CommandLineParameters.D_OPT + "test_dir=" + outputDir.getAbsolutePath(),
        };

        // Check results
        List<TestGenerationResult> results = botsing.parseCommandLine(prop);
        assertTrue(results.size() > 0);
        assertEquals(TestGenerationResult.Status.SUCCESS, results.get(0).getTestGenerationStatus());

        // Check output directory
        assertThat(outputDir, anExistingDirectory());
        assertThat(outputDir.list(), arrayWithSize(greaterThan(0)));
    }

    @Test
    public void testHelpOption() {
        Botsing botsing = new Botsing();
        String[] prop = {"-" + CommandLineParameters.HELP_OPT};
        Object result = botsing.parseCommandLine(prop);
        assertThat("Call with option " + CommandLineParameters.HELP_OPT, result, nullValue());
    }

    @Test
    public void testMissingMandatoryOption() {
        Botsing botsing = new Botsing();
        String[] prop = {
                "-" + CommandLineParameters.TARGET_FRAME_OPT,
                "1",
                "-" + CommandLineParameters.PROJECT_CP_OPT,
                "path"
        };
        Object result = botsing.parseCommandLine(prop);
        assertThat("Missing option " + CommandLineParameters.CRASH_LOG_OPT, result, nullValue());

        botsing = new Botsing();
        prop = new String[]{
                "-" + CommandLineParameters.CRASH_LOG_OPT,
                "file.log",
                "-" + CommandLineParameters.PROJECT_CP_OPT,
                "path"
        };
        result = botsing.parseCommandLine(prop);
        assertThat("Missing option " + CommandLineParameters.TARGET_FRAME_OPT, result, nullValue());

        botsing = new Botsing();
        prop = new String[]{
                "-" + CommandLineParameters.CRASH_LOG_OPT,
                "file.log",
                "-" + CommandLineParameters.TARGET_FRAME_OPT,
                "1"
        };

        result = botsing.parseCommandLine(prop);
        assertThat("Missing option " + CommandLineParameters.PROJECT_CP_OPT, result, nullValue());
    }


}
