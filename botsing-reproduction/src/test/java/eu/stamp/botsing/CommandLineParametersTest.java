package eu.stamp.botsing;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.junit.Test;

import java.util.Collection;
import java.util.Iterator;

import static org.junit.Assert.*;

public class CommandLineParametersTest {

    @Test
    public void testGetCommandLineOptions() {
        Options opt = CommandLineParameters.getCommandLineOptions();
        Collection<Option> opts = opt.getOptions();
        Iterator<Option> iter = opts.iterator();

        Option opt1 = iter.next();
        assertTrue(opt1.getDescription().equalsIgnoreCase("use value for given property"));
        assertTrue(opt1.getArgName().equalsIgnoreCase("property=value"));
        assertTrue(opt1.getOpt().equalsIgnoreCase("D"));

        Option opt2 = iter.next();
        assertTrue(opt2.getDescription().equalsIgnoreCase("classpath of the project under test and all its dependencies"));
        assertTrue(opt2.getArgName() == null);
        assertTrue(opt2.getOpt().equalsIgnoreCase("projectCP"));

        Option opt3 = iter.next();
        assertTrue(opt3.getDescription().equalsIgnoreCase("Level of the target frame"));
        assertTrue(opt3.getArgName() == null);
        assertTrue(opt3.getOpt().equalsIgnoreCase("target_frame"));

        Option opt4 = iter.next();
        assertTrue(opt4.getDescription().equalsIgnoreCase("Directory of the given stack trace"));
        assertTrue(opt4.getArgName() == null);
        assertTrue(opt4.getOpt().equalsIgnoreCase("crash_log"));

        assertFalse(iter.hasNext());
    }
}