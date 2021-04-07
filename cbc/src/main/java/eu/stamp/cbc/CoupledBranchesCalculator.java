package eu.stamp.cbc;

import eu.stamp.cbc.calculator.CoupledBranches;
import org.apache.commons.cli.*;
import static eu.stamp.cbc.CommandLineParameters.*;
import static eu.stamp.botsing.commons.SetupUtility.*;

import org.evosuite.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class CoupledBranchesCalculator {


    private static final Logger LOG = LoggerFactory.getLogger(CoupledBranchesCalculator.class);


    public void parseCommandLine(String[] args) {
        // Parse commands according to the defined options
        Options options = CommandLineParameters.getCommandLineOptions();
        CommandLine commands = parseCommands(args, options);


        // If help option is provided
        if (commands.hasOption(HELP_OPT)) {
            printHelpMessage(options);
        } else if (commands.hasOption(PROJECT_CP_OPT) && // Check the required options are available
                commands.hasOption(TEST_SUITE) &&
                commands.hasOption(CALLER) &&
                commands.hasOption(CALLEE)) {

            // Calculate CBC
            // Update EvoSuite's properties
            java.util.Properties properties = commands.getOptionProperties(D_OPT);
            updateEvoSuiteProperties(properties);
            // Setup project class paths
            setupProjectClasspath(getCompatibleCP(commands.getOptionValue(PROJECT_CP_OPT)));

            // Get Caller and Callee
            String caller = commands.getOptionValue(CALLER);
            String callee = commands.getOptionValue(CALLEE);

            // Get test Dir
            if (commands.hasOption(TEST_SUITE)) {
                String clingTest = commands.getOptionValue(TEST_SUITE);
                CoupledBranches.calculate(clingTest, caller, callee);
            }
        } else {
            LOG.error("A mandatory option -{} -{} -{} -{} -{} is missing!", PROJECT_CP_OPT, TEST_SUITE, CALLER, CALLEE);
            printHelpMessage(options);
        }
    }

    public static void updateEvoSuiteProperties(java.util.Properties properties){
        for (String property : properties.stringPropertyNames()) {
            try {
                Properties.getInstance().setValue(property, properties.getProperty(property));
            } catch (Properties.NoSuchParameterException e) {
                LOG.error("{} parameter does not exist", property);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }


    public static CommandLine parseCommands(String[] args, Options options){
        CommandLineParser parser = new DefaultParser();
        CommandLine commands;
        try {
            commands = parser.parse(options, args);
        } catch (ParseException e) {
            LOG.error("Could not parse command line!", e);
            printHelpMessage(options);
            return null;
        }
        return commands;
    }


    public static void printHelpMessage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar cbc.jar -project_cp dep1.jar;dep2.jar test_dir <test suite directory> )", options);
    }

    @SuppressWarnings("checkstyle:systemexit")
    public static void main(String[] args) {
        CoupledBranchesCalculator bot = new CoupledBranchesCalculator();
        bot.parseCommandLine(args);
        System.exit(0);
    }
}
