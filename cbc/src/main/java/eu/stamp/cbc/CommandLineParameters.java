package eu.stamp.cbc;

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

    public static final String D_OPT = "D";
    public static final String PROJECT_CP_OPT = "project_cp";
    public static final String TEST_SUITE = "test_suite";
    public static final String CALLER = "caller";
    public static final String CALLEE = "callee";
    public static final String HELP_OPT = "help";

    public static Options getCommandLineOptions() {
        Options options = new Options();
        // Properties
        options.addOption(Option.builder(D_OPT)
                .numberOfArgs(2)
                .argName("property=value")
                .valueSeparator()
                .desc("use value for given property")
                .build());
        // Classpath
        options.addOption(Option.builder(PROJECT_CP_OPT)
                .hasArg()
                .desc("classpath of the project under test and all its dependencies")
                .build());
        // caller
        options.addOption(Option.builder(CALLER)
                .hasArg()
                .desc("caller class")
                .build());
        // callee
        options.addOption(Option.builder(CALLEE)
                .hasArg()
                .desc("callee class")
                .build());
        // Tests directory
        options.addOption(Option.builder(TEST_SUITE)
                .hasArg()
                .desc("Cling test suite for calculating CBC")
                .build());


        // Help message
        options.addOption(Option.builder(HELP_OPT)
                .desc("Prints this help message.")
                .build());
        return options;
    }

}
