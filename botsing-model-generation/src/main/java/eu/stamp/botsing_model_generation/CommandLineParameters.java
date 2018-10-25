package eu.stamp.botsing_model_generation;

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
import org.apache.commons.cli.Options;

public class CommandLineParameters {

    public static final String PROJECT_CP_OPT = "projectCP";
    public static final String HELP_OPT = "help";
    public static final String PROJECT_PREFIX = "projectPrefix";

    public static Options getCommandLineOptions() {
        Options options = new Options();
        // Classpath
        options.addOption(Option.builder(PROJECT_CP_OPT)
                .hasArg()
                .desc("Classpath of the project under test and all its dependencies")
                .build());
        // Help message
        options.addOption(Option.builder(HELP_OPT)
                .desc("Prints this help message.")
                .build());

        options.addOption(Option.builder(PROJECT_PREFIX)
                .hasArg()
                .desc("Prefix of the classes that we want to use for manual/dynamic analysis")
                .build());

        return options;
    }

}
