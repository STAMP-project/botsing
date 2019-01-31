package eu.stamp_project.botsing;

import java.io.File;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class AnnotationTest {
	
	private static final String ANNOTATION = "annotation.txt";
	
	public static File openFile(String localName) throws Exception {
		return new File(FlattenTest.class.getResource(localName).toURI());
	}
	
	public static List<String> lines(String name) throws Exception {
		File log = openFile(name);
		List<String> lines = Main.fileToLines(log);
		return lines;
	}
	
	@Test
	public void test() throws Exception{
		System.out.println("QUIIIIIIIIIIIIIII");
		String test = "test";
		Assert.assertEquals("Test", "test", test);
		List<String> lines = lines(ANNOTATION);
		System.out.println(lines);
	}
}