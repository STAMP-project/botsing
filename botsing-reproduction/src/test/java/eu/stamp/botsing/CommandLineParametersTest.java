package eu.stamp.botsing;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.junit.Test;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;

import static eu.stamp.botsing.CommandLineParameters.*;

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