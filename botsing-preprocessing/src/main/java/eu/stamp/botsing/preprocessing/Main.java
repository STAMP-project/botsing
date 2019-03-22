package eu.stamp.botsing.preprocessing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Main {

	static public Options options = initOptions();
	private static String HYPHENS = "---";

	public static Options initOptions() {
		Options opt = new Options();
		// define flags
		Option flatten = new Option("f", "flatten", false, "use this option to flatten the stack trace");
		Option error = new Option("e", "error_message", false, "use this option to remove the error message");

		// define parameters
		Option crash_log = new Option("i", "crash_log", true, "path to the input stack trace");
		Option output_log = new Option("o", "output_log", true, "path to the output stack trace after processing");
		Option source = new Option("p", "package", true, "regular expression package pointing to the classes of the project");

		// define required options
		crash_log.setRequired(true);
		output_log.setRequired(true);

		opt.addOption(crash_log);
		opt.addOption(output_log);
		opt.addOption(source);
		opt.addOption(flatten);
		opt.addOption(error);

		return opt;
	}

	public static void main(String[] args) {
		if (args.length == 0) {
			printOptions();
		}

		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine cli = parser.parse(options, args);
			boolean f = cli.hasOption('f');
			boolean e = cli.hasOption('e');

			String input = cli.getOptionValue('i');
			String output = cli.getOptionValue('o');
			String regexp = cli.getOptionValue('p');// package

			if (!(f || e)) {
				System.out.println("Wrong arguments. No '-f' or '-e' flag selected");
				printOptions();
			}

			if (f && regexp == null) {
				System.out.println("Wrong arguments. For '-f' flag, it's necessary to set the regexp with '-p'");
				printOptions();
			}

			preprocess(f, e, input, output, regexp);

		} catch (ParseException e) {
			System.out.println("Wrong arguments. " + e.getMessage());
			printOptions();
		} catch (FileNotFoundException e) {
			System.out.println("Wrong arguments. " + e.getMessage());
			return;
		}
	}

	/**
	 * Performs the pre-processing on the stack trace based on the options
	 *
	 * @param flatten
	 *            if true, a chained stack trace is flattened
	 * @param error
	 *            if true, remove the error message
	 * @param input
	 *            input file path
	 * @param output
	 *            output file path
	 */
	public static void preprocess(boolean flatten, boolean error, String input, String output, String regexp)
			throws FileNotFoundException {
		File inputFile = new File(input);
		if (!inputFile.exists()) {
			throw new FileNotFoundException("Input file name '" + inputFile + "' does not exist!");
		}
		File outFile = new File(output);
		if (outFile.exists()) {
			outFile.delete();
		}
		List<String> lines = fileToLines(inputFile);

		// pre-processing
		if (flatten) {
			lines = StackFlatten.get().preprocess(lines, regexp);
		}
		if (error) {
			lines = ErrorMessage.get().preprocess(lines, null);
		}

		// generate output log file
		linesToFile(lines, outFile);

		System.out.println("End pre-processing");
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
			boolean first = false;
			Iterator<String> i = reader.lines().iterator();
			while (i.hasNext()) {
				String line = i.next();
				// read stack trace inside the first and the second hyphens
				// '---'
				if (line.startsWith(HYPHENS)) {
					if (!first) {
						first = true;
					} else {
						break;
					}
				} else {
					lines.add(line);
				}
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return lines;
	}

	/**
	 * reads a list of lines and saves them to a file
	 *
	 * @param lines
	 *            List of lines
	 * @param out
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

	static void printOptions() {
		Collection<Option> ops = options.getOptions();
		System.out.println("Available options are:");
		ops.forEach((ele) -> {
			System.out.print("-" + ele.getOpt() + " " + ele.getLongOpt());
			if (ele.isRequired()) {
				System.out.print(" [required] ");
			} else {
				System.out.print(" [optional] ");
			}
			System.out.print("type: ");
			if (ele.hasArg()) {
				System.out.print("param");
			} else {
				System.out.print("flag");
			}
			System.out.println(", description: " + ele.getDescription());
		});
		return;
	}

}
