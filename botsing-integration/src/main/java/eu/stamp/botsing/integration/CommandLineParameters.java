package eu.stamp.botsing.integration;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class CommandLineParameters {

    public static final String D_OPT = "D";
    public static final String PROJECT_CP_OPT = "project_cp";
    public static final String HELP_OPT = "help";
    public static final String SEARCH_ALGORITHM = "search_algorithm";
    public static final String FITNESS_FUNCTION = "fitness";


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
        // Search Algorithm
        options.addOption(Option.builder(SEARCH_ALGORITHM)
                .hasArg()
                .desc("Select the search algorithm.")
                .build());

        // FitnessFunction
        options.addOption(Option.builder(FITNESS_FUNCTION)
                .hasArg()
                .desc("Fitness function for guidance of the search algorithm")
                .build());
        // Help message
        options.addOption(Option.builder(HELP_OPT)
                .desc("Prints this help message.")
                .build());

        return options;
    }

}
