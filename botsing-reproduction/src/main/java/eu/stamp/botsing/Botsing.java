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
import org.apache.commons.cli.*;
import org.evosuite.Properties;
import org.evosuite.classpath.ClassPathHacker;
import org.evosuite.classpath.ClassPathHandler;
import org.evosuite.junit.writer.TestSuiteWriterUtils;


public class Botsing {

    public Object parseCommandLine(String[] args) {
        CommandLineParser parser = new GnuParser();
        // get permitted options
        CrashProperties crashProperties = CrashProperties.getInstance();
        Options options =  CommandLineParameters.getCommandLineOptions();

        try {
            // Parse commands according to the defined options
            CommandLine commands = parser.parse(options, args);
            java.util.Properties properties = commands.getOptionProperties("D");

            for(String property: properties.stringPropertyNames()){
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




        return CrashReproduction.execute();

    }
}
