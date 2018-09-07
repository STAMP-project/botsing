package eu.stamp.botsing;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

public class CommandLineParameters {

    public static Options getCommandLineOptions() {
        Options options = new Options();
        // define options
        @SuppressWarnings("static-access")
        Option property = OptionBuilder.withArgName("property=value").hasArgs(2).withValueSeparator().withDescription("use value for given property").create("D");
        Option projectCP = new Option("projectCP", true,
                "classpath of the project under test and all its dependencies");
        Option target_frame = new Option("target_frame",true, "Level of the target frame");
        Option crash_log = new Option("crash_log",true, "Directory of the given stack trace");

        options.addOption(property);
        options.addOption(projectCP);
        options.addOption(target_frame);
        options.addOption(crash_log);

        return options;
    }

}
