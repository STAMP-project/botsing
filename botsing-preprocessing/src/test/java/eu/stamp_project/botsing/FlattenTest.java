package eu.stamp_project.botsing;

import java.io.File;
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
	@Test
	public void testPartitioning() throws Exception {
		File log = openFile(CHAINED_ST2);
		List<String> lines = Main.fileToLines(log);
		Assert.assertEquals("File not read correctly", 11, lines.size());
		List<List<String>> chuncks = StackFlatten.splitLines(lines, StackFlatten.CAUSED_BY_PREFIX);
		Assert.assertEquals("Chuncks not created correctly, some missed", 3, chuncks.size());
		Assert.assertEquals("Chuncks not created correctly", 4, chuncks.get(2).size());
		List<String> orderedLines = StackFlatten.flatten(chuncks);
		Assert.assertEquals("Chuncks not reordered correctly", 5, orderedLines.size());	
	}
	
	@Test
	public void testComplexPartitioning() throws Exception {
		File log = openFile(CHAINED_ST);
		List<String> lines = Main.fileToLines(log);
		Assert.assertEquals("File not read correctly", 62, lines.size());
		List<List<String>> chuncks = StackFlatten.splitLines(lines, StackFlatten.CAUSED_BY_PREFIX);
		Assert.assertEquals("Chuncks not created correctly, some missed", 4, chuncks.size());
		Assert.assertEquals("Chuncks not created correctly", 8, chuncks.get(1).size());
		List<String> orderedLines = StackFlatten.flatten(chuncks);
		Assert.assertEquals("Chuncks not reordered correctly", 51, orderedLines.size());	
	}
	
	@Test
	public void testOptions() throws Exception {
		String[] args = {"-f", "-l=D:/_WORKSPACE/STAMP/input.log", "-o=D:/_WORKSPACE/STAMP/output.log"};
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine cli = parser.parse(Main.options, args);
			boolean flatten = cli.hasOption('f');
			String input = cli.getOptionValue('l');
			String output = cli.getOptionValue('o');
			
			Assert.assertEquals("Flatten parsed correctly",true, flatten);
			Assert.assertEquals("input parsed correctly","D:/_WORKSPACE/STAMP/input.log", input);
			Assert.assertEquals("input parsed correctly","D:/_WORKSPACE/STAMP/output.log", output);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
