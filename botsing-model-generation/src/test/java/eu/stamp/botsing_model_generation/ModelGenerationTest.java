package eu.stamp.botsing_model_generation;

import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;

public class ModelGenerationTest {

    @Test
    public void test1(){
        String user_dir = System.getProperty("user.dir"); // the current directory is the module <b>botsing-model-generation</b>
        File file = new File(user_dir);
        String base_dir = Paths.get(file.getParent(), "botsing-examples").toString();
        String  bin_path = Paths.get(base_dir, "src","main","resources","sample_dep").toString();
        String classPrefix = "org.tudelft";
        //run Botsing
        String[] prop = {
                "-projectCP",
                bin_path,
                "-projectPrefix",
                classPrefix

        };
        Main run = new Main();
        run.parseCommandLine(prop);
    }

}
