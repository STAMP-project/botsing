package eu.stamp.botsing;

/*-
 * #%L
 * botsing-reproduction
 * %%
 * Copyright (C) 2017 - 2018 eu.stamp-project
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import eu.stamp.botsing.reproduction.CrashReproduction;

import static eu.stamp.botsing.CommandLineParameters.*;

import org.apache.commons.cli.*;
import org.evosuite.Properties;
import org.evosuite.TestGenerationContext;
import org.evosuite.classpath.ClassPathHacker;
import org.evosuite.classpath.ClassPathHandler;
import org.evosuite.junit.writer.TestSuiteWriterUtils;
import org.evosuite.result.TestGenerationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;


public class Botsing {

    private static final Logger LOG = LoggerFactory.getLogger(Botsing.class);

    public List<TestGenerationResult> parseCommandLine(String[] args) {
        // Get default properties
        CrashProperties crashProperties = CrashProperties.getInstance();

        // Parse commands according to the defined options
        Options options = CommandLineParameters.getCommandLineOptions();
        CommandLine commands = parseCommands(args, options);

        // If help option is provided
        if (commands.hasOption(HELP_OPT)) {
            printHelpMessage(options);
        } else if(!(commands.hasOption(PROJECT_CP_OPT) && commands.hasOption(CRASH_LOG_OPT) && commands.hasOption(TARGET_FRAME_OPT))) { // Check the required options are there
            LOG.error("A mandatory option -{} -{} -{} is missing!", PROJECT_CP_OPT, CRASH_LOG_OPT, TARGET_FRAME_OPT);
            printHelpMessage(options);
        } else {// Otherwise, proceed to crash reproduction
            java.util.Properties properties = commands.getOptionProperties(D_OPT);
            updateProperties(properties);
            setupStackTrace(crashProperties, commands);
            setupProjectClasspath(crashProperties, commands);

            if(commands.hasOption(INTEGRATION_TESTING)){
                crashProperties.integrationTesting = true;
            }

            if(commands.hasOption(MODEL_PATH_OPT)){
                setupModelSeedingRelatedProperties(commands);
            }
            return CrashReproduction.execute();
        }
        return null;

    }

    private void setupModelSeedingRelatedProperties( CommandLine commands) {
        for (StackTraceElement ste: CrashProperties.getInstance().getStackTrace().getAllFrames()){
            try {
                Class.forName(ste.getClassName(), true,
                        TestGenerationContext.getInstance().getClassLoaderForSUT());
            } catch (Exception| Error e) {
                e.printStackTrace();
            }
        }

        String modelPath = commands.getOptionValue(MODEL_PATH_OPT);
        Properties.MODEL_PATH = modelPath;
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

    protected void updateProperties(java.util.Properties properties){
        for (String property : properties.stringPropertyNames()) {
            if (Properties.hasParameter(property)) {
                try {
                    Properties.getInstance().setValue(property, properties.getProperty(property));
                } catch (Properties.NoSuchParameterException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setupStackTrace(CrashProperties crashProperties, CommandLine commands){
        // Setup given stack trace
        crashProperties.setupStackTrace(commands.getOptionValue(CRASH_LOG_OPT),
                Integer.parseInt(commands.getOptionValue(TARGET_FRAME_OPT)));
    }

    protected void setupProjectClasspath(CrashProperties crashProperties, CommandLine commands){
        // Setup Project's class path
        String cp = commands.getOptionValue(PROJECT_CP_OPT);
        File file = new File(cp);
        // If the file is a directory, get all the jar files in that directory.
        if(file.isDirectory()){
            File[] jarsFiles = file.listFiles((File f) -> f.isFile() && f.getName().endsWith(".jar"));
            String[] jarsCp = new String[jarsFiles.length];
            for(int i = 0 ; i < jarsCp.length ; i++){
                jarsCp[i] = jarsFiles[i].getAbsolutePath();
            }
            crashProperties.setClasspath(jarsCp);
        } else {
            crashProperties.setClasspath(cp);
        }
        ClassPathHandler.getInstance().changeTargetClassPath(crashProperties.getProjectClassPaths());

        // locate Tool jar
        if (TestSuiteWriterUtils.needToUseAgent() && Properties.JUNIT_CHECK) {
            ClassPathHacker.initializeToolJar();
        }

        // Adding the target project classpath entries.
        for (String entry : ClassPathHandler.getInstance().getTargetProjectClasspath().split(File.pathSeparator)) {
            try {
                ClassPathHacker.addFile(entry);
            } catch (IOException e) {
                LOG.info("* Error while adding classpath entry: " + entry);
            }
        }
    }

    private void printHelpMessage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar botsing.jar -crash_log stacktrace.log -target_frame 2 -projectCP dep1.jar;dep2.jar  )", options);
    }

    @SuppressWarnings("checkstyle:systemexit")
    public static void main(String[] args) {
        Botsing bot = new Botsing();
        bot.parseCommandLine(args);
        System.exit(0);
    }
}
