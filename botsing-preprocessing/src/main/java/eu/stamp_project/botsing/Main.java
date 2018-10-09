package eu.stamp_project.botsing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Main {

	static public Options options = initOptions();

	public static Options initOptions() {
		Options opt = new Options();
		// define options
		Option flatten = new Option("f", "flatten", false, "use this option to flatten the stack trace");
		flatten.setType(Boolean.class);
		Option crash_log = new Option("l", "crash_log", true, "path to the input stack trace");
		Option output_log = new Option("o", "output_log", true, "path to the output stack trace after processing");
		opt.addOption(flatten);
		opt.addOption(crash_log);
		opt.addOption(output_log);
		return opt;
	}

	public static void main(String[] args) {
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine cli = parser.parse(options, args);
			boolean flatten = cli.hasOption('f');
			String input = cli.getOptionValue('l');
			String output = cli.getOptionValue('o');
			File inputFile = new File(input);
			File outFile = new File(output);
			if (outFile.exists()){
				System.out.println("Output file already exists! Exiting...");
				System.exit(0);
			}	
			List<String> inLines = fileToLines(inputFile);
			List<String> outLines;
			if (flatten)
				outLines = StackFlatten.flattenTrace(inLines);
			else
				outLines = inLines;
			linesToFile(outLines, outFile);
		} catch (ParseException e) {
			System.out.println("wrong arguments. Available options are:");
			System.out.println(options.toString());
			System.exit(1);
		}
	}

	/**
	 * reads a file and returns a list of lines for the content
	 * 
	 * @param logPath
	 *            path of the file
	 * @return List of lines
	 */
	static List<String> fileToLines(File log) {
		List<String> lines = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(log))) {
			// returns as stream and convert it into a List
			lines = reader.lines().collect(Collectors.toList());
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return lines;
	}

	/**
	 * reads a list of lines and saves them to a file
	 * 
	 * * @param lines List of lines
	 * 
	 * @param outPath
	 *            path of the file
	 */
	static File linesToFile(List<String> lines, File out) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(out))) {
			lines.forEach(l -> {
				try {
					writer.write(l);
					writer.newLine();
				} catch (IOException e) {
					e.printStackTrace();
				}		
			});
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return out;
	}

}
