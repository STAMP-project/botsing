package eu.stamp.botsing.coupling.analyze;

import eu.stamp.botsing.commons.ClassPaths;
import eu.stamp.botsing.coupling.analyze.calls.MethodCallAnalyzer;
import eu.stamp.botsing.coupling.analyze.hierarchy.ClassesInSameHierarchyTreeAnalyzer;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static eu.stamp.botsing.coupling.analyze.CommandLineParameters.*;

public class ClassCouplingAnalyzer {

    private static final Logger LOG = LoggerFactory.getLogger(ClassCouplingAnalyzer.class);

    @SuppressWarnings("checkstyle:systemexit")
    public static void main(String[] args) {
        ClassCouplingAnalyzer analyzer = new ClassCouplingAnalyzer();
        try {
            analyzer.parseCommandLine(args);
        } catch(IOException e) {
            LOG.error("Exception occured during model generation!", e);
        }
        System.exit(0);
    }


    public void parseCommandLine(String[] args) throws IOException {
        Options options = CommandLineParameters.getCommandLineOptions();
        CommandLine commands = parseCommands(args, options);
        if(commands.hasOption(HELP_OPT)) {
            printHelpMessage(options);
        } else if(commands.hasOption(PROJECT_CP_OPT) && commands.hasOption(PROJECT_PREFIX)) { // Generate the model for passed cps
            // Get EvoSuite compatible class path
            String cp = commands.getOptionValue(PROJECT_CP_OPT);
            List<String> classPathEntries = ClassPaths.getClassPathEntries(cp);

            String projectPrefix = commands.getOptionValue(PROJECT_PREFIX);

            // initialize analyzers
            // initial method call analyzer
            MethodCallAnalyzer methodCallAnalyzer = new MethodCallAnalyzer(classPathEntries,projectPrefix);
            // initial classes in the same hierarchy tree
            ClassesInSameHierarchyTreeAnalyzer classesInSameHierarchyTreeAnalyzer = new ClassesInSameHierarchyTreeAnalyzer(classPathEntries,projectPrefix);

            // Analyzing couplings according to the method calls
            methodCallAnalyzer.execute();
            // Analyzing coupling according in the super and sub classes
            classesInSameHierarchyTreeAnalyzer.execute();

            // ToDo: save lists into a csv file
            String outputFolder = commands.hasOption(OUTPUT_FOLDER) ? commands.getOptionValue(OUTPUT_FOLDER) :
                    "generated_results";


            } else {
                LOG.error("Project classpath and prefix should be passed as input. For more information. For more " +
                        "information, use -" + HELP_OPT);
            }
    }



    protected CommandLine parseCommands(String[] args, Options options) {
        CommandLineParser parser = new DefaultParser();
        CommandLine commands = null;
        try {
            commands = parser.parse(options, args);
        } catch(ParseException e) {
            LOG.error("Could not parse command line!", e);
            printHelpMessage(options);
            return null;
        }
        return commands;
    }

    private void printHelpMessage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar botsing_model_generator.jar -" + PROJECT_CP_OPT + " dep1.jar;dep2.jar  )",
                options);
    }

}
