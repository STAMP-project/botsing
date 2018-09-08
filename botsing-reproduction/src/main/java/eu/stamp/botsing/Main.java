package eu.stamp.botsing;

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
        BotSing botsing = new BotSing();
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
