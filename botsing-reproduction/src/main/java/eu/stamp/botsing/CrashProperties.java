
package eu.stamp.botsing;

import org.apache.commons.cli.CommandLine;
import org.evosuite.Properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Paths;


public class CrashProperties {

    private static CrashProperties instance = null;
    private StackTrace crash = StackTrace.getInstance();
    private String[] projectClassPaths;
    public static enum CrashReproductionSearchStrategy {
        Simple_GGA;

        private CrashReproductionSearchStrategy() {
        }
    }

    @Properties.Parameter(key = "CrashReproductionSearchStrategy", group = "Crash reproduction", description = "Which mode to use for crash reproduction")
    public static CrashProperties.CrashReproductionSearchStrategy STRATEGY = CrashProperties.CrashReproductionSearchStrategy.Simple_GGA;


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

    public String getStringValue(String property) throws IllegalAccessException, Properties.NoSuchParameterException {
        if (Properties.hasParameter(property)){
            return Properties.getStringValue(property);
        }else if (configFile.containsKey(property)){
            return configFile.getProperty(property);
        }
        return null;
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

    public Properties.StoppingCondition getStoppingCondition(){
        return Properties.STOPPING_CONDITION;
    }



}
