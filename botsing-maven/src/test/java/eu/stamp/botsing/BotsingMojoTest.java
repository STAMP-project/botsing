package eu.stamp.botsing;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.maven.it.Verifier;

public class BotsingMojoTest {

	/*
	 * Test Disabled until FIXME will be resolved
	 */
	// @Test
	public void shouldExecuteBotsingMojoOnBotsingexampleProjectAndGenerateTestWithMavenVerifier() throws Exception {

		// botsing-example folder
		String user_dir = System.getProperty("user.dir");
		File file = new File(user_dir);
		String baseDir = Paths.get(file.getParent(), "botsing-examples").toString();

		// use local Maven setting.xml configuration file
		String user_home = System.getProperty("user.home");
		String settingsFile = Paths.get(user_home, ".m2/conf/settings.xml").toString();

		// init verifier
		Verifier verifier;
		verifier = new Verifier(baseDir, settingsFile);

		// add CLI parameters
		verifier.addCliOption("-Dcrash_log=src/main/resources/Fraction.log");
		verifier.addCliOption("-Dtarget_frame=1");
		verifier.addCliOption("-Dorg.slf4j.simpleLogger.log.org.evosuite=off ");
		verifier.addCliOption("-Dorg.slf4j.simpleLogger.showLogName=true");

		// add goals to execute
		List<String> goals = new ArrayList<String>();
		goals.add("clean");
		goals.add("compile");
		goals.add("eu.stamp-project:botsing-maven:1.0.4-SNAPSHOT:botsing");
		// goals.add("eu.stamp-project:botsing-maven:botsing");

		try {
			// execute goals
			verifier.executeGoals(goals);

			// FIXME:
			// IllegalAccessError: tried to access class com.sun.tools.javac.util.Log$2 from
			// class com.sun.tools.javac.util.Log
			// at eu.stamp.botsing.reproduction.CrashReproduction.compileAndCheckTests
			// (CrashReproduction.java:293)

			// check maven log
			verifier.verifyErrorFreeLog();

			// check botsing default output folder
			File outputDir = Paths.get(baseDir, "crash-reproduction-tests").toFile();

			// check that there is something inside
			assertTrue(outputDir.list().length > 0);

		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

		verifier.resetStreams();
	}

	/*
	 * Test Disabled until FIXME will be resolved
	 */
	//@Test
	public void shouldExecuteBotsingMojoOnBotsingexampleProjectAndGenerateTest() throws Exception {
		// botsing-example folder
		String user_dir = System.getProperty("user.dir");
		File file = new File(user_dir);
		File baseDir = Paths.get(file.getParent(), "botsing-examples").toFile();

		try {
			// execute botsing-maven as an external process
			executeProcess(baseDir, "mvn", "clean", "compile", "-X",  "eu.stamp-project:botsing-maven:1.0.4-SNAPSHOT:botsing",
					"-Dcrash_log=src/main/resources/Fraction.log", "-Dtarget_frame=1",
					"-Dorg.slf4j.simpleLogger.log.org.evosuite=off ", "-Dorg.slf4j.simpleLogger.showLogName=true");

			// FIXME: Failure to find org.apache.maven.plugins:maven-dependency-plugin:jar:${maven-dependency-plugin.version}

			// check botsing default output folder
			File outputDir = Paths.get(baseDir.getAbsolutePath(), "crash-reproduction-tests").toFile();

			// check that there is something inside
			String message = "outputDir '"+outputDir.getAbsolutePath()+"' is empty";
			assertNotNull(message, outputDir);
			assertNotNull(message, outputDir.list());
			assertTrue(message, outputDir.list().length > 0);

		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	private static int executeProcess(File workDir, String... command) throws InterruptedException, IOException {
		ProcessBuilder builder = new ProcessBuilder(command);

		builder.directory(workDir.getAbsoluteFile());
		builder.redirectErrorStream(true);

		Process process = builder.start();

		Scanner s = new Scanner(process.getInputStream());
		StringBuilder text = new StringBuilder();
		while (s.hasNextLine()) {
			text.append(s.nextLine());
			text.append("\n");
		}
		s.close();

		System.out.println(text);

		/** FIXME
		 * org.evosuite.junit.JUnitAnalyzer - Going to execute: removeTestsThatDoNotCompile
		 *  An exception has occurred in the compiler (1.8.0_201). Please file a bug against the Java compiler via the Java
		 *  bug reporting page (http://bugreport.java.com) after checking the Bug Database (http://bugs.java.com) for duplicates.
		 *  Include your program and the following diagnostic in your report. Thank you.
		 *
		 *  java.lang.IllegalAccessError: tried to access class com.sun.tools.javac.util.Log$2 from class com.sun.tools.javac.util.Log
		 *  at eu.stamp.botsing.reproduction.CrashReproduction.compileAndCheckTests (CrashReproduction.java:293)
		 */

		int result =  process.waitFor();

		return result;
	}

}
