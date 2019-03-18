package eu.stamp.botsing.model.generation;

import ch.qos.logback.classic.Level;
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

import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.io.FileMatchers.anExistingDirectory;
import static org.junit.Assert.assertThat;

public class ModelGenerationTest {

    private static final Logger LOG = LoggerFactory.getLogger(ModelGenerationTest.class);

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
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
    }

    @Test
    public void testParseCommandLine(){
        String user_dir = System.getProperty("user.dir"); // the current directory is the module <b>botsing-model-generation</b>
        File file = new File(user_dir);
        String base_dir = Paths.get(file.getParent(), "botsing-examples").toString();
        String  bin_path = Paths.get(base_dir, "src","main","resources","sample_dep").toString();
        String classPrefix = "org.tudelft";
        File outputDir = Paths.get(user_dir, "target", "generated-models").toFile();
        //run Botsing
        String[] prop = {
                CommandLineParameters.PROJECT_CP_OPT,
                bin_path,
                CommandLineParameters.PROJECT_PREFIX,
                classPrefix,
                CommandLineParameters.OUTPUT_FOLDER,
                outputDir.getAbsolutePath()
        };
        ModelGeneration main = new ModelGeneration();
        main.parseCommandLine(prop);

        // Check output directory
        assertThat(outputDir, anExistingDirectory());
        assertThat(outputDir.list(), arrayWithSize(greaterThan(0)));
    }

}
