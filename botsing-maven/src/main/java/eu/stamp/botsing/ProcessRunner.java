package eu.stamp.botsing;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.logging.Log;

public class ProcessRunner {

	public static Integer executeBotsing(File basedir, File botsingReproductionJar, BotsingConfiguration configuration, Integer maxTargetFrame, Log log) throws InterruptedException, IOException {

		Integer targetFrame = configuration.getTargetFrame();
		boolean success = false;
		if (maxTargetFrame == null) {

			// execute Botsing only in the target frame passed
			success = ProcessRunner.executeBotsing(basedir, botsingReproductionJar, configuration.getProperties(), log);

		} else {
			// targetFrame (should be null) overridden from maxTargetFrame
			targetFrame = maxTargetFrame;

			// execute Botsing decreasing target frame until a Botsing is executed successfully
			while (!success || targetFrame == 0) {

				log.info("Running Botsing with frame " + targetFrame);
				configuration.addTargetFrame(targetFrame);

				success = ProcessRunner.executeBotsing(basedir, botsingReproductionJar, configuration.getProperties(), log);

				// check that the generated test does not contains "EvoSuite did not generate any tests"
				if (success && Paths.get(configuration.getTestDir()).toFile().list().length > 0) {

					boolean emptyTest = FileUtility.search(configuration.getTestDir(), ".*EvoSuite did not generate any tests.*");
					if (!emptyTest) {
						break;
					}
				}

				targetFrame = configuration.decreaseTargetFrame();
			}
		}

		// return target frame that get a successful execution or -1
		if (success) {
			return targetFrame;

		} else {
			return -1;
		}
	}

	private static boolean executeBotsing(File basedir, File botsingReproductionJar, List<String> properties, Log log)
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

	private static boolean executeProcess(File workDir, Log log, String... command) throws InterruptedException, IOException {
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