package eu.stamp.botsing.model.generation.testusage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Paths;

public class TestUsagePoolManager extends TestUsagePool {

    private static final Logger LOG = LoggerFactory.getLogger(TestUsagePoolManager.class);

    private static TestUsagePoolManager instance = null;

    private TestUsagePoolManager() {
    }

    public static TestUsagePoolManager getInstance() {
        if(instance == null) {
            instance = new TestUsagePoolManager();
        }
        return instance;
    }


    public void savingTestsUsages(String outputPath) {
        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(this.pool);
        File outDirectory = new File(outputPath);
        if(!outDirectory.exists()) {
            outDirectory.mkdirs();
        }
        try(PrintWriter out = new PrintWriter(Paths.get(outputPath, "tests.xml").toString())) {
            out.println(json);
            LOG.debug("The saved test usage is: {}", json);
        } catch(FileNotFoundException e) {
            LOG.error("The output directory for carved tests is not valid.");
        }
    }
}
