package eu.stamp.botsing.integration;

import static eu.stamp.botsing.commons.SetupUtility.*;
import static eu.stamp.botsing.integration.CommandLineParameters.*;

import eu.stamp.botsing.integration.integrationtesting.IntegrationTesting;
import org.apache.commons.cli.*;
import org.evosuite.result.TestGenerationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class BotsingIntegration {

    private static final Logger LOG = LoggerFactory.getLogger(BotsingIntegration.class);

    public List<TestGenerationResult> parseCommandLine(String[] args) {
        // Get default properties
        IntegrationTestingProperties integrationTestingProperties = IntegrationTestingProperties.getInstance();

        // Parse commands according to the defined options
        Options options = CommandLineParameters.getCommandLineOptions();
        CommandLine commands = parseCommands(args, options);

        // If help option is provided
        if (commands.hasOption(HELP_OPT)) {
            printHelpMessage(options);
        } else if(!commands.hasOption(PROJECT_CP_OPT) || !commands.hasOption(TARGET_CLASSES) ) { // Check the required options are there
            LOG.error("A mandatory option -{} -{} is missing!", PROJECT_CP_OPT, TARGET_CLASSES);
            printHelpMessage(options);
        } else {// Otherwise, proceed to crash reproduction

            // Update EvoSuite's properties
            java.util.Properties properties = commands.getOptionProperties(D_OPT);
            updateEvoSuiteProperties(properties);
            // Setup project class paths
            integrationTestingProperties.setClasspath(getCompatibleCP(commands.getOptionValue(PROJECT_CP_OPT)));
            setupProjectClasspath(integrationTestingProperties.getProjectClassPaths());
            // Setup target classes
            setupTargetClasses(commands.getOptionValue(TARGET_CLASSES));
            // Set the search algorithm
            if(commands.hasOption(SEARCH_ALGORITHM)){
                String algorithm = commands.getOptionValue(SEARCH_ALGORITHM);
                IntegrationTestingProperties.searchAlgorithm = IntegrationTestingProperties.SearchAlgorithm.valueOf(algorithm);
            }
            // Set fitness function(s)
            if(commands.hasOption(FITNESS_FUNCTION)){
                String fitnessFunction = commands.getOptionValue(FITNESS_FUNCTION);
                IntegrationTestingProperties.fitnessFunctions = new IntegrationTestingProperties.FitnessFunction[]{IntegrationTestingProperties.FitnessFunction.valueOf(fitnessFunction)};
            }

            // execute
          return IntegrationTesting.execute();
        }
        return null;

    }

    private void setupTargetClasses(String targetClasses) {
        String separator = System.getProperty("path.separator");
        IntegrationTestingProperties.TARGET_CLASSES = targetClasses.split(separator);
        if(IntegrationTestingProperties.TARGET_CLASSES.length <2){
            throw new IllegalArgumentException("Number of target classes should be equal or more than one");
        }else if (IntegrationTestingProperties.TARGET_CLASSES.length > 2){
            LOG.warn("Searching for integration testing between more than 2 classes may lead to path explosion.");
            throw new IllegalArgumentException("Botsing does not support more than two classes for now!");
        }
    }


}
