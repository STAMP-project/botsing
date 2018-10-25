package eu.stamp.botsing;

import ch.qos.logback.classic.Level;
import eu.stamp.botsing.reproduction.CrashReproductionHelperTest;
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

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.io.FileMatchers.*;

/**
 * This class contains integration test cases for botsing. Each test method runs Botsing against one of the exmaple of crashes in the module <b>botsing-example</b>
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
    }

    @Test
    public void testFractionCrash() {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.WARN);

        Botsing botsing = new Botsing();

        // setup of the directory with file *class and file *.log
        String user_dir = System.getProperty("user.dir"); // the current directory is the module <b>botsing-reproduction</b>
        File file = new File(user_dir);
        String base_dir = Paths.get(file.getParent(), "botsing-examples").toString(); // the crash to replicate is inside the module <b>botsing-examples</b>

        // Set the output directory
        File outputDir = Paths.get(user_dir, "target", "crash-reproduction-tests").toFile();

        //run Botsing
        String[] prop = {
                "-crash_log",
                Paths.get(base_dir, "src", "main", "resources", "Fraction.log").toString(),
                "-target_frame",
                "" + 1,
                "-projectCP",
                Paths.get(base_dir, "target", "classes").toString() + System.getProperty("path.separator"),
                "-Dtest_dir=" + outputDir.getAbsolutePath(),
        };

        // Check results
        List<TestGenerationResult> results = botsing.parseCommandLine(prop);
        assertThat(results, hasSize(greaterThan(0)));
        ;
        assertThat(results.get(0).getTestGenerationStatus(), is(TestGenerationResult.Status.SUCCESS));

        // Check output directory
        assertThat(outputDir, anExistingDirectory());
        assertThat(outputDir.list(), arrayWithSize(greaterThan(0)));
    }
}