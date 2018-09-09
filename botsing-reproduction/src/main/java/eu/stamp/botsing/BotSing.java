package eu.stamp.botsing;

import eu.stamp.botsing.reproduction.CrashReproduction;
import org.apache.commons.cli.*;
import org.evosuite.Properties;
import org.evosuite.classpath.ClassPathHacker;
import org.evosuite.classpath.ClassPathHandler;
import org.evosuite.junit.writer.TestSuiteWriterUtils;
import org.evosuite.result.TestGenerationResult;

import java.util.List;

public class BotSing {

    public Object parseCommandLine(String[] args) {
        CommandLineParser parser = new GnuParser();
        // get permitted options
        CrashProperties crashProperties = CrashProperties.getInstance();
        Options options =  CommandLineParameters.getCommandLineOptions();

        try {
            // Parse commands according to the defined options
            CommandLine commands = parser.parse(options, args);

            // Setup given stack trace
            crashProperties.setupStackTrace(commands);

            // Setup Project's class path
            if (commands.hasOption("projectCP")) {
                crashProperties.setClasspath(commands.getOptionValue("projectCP"));
                ClassPathHandler.getInstance().changeTargetClassPath(crashProperties.getProjectClassPaths());
            }

            // locate Tool jar
            if (TestSuiteWriterUtils.needToUseAgent() && Properties.JUNIT_CHECK) {
                ClassPathHacker.initializeToolJar();
            }



        } catch (ParseException e) {
            e.printStackTrace();
        }




        List<List<TestGenerationResult>> result = CrashReproduction.execute();
        return result;

    }
}
