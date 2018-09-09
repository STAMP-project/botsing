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

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

public class CommandLineParameters {

    public static Options getCommandLineOptions() {
        Options options = new Options();
        // define options
        @SuppressWarnings("static-access")
        Option property = OptionBuilder.withArgName("property=value").hasArgs(2).withValueSeparator().withDescription("use value for given property").create("D");
        Option projectCP = new Option("projectCP", true,
                "classpath of the project under test and all its dependencies");
        Option target_frame = new Option("target_frame",true, "Level of the target frame");
        Option crash_log = new Option("crash_log",true, "Directory of the given stack trace");

        options.addOption(property);
        options.addOption(projectCP);
        options.addOption(target_frame);
        options.addOption(crash_log);

        return options;
    }

}
