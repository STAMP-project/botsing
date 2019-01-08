package eu.stamp.botsing.model.generation;

import be.yami.exception.SessionBuildException;
import com.google.gson.Gson;
import eu.stamp.botsing.model.generation.callsequence.CallSequenceCollector;
import eu.stamp.botsing.model.generation.callsequence.CallSequencesPoolManager;
import eu.stamp.botsing.model.generation.helper.LogReader;
import eu.stamp.botsing.model.generation.model.ModelGenerator;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    @SuppressWarnings("checkstyle:systemexit")
    public static void main(String[] args) {
        Main botsingModelGeneration = new Main();
        botsingModelGeneration.parseCommandLine(args);
        System.exit(0);
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

            String outputFolder = commands.hasOption(CommandLineParameters.OUTPUT_FOLDER)?commands.getOptionValue(CommandLineParameters.OUTPUT_FOLDER):"generated_results";

            ArrayList<String> involvedObejcts =  new ArrayList<>();
            if(commands.hasOption(CommandLineParameters.CRASHES)){
                Gson gson = new Gson();
                ArrayList<String> crashes = gson.fromJson(commands.getOptionValue(CommandLineParameters.CRASHES), ArrayList.class);
                LOG.info("scratch: {}",crashes);
                LogReader logReader= new LogReader(crashes);
                involvedObejcts = logReader.collectInvolvedObjects();
            }

            // set project prefix
            if (commands.hasOption(CommandLineParameters.PROJECT_PREFIX) ^ commands.hasOption(CommandLineParameters.PROJECT_PACKAGE)) {
                if(commands.hasOption(CommandLineParameters.PROJECT_PREFIX)){
                    callSequenceCollector.collect(commands.getOptionValue(CommandLineParameters.PROJECT_PREFIX), outputFolder, involvedObejcts, true);
                }else{
                    callSequenceCollector.collect(commands.getOptionValue(CommandLineParameters.PROJECT_PACKAGE), outputFolder,involvedObejcts, false);
                }

                // Here, we have the list of call sequences. We just need to pass it to the yami tool
                ModelGenerator modelGenerator =  new ModelGenerator();

                try {
                    modelGenerator.generate(CallSequencesPoolManager.getInstance().getPool(),Paths.get(outputFolder, "models").toString());
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (SessionBuildException e) {
                    e.printStackTrace();
                }

            }else{
                LOG.error("Either project prefix or project package name should be passed as an input. For more information -> help");
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
