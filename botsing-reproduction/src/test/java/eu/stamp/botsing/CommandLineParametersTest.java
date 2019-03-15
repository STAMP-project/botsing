package eu.stamp.botsing;

import org.apache.commons.cli.Options;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

import static eu.stamp.botsing.CommandLineParameters.*;

public class CommandLineParametersTest {

    private static final Logger LOG = LoggerFactory.getLogger(CommandLineParametersTest.class);

    @Rule
    public TestRule watcher = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            LOG.info(String.format("Starting test: %s()...",
                    description.getMethodName()));
        }
    };

    @Test
    public void testGetCommandLineOptions() {
        Options opt = CommandLineParameters.getCommandLineOptions();

        assertTrue(opt.hasOption(D_OPT));
        assertTrue(opt.hasOption(PROJECT_CP_OPT));
        assertTrue(opt.hasOption(TARGET_FRAME_OPT));
        assertTrue(opt.hasOption(CRASH_LOG_OPT));
        assertTrue(opt.hasOption(HELP_OPT));
    }
}