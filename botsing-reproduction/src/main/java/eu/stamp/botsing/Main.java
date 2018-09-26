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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static int target_frame_level = 2;

    public static String user_dir = System.getProperty("user.dir");
    public static String log_dir = Paths.get(user_dir, "sample.log").toString();
    public static String  bin_path = Paths.get(user_dir, "sample_dep").toString();
    private static String separator = System.getProperty("path.separator");
    public static void main(String[] args) {
        Botsing botsing = new Botsing();
        String[] prop = {
                "-Dcrash_log="+log_dir,
                "-Dtarget_frame="+target_frame_level,
                "-projectCP",
                getListOfDeps(),
        };
        botsing.parseCommandLine(prop);
    }


    public static String getListOfDeps(){
        String dependencies = "";
        File depFolder = new File(bin_path);
        File[] listOfFilesInSourceFolder = depFolder.listFiles();
        for(int i = 0; i < listOfFilesInSourceFolder.length; i++) {
            if (listOfFilesInSourceFolder[i].getName().charAt(0) != '.') {
                Path depPath = Paths.get(depFolder.getAbsolutePath(), listOfFilesInSourceFolder[i].getName());
                dependencies += (depPath.toString() + separator);
            }
        }
        return dependencies;
    }
}
