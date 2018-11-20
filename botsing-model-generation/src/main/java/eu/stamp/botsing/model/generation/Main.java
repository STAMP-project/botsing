package eu.stamp.botsing.model.generation;

import be.yami.exception.SessionBuildException;
import eu.stamp.botsing.model.generation.callsequence.CallSequenceCollector;
import eu.stamp.botsing.model.generation.callsequence.CallSequencesPoolManager;
import eu.stamp.botsing.model.generation.model.ModelGenerator;
import org.apache.commons.cli.*;
import org.evosuite.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.File;
import java.io.IOException;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        Main botsingModelGeneration = new Main();
        botsingModelGeneration.parseCommandLine(args);
    }


    public  void parseCommandLine(String[] args) {
        Options options = CommandLineParameters.getCommandLineOptions();
        CommandLine commands = parseCommands(args, options);
        if (commands.hasOption(CommandLineParameters.HELP_OPT)){
            printHelpMessage(options);
        }else if(commands.hasOption(CommandLineParameters.PROJECT_CP_OPT)){ // Generate the model for passed cps
            CallSequenceCollector callSequenceCollector;
            File file = new File(commands.getOptionValue(CommandLineParameters.PROJECT_CP_OPT));
            if(file.isDirectory()) {
                File[] jarsFiles = file.listFiles((File f) -> f.isFile() && f.getName().endsWith(".jar"));
                String[] jarsCp = new String[jarsFiles.length];
                for (int i = 0; i < jarsCp.length; i++) {
                    jarsCp[i] = jarsFiles[i].getAbsolutePath();
                }
                callSequenceCollector =  new CallSequenceCollector(jarsCp);
            }else{
                callSequenceCollector =  new CallSequenceCollector(commands.getOptionValue(CommandLineParameters.PROJECT_CP_OPT));
            }

            // set project prefix
            if (commands.hasOption(CommandLineParameters.PROJECT_PREFIX)) {
                Properties.TARGET_CLASS_PREFIX = commands.getOptionValue(CommandLineParameters.PROJECT_PREFIX);
                callSequenceCollector.collect();
                // Here, we have the list of call sequences. We just need to pass it to the yami tool
                ModelGenerator modelGenerator =  new ModelGenerator();
                String outputFolder = commands.hasOption(CommandLineParameters.OUTPUT_FOLDER)?commands.getOptionValue(CommandLineParameters.OUTPUT_FOLDER):"generated_model";
                try {
                    modelGenerator.generate(CallSequencesPoolManager.getInstance().getPool(),outputFolder);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (SessionBuildException e) {
                    e.printStackTrace();
                }

            }else{
                LOG.error("Project prefix should be passed as an input. For more information -> help");
            }
        }else{
            LOG.error("Project classpath should be passed as an input. For more information -> help");
        }

    }


    protected CommandLine parseCommands(String[] args, Options options){
        CommandLineParser parser = new DefaultParser();
        CommandLine commands = null;
        try {
            commands = parser.parse(options, args);
        } catch (ParseException e) {
            LOG.error("Could not parse command line!", e);
            printHelpMessage(options);
            return null;
        }
        return commands;
    }

    private void printHelpMessage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar botsing_model_generator.jar -projectCP dep1.jar;dep2.jar  )", options);
    }
}
