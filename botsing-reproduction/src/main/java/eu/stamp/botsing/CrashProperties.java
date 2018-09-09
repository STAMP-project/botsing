
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

import org.apache.commons.cli.CommandLine;
import org.evosuite.Properties;


import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Paths;


public class CrashProperties {

    private static CrashProperties instance = null;
    private StackTrace crash = StackTrace.getInstance();
    private String[] projectClassPaths;

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Parameter {
        String key();

        String group() default "Experimental";

        String description();
    }
    public static enum TestGenerationStrategy {
        Single_GA;

        private TestGenerationStrategy() {
        }
    }

    public static enum SearchAlgorithm{
        Single_Objective_GGA;
        private SearchAlgorithm(){}
    }



    @Properties.Parameter(key = "testGenerationStrategy", group = "Crash reproduction", description = "Which mode to use for crash reproduction")
    public static CrashProperties.TestGenerationStrategy testGenerationStrategy = CrashProperties.TestGenerationStrategy.Single_GA;


    @Properties.Parameter(key = "SearchAlgorithm", group = "Crash reproduction", description = "Which search algorithm to use for crash reproduction")
    public static CrashProperties.SearchAlgorithm searchAlgorithm = SearchAlgorithm.Single_Objective_GGA;


    /** The target frame in the crash stack trace */
    @Parameter(key = "max_target_injection_tries", group = "Runtime", description = "The maximum number of times the search tries to generate an individuals with the target method.")
    public static int max_target_injection_tries = 150;

    static java.util.Properties configFile = new java.util.Properties();
    private CrashProperties(){
        loadConfig();
        for (String property : configFile.stringPropertyNames()){
            try {
                if (Properties.hasParameter(property))
                    Properties.getInstance().setValue(property,configFile.getProperty(property));
            } catch (Properties.NoSuchParameterException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }



    }

    private void loadConfig(){
        try {
        InputStream inputstream = new FileInputStream(Paths.get(System.getProperty("user.dir"),"src","main","java","eu","stamp","botsing","config.properties").toString());
        configFile.load(inputstream);

        }catch(Exception eta){
            eta.printStackTrace();
        }
    }

    public static CrashProperties getInstance() {
        if (instance == null)
            instance = new CrashProperties();
        return instance;
    }

    public static String getStringValue(String property) throws IllegalAccessException, Properties.NoSuchParameterException {
        if (Properties.hasParameter(property)){
            return Properties.getStringValue(property);
        }else if (configFile.containsKey(property)){
            return configFile.getProperty(property);
        }
        return null;
    }



    public static int getIntValue(String property) throws IllegalAccessException, Properties.NoSuchParameterException {
            return Properties.getIntegerValue(property);
    }


    public static long getLongValue(String property) throws IllegalAccessException, Properties.NoSuchParameterException {
        return Properties.getLongValue(property);
    }


    public static Boolean getBooleanValue(String property){
        try{
        if (Properties.hasParameter(property)){
            return Properties.getBooleanValue(property);
        }else if (configFile.containsKey(property)){
            return Boolean.valueOf(configFile.getProperty(property));
        }
        } catch (Properties.NoSuchParameterException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }


    public void setupStackTrace(CommandLine command){
        java.util.Properties properties = command.getOptionProperties("D");
        crash.setup(properties.getProperty("crash_log"),Integer.parseInt(properties.getProperty("target_frame")));
    }

    public void setClasspath(String projectClassPath){
        projectClassPaths = projectClassPath.split(File.pathSeparator);
    }

    public String[] getProjectClassPaths() {
        return projectClassPaths;
    }

    public StackTrace getStackTrace(){
        return crash;
    }

    public static Properties.StoppingCondition getStoppingCondition(){
        return Properties.STOPPING_CONDITION;
    }
    public static Throwable getTargetException () {
        StackTraceElement [] stackArray = new StackTraceElement [StackTrace.getInstance().getNumberOfFrames()];
        stackArray = StackTrace.getInstance().getFrames().toArray(stackArray);
        Throwable targetException = new Exception();
        targetException.setStackTrace(stackArray);

        return targetException;
    }




        }
