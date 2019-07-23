package eu.stamp.botsing.commons;

import com.google.common.collect.Lists;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class ClassPathsTest {

    private static final Logger LOG = LoggerFactory.getLogger(ClassPathsTest.class);

    @Rule
    public TestRule watcher = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            LOG.info(String.format("Starting test: %s()...",
                    description.getMethodName()));
        }
    };

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Test
    public void testOneEntry() throws IOException {
        File entry = tmpFolder.newFolder("oneentry");
        String rawEntry = entry.getAbsolutePath();
        List<String> classPathEntries = ClassPaths.getClassPathEntries(rawEntry);
        assertThat(classPathEntries, contains(entry.getAbsolutePath()));
    }

    @Test
    public void tesZeroEntry() {
        String rawEntry = "";
        List<String> classPathEntries = ClassPaths.getClassPathEntries(rawEntry);
        assertThat(classPathEntries, hasSize(0));
    }

    @Test
    public void testMultipleEntry() throws IOException {
        List<String> entries = Lists.newArrayList(tmpFolder.newFolder("oneentry").getAbsolutePath(),
                tmpFolder.newFile("twoentry.jar").getAbsolutePath(),
                tmpFolder.newFolder("threeentry").getAbsolutePath());
        String rawEntry = String.join(File.pathSeparator, entries);
        List<String> classPathEntries = ClassPaths.getClassPathEntries(rawEntry);
        assertThat(classPathEntries, containsInAnyOrder(entries.toArray(new String[entries.size()])));
    }

    @Test
    public void testMultipleEntryWithJars() throws IOException {
        List<String> entries = Lists.newArrayList(tmpFolder.newFolder("oneentry").getAbsolutePath(),
                tmpFolder.newFile("twoentry.jar").getAbsolutePath(),
                tmpFolder.newFolder("threeentry").getAbsolutePath());
        entries.set(1, entries.get(1).replace(File.separator + "twoentry.jar", "")); // remove jar file from the name
        String rawEntry = String.join(File.pathSeparator, entries);
        //Correct oracle
        entries.add(entries.get(1) + File.separator + "twoentry.jar"); // remove jar file from the name
        List<String> classPathEntries = ClassPaths.getClassPathEntries(rawEntry);
        assertThat(classPathEntries, containsInAnyOrder(entries.toArray(new String[entries.size()])));
    }

    @Test
    public void testUnexistingFile() throws IOException {
        List<String> entries = Lists.newArrayList(tmpFolder.newFolder("oneentry").getAbsolutePath(),
                tmpFolder.newFile("twoentry.jar").getAbsolutePath(),
                tmpFolder.newFolder("threeentry").getAbsolutePath());
        String nonExisting = tmpFolder.newFolder("fourthentry").getAbsolutePath() + File.separator + "nonexisting.jar";
        String rawEntry = String.join(File.pathSeparator, entries) + File.pathSeparator + nonExisting;
        List<String> classPathEntries = ClassPaths.getClassPathEntries(rawEntry);
        assertThat(classPathEntries, containsInAnyOrder(entries.toArray(new String[entries.size()])));
        assertThat(classPathEntries, not(contains(nonExisting)));
    }

}