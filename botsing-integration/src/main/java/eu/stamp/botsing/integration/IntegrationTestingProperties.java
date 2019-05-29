package eu.stamp.botsing.integration;

import org.evosuite.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class IntegrationTestingProperties {

    private static final Logger LOG = LoggerFactory.getLogger(IntegrationTestingProperties.class);
    public static final String CONFIG_PROPERTIES_FILE_NAME = "config.properties";

    private static IntegrationTestingProperties instance = null;
    private String[] projectClassPaths;


    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Parameter {
        String key();

        String group() default "Experimental";

        String description();
    }


    public enum FitnessFunction {
        Integration;
        FitnessFunction() {
        }
    }

    public enum SearchAlgorithm {
        MOSA;
        SearchAlgorithm() {
        }
    }


    @Properties.Parameter(key = "SearchAlgorithm", group = "search algorithm", description = "Which search algorithm should be used")
    public static IntegrationTestingProperties.SearchAlgorithm searchAlgorithm = SearchAlgorithm.MOSA;


    @Properties.Parameter(key = "FitnessFunctions", group = "search algorithm", description = "Which fitness function(s) should be used for the search process")
    public static IntegrationTestingProperties.FitnessFunction[] fitnessFunctions = {FitnessFunction.Integration};


    static java.util.Properties configFile = new java.util.Properties();

    private IntegrationTestingProperties() {
        loadConfig();
        for (String property : configFile.stringPropertyNames()) {
            try {
                if (Properties.hasParameter(property)) {
                    Properties.getInstance().setValue(property, configFile.getProperty(property));
                }
            } catch (Properties.NoSuchParameterException e) {
                LOG.debug("Property {} not found!", property, e);
                throw new IllegalArgumentException("Property " + property + " not found!", e);
            } catch (IllegalAccessException e) {
                LOG.debug("Illegal access for property {}!", property, e);
                throw new IllegalArgumentException("Illegal access for property " + property + "!", e);
            }
        }


    }

    private void loadConfig() {
        try {
            InputStream inputstream = getClass().getClassLoader().getResourceAsStream(CONFIG_PROPERTIES_FILE_NAME);
            configFile.load(inputstream);
        } catch (FileNotFoundException eta) {
            LOG.error("Default config.properties file not found in the resources of the jar file!");
            throw new IllegalStateException("Default config.properties file not found in the resources of the jar file!");
        } catch (IOException e) {
            LOG.error("Exception while reading default config.properties from the resources of the jar file!");
            throw new IllegalStateException("Exception while reading default config.properties from the resources of the jar file!");
        }
    }

    public static IntegrationTestingProperties getInstance() {
        if (instance == null) {
            instance = new IntegrationTestingProperties();
        }
        return instance;
    }

//    public String getStringValue(String property) throws IllegalAccessException, Properties.NoSuchParameterException {
//        if (Properties.hasParameter(property)) {
//            return Properties.getStringValue(property);
//        } else if (configFile.containsKey(property)) {
//            return configFile.getProperty(property);
//        }
//        return null;
//    }
//
//
//    public int getIntValue(String property) throws IllegalAccessException, Properties.NoSuchParameterException {
//        return Properties.getIntegerValue(property);
//    }
//
//
//    public long getLongValue(String property) throws IllegalAccessException, Properties.NoSuchParameterException {
//        return Properties.getLongValue(property);
//    }
//
//    /**
//     * Returns the value of the given property or null if the property could not be found.
//     * @param property the property to get the value of.
//     * @return The value of the given property or null if the property could not be found.
//     * @throws IllegalStateException If the property could not be accessed.
//     */
//    public Boolean getBooleanValue(String property) {
//        try {
//            if (Properties.hasParameter(property)) {
//                return Properties.getBooleanValue(property);
//            } else if (configFile.containsKey(property)) {
//                return Boolean.valueOf(configFile.getProperty(property));
//            }
//        } catch (Properties.NoSuchParameterException e) {
//            LOG.debug("Property {} not found!", property, e);
//        } catch (IllegalAccessException e) {
//            LOG.debug("Illegal access for property {}!", property, e);
//            throw new IllegalStateException("Illegal access for property " + property + "!", e);
//        }
//        return null;
//    }

    public void setClasspath(String projectClassPath) {
        projectClassPaths = projectClassPath.split(File.pathSeparator);
    }

    public void setClasspath(String[] projectClassPath) {
        projectClassPaths = projectClassPath;
    }


    public String[] getProjectClassPaths() {
        return projectClassPaths;
    }


    public Properties.StoppingCondition getStoppingCondition() {
        return Properties.STOPPING_CONDITION;
    }


}
