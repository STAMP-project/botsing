package eu.stamp.botsing.commons;

import org.apache.commons.cli.*;
import org.evosuite.Properties;
import org.evosuite.classpath.ClassPathHacker;
import org.evosuite.classpath.ClassPathHandler;
import org.evosuite.junit.writer.TestSuiteWriterUtils;
import org.evosuite.setup.DependencyAnalysis;
import org.evosuite.testcase.execution.ExecutionTrace;
import org.evosuite.testcase.execution.ExecutionTracer;
import org.evosuite.testcase.execution.reset.ClassReInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class SetupUtility {

    private static final Logger LOG = LoggerFactory.getLogger(SetupUtility.class);

    public static void printHelpMessage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar botsing.jar -crash_log stacktrace.log -target_frame 2 -project_cp dep1.jar;dep2.jar  )", options);
    }


    public static CommandLine parseCommands(String[] args, Options options){
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



    public static void updateEvoSuiteProperties(java.util.Properties properties){
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

    public static String[] getCompatibleCP(String cp){
        // Get EvoSuite compatible class path
        List<String> classPathEntries = ClassPaths.getClassPathEntries(cp);
        return classPathEntries.toArray(new String[classPathEntries.size()]);
    }


    public static void setupProjectClasspath(String[] projectCP){

        ClassPathHandler.getInstance().changeTargetClassPath(projectCP);

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



    public static void configureClassReInitializer() {
        ExecutionTrace execTrace = ExecutionTracer.getExecutionTracer().getTrace();
        final List<String> initializedClasses = execTrace.getInitializedClasses();
        ClassReInitializer.getInstance().addInitializedClasses(initializedClasses);
        ClassReInitializer.getInstance().setReInitializeAllClasses(Properties.RESET_ALL_CLASSES_DURING_TEST_GENERATION);
    }

    public static void analyzeClassDependencies(String  className) {
        String cp = ClassPathHandler.getInstance().getTargetProjectClasspath();
        List<String> cpList = Arrays.asList(cp.split(File.pathSeparator));
        Properties.TARGET_CLASS=className;
        try {
            LOG.info("Starting the dependency analysis. The number of detected jar files is {}.",cpList.size());
            DependencyAnalysis.analyzeClass(className,Arrays.asList(cp.split(File.pathSeparator)));
            LOG.info("Analysing dependencies done!");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
