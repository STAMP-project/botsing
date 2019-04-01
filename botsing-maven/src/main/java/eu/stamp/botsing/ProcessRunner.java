package eu.stamp.botsing;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.logging.Log;

public class ProcessRunner {

	public static boolean executeBotsing(File basedir, File botsingReproductionJar, List<String> properties, Log log)
			throws InterruptedException, IOException {

		final String JAVA_CMD = System.getProperty("java.home") + File.separatorChar + "bin" + File.separatorChar
				+ "java";

		ArrayList<String> jarCommand = new ArrayList<String>();
		jarCommand.add(JAVA_CMD);
		jarCommand.add("-jar");

		jarCommand.add(botsingReproductionJar.getAbsolutePath());

		jarCommand.addAll(properties);

		return ProcessRunner.executeProcess(basedir, log, jarCommand.toArray(new String[0]));
	}

	public static boolean executeProcess(File workDir, Log log, String... command) throws InterruptedException, IOException {
		Process process = null;

		if (log.isDebugEnabled()) {
			log.debug("Going to execute command: " + String.join(" ", command));
		}

		try {
			ProcessBuilder builder = new ProcessBuilder(command);

			builder.directory(workDir.getAbsoluteFile());
			builder.redirectErrorStream(true);

			process = builder.start();
			handleProcessOutput(process, log);

			// Scanner s = new Scanner(process.getInputStream());
			// StringBuilder text = new StringBuilder();
			// while (s.hasNextLine()) {
			// text.append(s.nextLine());
			// text.append("\n");
			// }
			// s.close();

			// log.debug("Process exited with result {} and output {} ", result, text);

			int exitCode = process.waitFor();

			if (exitCode != 0) {
				log.error("Error executing botsing-reproduction");
				return false;
			} else {
				log.debug("botsing-reproduction terminated");
			}

		} catch (InterruptedException e) {
			if (process != null) {

				try {
					// be sure streamers are closed, otherwise process might hang on Windows
					process.getOutputStream().close();
					process.getInputStream().close();
					process.getErrorStream().close();

				} catch (Exception t) {
					log.error("Failed to close process stream: " + t.toString());
				}

				process.destroy();

			}
			return false;

		}

		process = null;
		return true;
	}

	private static void handleProcessOutput(final Process process, Log logger) {

		Thread reader = new Thread() {
			@Override
			public void run() {
				try {
					BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));

					while (!this.isInterrupted()) {
						String line = in.readLine();
						if (line != null && !line.isEmpty()) {
							logger.info(line);
						}
					}
				} catch (Exception e) {
					logger.debug("Exception while reading spawn process output: " + e.toString());
				}
			}
		};

		reader.start();
		logger.debug("Started thread to read spawn process output");
	}
}