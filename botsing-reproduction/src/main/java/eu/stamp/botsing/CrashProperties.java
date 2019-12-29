
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
import java.util.ArrayList;
import java.util.List;


public class CrashProperties {

    private static final Logger LOG = LoggerFactory.getLogger(CrashProperties.class);
    public static final String CONFIG_PROPERTIES_FILE_NAME = "config.properties";

    private static CrashProperties instance = null;
    private List<StackTrace> crashes = new ArrayList<>();
    private String[] projectClassPaths;


    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Parameter {
        String key();

        String group() default "Experimental";

        String description();
    }

    public enum TestGenerationStrategy {
        Single_GA,
        Multi_GA;

        TestGenerationStrategy() {
        }
    }

    public enum FitnessFunction {
        WeightedSum,
        SimpleSum,
        TestLen,
        IntegrationIndexedAccess,
        IntegrationSingleObjective;
        FitnessFunction() {
        }
    }

    public enum SearchAlgorithm {
        Single_Objective_GGA,
        Guided_MOSA,
        NSGA_II,
        SPEA2,
        MOEAD,
        DynaMOSA;

        SearchAlgorithm() {
        }
    }


//    @Properties.Parameter(key = "testGenerationStrategy", group = "Crash reproduction", description = "Which mode to use for crash reproduction")
//    public static CrashProperties.TestGenerationStrategy testGenerationStrategy = CrashProperties.TestGenerationStrategy.Single_GA;


    @Properties.Parameter(key = "SearchAlgorithm", group = "Crash reproduction", description = "Which search algorithm to use for crash reproduction")
    public static CrashProperties.SearchAlgorithm searchAlgorithm = SearchAlgorithm.MOEAD;


    @Properties.Parameter(key = "FitnessFunctions", group = "Crash reproduction", description = "Which fitness function should be used for the GGA")
    public static CrashProperties.FitnessFunction[] fitnessFunctions = {FitnessFunction.WeightedSum, FitnessFunction.TestLen};



    /**
     * The target frame in the crash stack trace
     */
    @Parameter(key = "max_target_injection_tries", group = "Runtime", description = "The maximum number of times the search tries to generate an individuals with the target method.")
    public static int max_target_injection_tries = 150;


    @Parameter(key = "integration_testing", group = "Crash reproduction", description = "Use integration testing for reproduce the crash.")
    public static boolean integrationTesting = false;
    @Parameter(key = "line_estimation", group = "Crash reproduction", description = "Detect Missing lines in the stack trace")
    public static boolean lineEstimation = true;


    @Parameter(key = "io_diversity", group = "Crash reproduction", description = "Enables I/O diversity as extra goals to MOSA")
    public static boolean IODiversity = false;

    @Parameter(key = "neighborhood_selection_probability", group = "MOEAD", description = "neighborhood Selection Probability")
    public static double neighborhoodSelectionProbability = 0.2;

    @Parameter(key = "maximum_number_of_replaced_solutions", group = "MOEAD", description = "Maximum Number Of Replaced Solutions")
    public static int maximumNumberOfReplacedSolutions = 100;

    @Parameter(key = "ideal_point_shift", group = "MOEAD", description = "Shift objectives of ideal point to a better situation (to avoid local optimum)")
    public static double idealPointShift = 0.2;


    public enum DistanceCalculator {
        WS, // Weighted Sum
        TE, // Tchebycheff (default)
        PBI, // Penalty-based boundary intersection
    }

    @Parameter(key = "distance_calculator", group = "MOEAD", description = "neighborhood Selection Probability")
    public static CrashProperties.DistanceCalculator distanceCalculator = DistanceCalculator.TE;

    static java.util.Properties configFile = new java.util.Properties();

    private CrashProperties() {
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

    public static CrashProperties getInstance() {
        if (instance == null) {
            instance = new CrashProperties();
        }
        return instance;
    }

    public String getStringValue(String property) throws IllegalAccessException, Properties.NoSuchParameterException {
        if (Properties.hasParameter(property)) {
            return Properties.getStringValue(property);
        } else if (configFile.containsKey(property)) {
            return configFile.getProperty(property);
        }
        return null;
    }


    public int getIntValue(String property) throws IllegalAccessException, Properties.NoSuchParameterException {
        return Properties.getIntegerValue(property);
    }


    public long getLongValue(String property) throws IllegalAccessException, Properties.NoSuchParameterException {
        return Properties.getLongValue(property);
    }

    /**
     * Returns the value of the given property or null if the property could not be found.
     * @param property the property to get the value of.
     * @return The value of the given property or null if the property could not be found.
     * @throws IllegalStateException If the property could not be accessed.
     */
    public Boolean getBooleanValue(String property) {
        try {
            if (Properties.hasParameter(property)) {
                return Properties.getBooleanValue(property);
            } else if (configFile.containsKey(property)) {
                return Boolean.valueOf(configFile.getProperty(property));
            }
        } catch (Properties.NoSuchParameterException e) {
            LOG.debug("Property {} not found!", property, e);
        } catch (IllegalAccessException e) {
            LOG.debug("Illegal access for property {}!", property, e);
            throw new IllegalStateException("Illegal access for property " + property + "!", e);
        }
        return null;
    }

    public void setupStackTrace(String stacktraceFile, int targetFrame) {
        crashes.add(new StackTrace());
        crashes.get(crashes.size() - 1).setup(stacktraceFile, targetFrame);
    }

    public void setupStackTrace(StackTrace crash) {
        this.crashes.add(crash);
    }

    public void setClasspath(String projectClassPath) {
        projectClassPaths = projectClassPath.split(File.pathSeparator);
    }

    public void setClasspath(String[] projectClassPath) {
        projectClassPaths = projectClassPath;
    }


    public String[] getProjectClassPaths() {
        return projectClassPaths;
    }

    public StackTrace getStackTrace(int index) {
        if(crashes.size() <= index){
            throw new IndexOutOfBoundsException("The given index for crashes is out of bounds");
        }

        return crashes.get(index);
    }

    public Properties.StoppingCondition getStoppingCondition() {
        return Properties.STOPPING_CONDITION;
    }

    public Throwable getTargetException(int crashIndex) {
        if (crashes.size() <= crashIndex) {
            throw new IndexOutOfBoundsException("The given index for crashes is out of bounds");
        }


        StackTraceElement[] stackArray = new StackTraceElement[crashes.get(crashIndex).getNumberOfFrames()];
        stackArray = crashes.get(crashIndex).getFrames().toArray(stackArray);
        Throwable targetException = new Exception();
        targetException.setStackTrace(stackArray);
        return targetException;
    }


    public void resetStackTrace(int index) {
        if(crashes.size() <= index){
            throw new IndexOutOfBoundsException("The given index for crashes is out of bounds");
        }

        crashes.set(index,new StackTrace());
    }

    public void clearStackTraceList(){
        crashes.clear();
    }


    public List<String> getTargetClasses(int index) {

        if(crashes.size() <= index){
            throw new IndexOutOfBoundsException("The given index for crashes is out of bounds");
        }

        return crashes.get(index).getTargetClasses();
    }

    public int getCrashesSize(){
        return crashes.size();
    }

}
