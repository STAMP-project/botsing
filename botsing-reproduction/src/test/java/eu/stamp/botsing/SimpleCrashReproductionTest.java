package eu.stamp.botsing;

import ch.qos.logback.classic.Level;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Ignore
public class SimpleCrashReproductionTest {

    public static int target_frame_level = 3;

    public static String user_dir = System.getProperty("user.dir");
    public static String test_dir = Paths.get(user_dir,"src","test","java","eu","stamp","botsing").toString();
    public static String log_dir = Paths.get(test_dir, "sample.log").toString();
    public static String  bin_path = Paths.get(test_dir, "sample_dep").toString();
    private static String separator = System.getProperty("path.separator");

    @Test
    public void runtest(){
        setLoggingLevel(Level.INFO);

        BotSing botsing = new BotSing();
        String[] prop = {
                "-Dcrash_log="+log_dir,
                "-Dtarget_frame="+target_frame_level,
                "-projectCP",
                getListOfDeps(),
        };
        botsing.parseCommandLine(prop);
    }

    public static void setLoggingLevel(ch.qos.logback.classic.Level level) {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        root.setLevel(level);
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
