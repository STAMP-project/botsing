package eu.stamp.botsing;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.maven.plugin.logging.Log;

import eu.stamp.botsing.setup.BotsingConfiguration;
import eu.stamp.botsing.setup.EvoSuiteConfiguration;
import eu.stamp.botsing.setup.FileUtility;
import eu.stamp.botsing.setup.ModelGenerationConfiguration;

public class ProcessRunner {

	final static String JAVA_CMD = System.getProperty("java.home") + File.separatorChar + "bin" + File.separatorChar
			+ "java";

	public static Integer executeBotsing(File basedir, File botsingReproductionJar, BotsingConfiguration configuration,
			Integer maxTargetFrame, Log log) throws InterruptedException, IOException {

		Integer targetFrame = configuration.getTargetFrame();
		boolean success = false;
		if (maxTargetFrame == null) {

			// execute Botsing only in the target frame passed
			success = ProcessRunner.executeBotsing(basedir, botsingReproductionJar,
					new Long(configuration.getGlobalTimeout()), configuration.getProperties(), log);

		} else {
			// targetFrame (should be null) overridden from maxTargetFrame
			targetFrame = maxTargetFrame;

			// execute Botsing decreasing target frame until a Botsing is
			// executed successfully
			while (!success && targetFrame > 0) {

				log.info("Running Botsing with frame " + targetFrame);
				configuration.addMandatoryProperty(BotsingConfiguration.TARGET_FRAME_OPT, targetFrame.toString());

				// clean output folder
				FileUtility.deleteFolder(configuration.getOptionValue(BotsingConfiguration.TEST_DIR_OPT));

				// execute Botsing
				success = ProcessRunner.executeBotsing(basedir, botsingReproductionJar,
						new Long(configuration.getGlobalTimeout()), configuration.getProperties(), log);

				// stop only if the generated test does not contains "EvoSuite
				// did not generate any tests"
				if (success) {

					if (hasReproductionTestBeenGenerated(configuration)) {
						break;

					} else {
						success = false;
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

	private static boolean hasReproductionTestBeenGenerated(BotsingConfiguration configuration) throws IOException {

		Path testDirPath = Paths.get(configuration.getOptionValue(BotsingConfiguration.TEST_DIR_OPT));
		if (Files.exists(testDirPath) && testDirPath.toFile().list().length > 0) {

			boolean emptyTest = FileUtility.search(configuration.getOptionValue(BotsingConfiguration.TEST_DIR_OPT),
					".*EvoSuite did not generate any tests.*", new String[] { "java" });

			if (!emptyTest) {
				// generated test are NOT empty
				return true;

			} else {
				// generated test are empty
				return false;
			}

		} else {
			// nothing has been generated
			return false;
		}
	}

	private static boolean executeBotsing(File basedir, File botsingReproductionJar, long timeout,
			List<String> properties, Log log) throws InterruptedException, IOException {

		ArrayList<String> jarCommand = new ArrayList<String>();
		jarCommand.add(JAVA_CMD);
		jarCommand.add("-jar");

		jarCommand.add(botsingReproductionJar.getAbsolutePath());

		jarCommand.addAll(properties);

		return ProcessRunner.executeProcess(basedir, log, timeout, jarCommand.toArray(new String[0]));
	}

	private static boolean executeProcess(File workDir, Log log, long timeout, String... command)
			throws InterruptedException, IOException {
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

			if (timeout > 0) {
				boolean exitResult = process.waitFor(timeout, TimeUnit.SECONDS);

				// process did not complete before timeout, kill it
				if (!exitResult) {
					log.error("Process terminated because it took more than " + timeout + "s to complete");
					throw new InterruptedException();
				}
			}
			// Get process exitcode
			int exitCode = process.waitFor();

			if (exitCode != 0) {
				// process terminated abnormally
				log.error("Error executing the process");
				return false;

			} else {
				// process terminated normally
				log.debug("Process terminated");
			}

		} catch (InterruptedException e) {
			if (process != null) {

				try {
					// be sure streamers are closed, otherwise process might
					// hang on Windows
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

	private static boolean executeProcess(File workDir, Log log, String... command)
			throws InterruptedException, IOException {

		return executeProcess(workDir, log, 0, command);
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

	public static boolean executeBotsingModelGeneration(File basedir, File botsingModelGenerationJar,
			ModelGenerationConfiguration configuration, Log log) throws InterruptedException, IOException {

		ArrayList<String> jarCommand = new ArrayList<String>();
		jarCommand.add(JAVA_CMD);
		jarCommand.add("-jar");

		jarCommand.add(botsingModelGenerationJar.getAbsolutePath());

		jarCommand.addAll(configuration.getProperties());

		// clean outDir folder
		FileUtility.deleteFolder(configuration.getOptionValue(ModelGenerationConfiguration.OUT_DIR_OPT));

		return ProcessRunner.executeProcess(basedir, log, jarCommand.toArray(new String[0]));
	}

	public static boolean executeEvoSuite(File basedir, File evoSuiteJar, EvoSuiteConfiguration configuration, Log log)
			throws InterruptedException, IOException {

		ArrayList<String> jarCommand = new ArrayList<String>();
		jarCommand.add(JAVA_CMD);
		jarCommand.add("-jar");

		jarCommand.add(evoSuiteJar.getAbsolutePath());

		jarCommand.addAll(configuration.getProperties());

		jarCommand.add("-generateMOSuite");

		log.info(" --- " + jarCommand);

		return ProcessRunner.executeProcess(basedir, log, jarCommand.toArray(new String[0]));
	}
}