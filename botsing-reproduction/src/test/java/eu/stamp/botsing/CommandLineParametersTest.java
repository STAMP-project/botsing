package eu.stamp.botsing;

import static eu.stamp.botsing.CommandLineParameters.CRASH_LOG_OPT;
import static eu.stamp.botsing.CommandLineParameters.D_OPT;
import static eu.stamp.botsing.CommandLineParameters.HELP_OPT;
import static eu.stamp.botsing.CommandLineParameters.PROJECT_CP_OPT;
import static eu.stamp.botsing.CommandLineParameters.TARGET_FRAME_OPT;
import static org.junit.Assert.assertTrue;

import org.apache.commons.cli.Options;
import org.junit.Test;

public class CommandLineParametersTest {

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