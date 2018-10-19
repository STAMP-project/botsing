package eu.stamp_project.botsing;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.junit.Assert;
import org.junit.Test;

public class FlattenTest {
	private static final String CHAINED_ST = "nested_st.txt";
	private static final String CHAINED_ST2 = "nested_st2.txt";

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
		List<String> orderedLines = StackFlatten.get().flatten(chuncks);
		Assert.assertEquals("Chuncks not reordered correctly", 5, orderedLines.size());
	}

	@Test
	public void testComplexPartitioning() throws Exception {
		List<String> lines = lines(CHAINED_ST);
		Assert.assertEquals("File not read correctly", 62, lines.size());
		List<List<String>> chuncks = StackFlatten.get().splitLines(lines, StackFlatten.CAUSED_BY_PREFIX);
		Assert.assertEquals("Chuncks not created correctly, some missed", 4, chuncks.size());
		Assert.assertEquals("Chuncks not created correctly", 8, chuncks.get(1).size());
		List<String> orderedLines = StackFlatten.get().flatten(chuncks);
		Assert.assertEquals("Chuncks not reordered correctly", 51, orderedLines.size());
	}

	@Test
	public void testOptions() throws Exception {
		String[] args = { "-f", "-l=fakeinput.log", "-o=fakeoutput.log" };
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine cli = parser.parse(Main.options, args);
			boolean flatten = cli.hasOption('f');
			boolean e = cli.hasOption('e');
			boolean a = cli.hasOption('a');
			String input = cli.getOptionValue('l');
			String output = cli.getOptionValue('o');
			Assert.assertEquals("Flatten parsed correctly", true, flatten);
			Assert.assertEquals("input parsed correctly", "fakeinput.log", input);
			Assert.assertEquals("input parsed correctly", "fakeoutput.log", output);
			try {
				Main.preprocess(flatten, e, a, input, output);
			} catch (FileNotFoundException e1) {
				return;
			}
			Assert.fail("should not reach here");
		} catch (Exception e) {
			Assert.fail("should not reach here");
		}
	}

	@Test
	public void testErrorMessage() throws Exception {
		List<String> lines = lines(CHAINED_ST2);
		List<String> newLines = ErrorMessage.get().preprocess(lines);
		Assert.assertEquals("Thread info and error message removed", "example.BusinessLevelException", newLines.get(0));
	}

	@Test
	public void testChain() throws Exception {
		List<String> lines = lines(CHAINED_ST2);
		List<String> newLines = StackFlatten.get().preprocess(lines);
		newLines = ErrorMessage.get().preprocess(newLines);
		Assert.assertEquals("Chuncks not reordered correctly", 5, newLines.size());
		Assert.assertEquals("Thread info and error message removed", "example.DatabaseException", newLines.get(0));
	}
}
