package eu.stamp.coupling.analyze.local;

import eu.stamp.coupling.analyze.ClassCouplingAnalyzer;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Paths;

public class SimpleCouplingTest {

    public static String user_dir = System.getProperty("user.dir");
    public static String test_dir = Paths.get(user_dir,"src","test","java","eu","stamp","botsing","coupling","analyze","local").toString();
    public static String bin_path = Paths.get(test_dir,"lang-4","target","classes").toString();
    String classPrefix = "org.apache.commons.lang";

    @Test
    public void test() throws IOException {

        ClassCouplingAnalyzer main = new ClassCouplingAnalyzer();


        String[] prop = {
                "-project_prefix",
                classPrefix,
                "-project_cp",
                bin_path,
                "-target_class",
                "org.apache.commons.lang3.text.translate.LookupTranslator",
                "-out_dir",
                "lang4"

        };

        main.parseCommandLine(prop);

    }
}
