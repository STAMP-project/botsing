package eu.stamp.botsing.model.generation;

import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;

import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.io.FileMatchers.anExistingDirectory;
import static org.junit.Assert.assertThat;

public class ModelGenerationTest {

    @Test
    public void testMain(){
        String user_dir = System.getProperty("user.dir"); // the current directory is the module <b>botsing-model-generation</b>
        File file = new File(user_dir);
        String base_dir = Paths.get(file.getParent(), "botsing-examples").toString();
        String  bin_path = Paths.get(base_dir, "src","main","resources","sample_dep").toString();
        String classPrefix = "org.tudelft";
        File outputDir = Paths.get(user_dir, "target", "generated-models").toFile();
        //run Botsing
        String[] prop = {
                "-projectCP",
                bin_path,
                "-projectPrefix",
                classPrefix,
                "-outDir",
                outputDir.getAbsolutePath()
        };
        Main.main(prop);

        // Check output directory
        assertThat(outputDir, anExistingDirectory());
        assertThat(outputDir.list(), arrayWithSize(greaterThan(0)));
    }

}
