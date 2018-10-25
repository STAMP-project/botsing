package eu.stamp.botsing_model_generation;

import eu.stamp.botsing_model_generation.generation.behavioral_model.ModelGeneration;
import eu.stamp.botsing_model_generation.generation.behavioral_model.model.Model;
import org.apache.commons.cli.*;
import org.evosuite.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.File;

import static eu.stamp.botsing_model_generation.CommandLineParameters.*;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) {
        Main modelGenerator = new Main();
        modelGenerator.parseCommandLine(args);


    }


    public  void parseCommandLine(String[] args) {
        Options options = CommandLineParameters.getCommandLineOptions();
        CommandLine commands = parseCommands(args, options);
        if (commands.hasOption(HELP_OPT)){
            printHelpMessage(options);
        }else if(commands.hasOption(PROJECT_CP_OPT)){ // Generate the model for passed cps
            ModelGeneration modelGeneration;
            File file = new File(commands.getOptionValue(PROJECT_CP_OPT));
            if(file.isDirectory()) {
                File[] jarsFiles = file.listFiles((File f) -> f.isFile() && f.getName().endsWith(".jar"));
                String[] jarsCp = new String[jarsFiles.length];
                for (int i = 0; i < jarsCp.length; i++) {
                    jarsCp[i] = jarsFiles[i].getAbsolutePath();
                }
                modelGeneration =  new ModelGeneration(jarsCp);
            }else{
                modelGeneration =  new ModelGeneration(commands.getOptionValue(PROJECT_CP_OPT));
            }

            // set project prefix
            if (commands.hasOption(PROJECT_PREFIX)) {
                Properties.TARGET_CLASS_PREFIX = commands.getOptionValue(PROJECT_PREFIX);
                Model result = modelGeneration.generate();
                if(result == null){
                    LOG.error("The generated model is NULL!");
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
