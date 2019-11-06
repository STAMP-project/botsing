package eu.stamp.coupling.analyze;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class CommandLineParameters {

    public static final String D_OPT = "D";
    public static final String PROJECT_CP_OPT = "project_cp";
    public static final String HELP_OPT = "help";
    public static final String OUTPUT_FOLDER = "out_dir";
    public static final String PROJECT_PREFIX = "project_prefix";
    public static final String TARGET_CLASS = "target_class";


    public static Options getCommandLineOptions() {
        Options options = new Options();
        // Properties
        options.addOption(Option.builder(D_OPT)
                .numberOfArgs(2)
                .argName("property=value")
                .valueSeparator()
                .desc("use value for given property")
                .build());
        // Classpath
        options.addOption(Option.builder(PROJECT_CP_OPT)
                .hasArg()
                .desc("classpath of the project under test and all its dependencies")
                .build());
        // output directory
        options.addOption(Option.builder(OUTPUT_FOLDER)
                .hasArg()
                .desc("the output directory.")
                .build());
        // the class that should always be in the branch pairs
        options.addOption(Option.builder(TARGET_CLASS)
                .hasArg()
                .desc("the class that should always be in the branch pairs.")
                .build());
        //if all of the interesting classes has the same prefix, the user can use this option
        options.addOption(Option.builder(PROJECT_PREFIX)
                .hasArg()
                .desc("Prefix of the interesting classes")
                .build());
        // Help message
        options.addOption(Option.builder(HELP_OPT)
                .desc("Prints this help message.")
                .build());

        return options;
    }

}
