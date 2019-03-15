package eu.stamp.botsing;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlattenTest {
	private static final Logger LOG = LoggerFactory.getLogger(FlattenTest.class);

    @Rule
    public TestRule watcher = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            LOG.info(String.format("Starting test: %s()...",
                    description.getMethodName()));
        }
    };

    private static final String CHAINED_ST = "nested_st.txt";
    private static final String CHAINED_ST2 = "nested_st2.txt";
    private static final String PACKAGE = "example.Service.serviceMethod.*";

    public static File openFile(String localName) throws Exception {
        return new File(FlattenTest.class.getResource(localName).toURI());
    }

    public static List<String> lines(String name) throws Exception {
        File log = openFile(name);
        List<String> lines = Main.fileToLines(log);
        return lines;
    }

    @Test
    public void testPartitioning() throws Exception {
        List<String> lines = lines(CHAINED_ST2);
        Assert.assertEquals("File not read correctly", 11, lines.size());
        List<List<String>> chuncks = StackFlatten.get().splitLines(lines, StackFlatten.CAUSED_BY_PREFIX);
        Assert.assertEquals("Chuncks not created correctly, some missed", 3, chuncks.size());
        Assert.assertEquals("Chuncks not created correctly", 4, chuncks.get(2).size());
        List<String> orderedLines = StackFlatten.get().flatten(chuncks, PACKAGE);
        Assert.assertEquals("Chuncks not reordered correctly", 3, orderedLines.size());
    }


    @Test
    public void testComplexPartitioning() throws Exception {
        List<String> lines = lines(CHAINED_ST);
        Assert.assertEquals("File not read correctly", 62, lines.size());
        List<List<String>> chuncks = StackFlatten.get().splitLines(lines, StackFlatten.CAUSED_BY_PREFIX);
        Assert.assertEquals("Chuncks not created correctly, some missed", 4, chuncks.size());
        Assert.assertEquals("Chuncks not created correctly", 8, chuncks.get(1).size());
        List<String> orderedLines = StackFlatten.get().flatten(chuncks, PACKAGE);
        System.out.println(orderedLines.size());
        Assert.assertEquals("Chuncks not reordered correctly", 0, orderedLines.size());
    }

    @Test
    public void testOptions() {
        String[] args = {"-f", "-i="+ CHAINED_ST, "-o=fakeoutput.log"};
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cli = parser.parse(Main.options, args);
            boolean f = cli.hasOption('f');
            boolean e = cli.hasOption('e');
            String input = cli.getOptionValue('i');
            String output = cli.getOptionValue('o');
            String regexp = cli.getOptionValue('p');
            Assert.assertEquals("Flatten parsed correctly", true, f);
            Assert.assertEquals("input parsed correctly", CHAINED_ST, input);
            Assert.assertEquals("output parsed correctly", "fakeoutput.log", output);
            try {
                Main.preprocess(f, e, input, output, regexp);
            } catch (FileNotFoundException e1) {
            	System.out.println(e1.getMessage());
                return;
            }
            Assert.fail("should not reach here");
        } catch (Exception e) {
        	System.out.println(e.getMessage());
            Assert.fail("should not reach here");
        }
    }

    @Test
    public void testErrorMessage() throws Exception {
        List<String> lines = lines(CHAINED_ST2);
        List<String> newLines = ErrorMessage.get().preprocess(lines, null);
        Assert.assertEquals("Thread info and error message removed", "example.BusinessLevelException", newLines.get(0));
    }

    @Test
    public void testChain() throws Exception {
        List<String> lines = lines(CHAINED_ST2);
        List<String> newLines = StackFlatten.get().preprocess(lines, PACKAGE);
        newLines = ErrorMessage.get().preprocess(newLines, null);
        Assert.assertEquals("Chuncks not reordered correctly", 3, newLines.size());
        Assert.assertEquals("Thread info and error message removed", "example.DatabaseException", newLines.get(0));
    }
}
