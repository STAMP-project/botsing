package eu.stamp.botsing.model.generation;

import be.yami.exception.SessionBuildException;
import com.google.gson.Gson;
import eu.stamp.botsing.commons.ClassPaths;
import eu.stamp.botsing.model.generation.callsequence.CallSequenceCollector;
import eu.stamp.botsing.model.generation.callsequence.CallSequencesPoolManager;
import eu.stamp.botsing.model.generation.helper.LogReader;
import eu.stamp.botsing.model.generation.model.ModelGenerator;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static eu.stamp.botsing.model.generation.CommandLineParameters.*;

public class ModelGeneration {

    private static final Logger LOG = LoggerFactory.getLogger(ModelGeneration.class);

    @SuppressWarnings("checkstyle:systemexit")
    public static void main(String[] args) {
        ModelGeneration botsingModelGeneration = new ModelGeneration();
        try {
            botsingModelGeneration.parseCommandLine(args);
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
        } else if(commands.hasOption(PROJECT_CP_OPT)) { // Generate the model for passed cps
            // Get EvoSuite compatible class path
            String cp = commands.getOptionValue(PROJECT_CP_OPT);
            List<String> classPathEntries = ClassPaths.getClassPathEntries(cp);

            CallSequenceCollector callSequenceCollector =
                    new CallSequenceCollector(classPathEntries.toArray(new String[classPathEntries.size()]));

            String outputFolder = commands.hasOption(OUTPUT_FOLDER) ? commands.getOptionValue(OUTPUT_FOLDER) :
                    "generated_results";

            List<String> involvedObejcts = new ArrayList<>();
            if(commands.hasOption(CRASHES)) {
                Gson gson = new Gson();
                ArrayList<String> crashes = gson.fromJson(commands.getOptionValue(CRASHES), ArrayList.class);
                LogReader logReader = new LogReader(crashes);
                involvedObejcts = logReader.collectInvolvedObjects();
            } else if(commands.hasOption(LIST_CLASSES)) {
                File listClasses = new File(commands.getOptionValue(LIST_CLASSES));
                involvedObejcts = Files.readAllLines(listClasses.toPath());
            }

            // set project prefix
            if(commands.hasOption(PROJECT_PREFIX) ^ commands.hasOption(PROJECT_PACKAGE)) {
                if(commands.hasOption(PROJECT_PREFIX)) {
                    callSequenceCollector.collect(commands.getOptionValue(PROJECT_PREFIX), outputFolder,
                            involvedObejcts, true);
                } else {
                    callSequenceCollector.collect(commands.getOptionValue(PROJECT_PACKAGE), outputFolder,
                            involvedObejcts, false);
                }

                // Here, we have the list of call sequences. We just need to pass it to the yami tool
                ModelGenerator modelGenerator = new ModelGenerator();

                try {
                    modelGenerator.generate(CallSequencesPoolManager.getInstance().getPool(), Paths.get(outputFolder,
                            "models").toString());
                } catch(SessionBuildException e) {
                    LOG.error("Exception while building a session to learn the model!", e);
                }

            } else {
                LOG.error("Either project prefix or project package name should be passed as an input. For more " +
                        "information, use -" + HELP_OPT);
            }
        } else {
            LOG.error("Project classpath should be passed as an input. For more information -> help");
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
