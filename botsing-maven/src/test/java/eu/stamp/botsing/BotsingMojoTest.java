package eu.stamp.botsing;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.it.Verifier;
import org.junit.Ignore;
import org.junit.Test;

public class BotsingMojoTest {

	@Test
	@Ignore
	public void shouldExecuteBotsingMojoOnBotsingexampleProjectAndGenerateTestWithMavenVerifier() throws Exception {

		// botsing-example folder
		String user_dir = System.getProperty("user.dir");
		File file = new File(user_dir);
		String baseDir = Paths.get(file.getParent(), "botsing-examples").toString();

		// clean output folder
		File outputDir = Paths.get(baseDir, "crash-reproduction-tests").toFile();
		outputDir.delete();

		// use local Maven setting.xml configuration file
		String user_home = System.getProperty("user.home");
		String settingsFile = Paths.get(user_home, ".m2/conf/settings.xml").toString();

		// init verifier
		Verifier verifier;
		verifier = new Verifier(baseDir, settingsFile);

		// add CLI parameters
		verifier.addCliOption("-Dcrash_log=src/main/resources/Fraction.log");
		verifier.addCliOption("-Dtarget_frame=1");
		//verifier.addCliOption("-Dorg.slf4j.simpleLogger.log.org.evosuite=off ");
		//verifier.addCliOption("-Dorg.slf4j.simpleLogger.showLogName=true");

		// add goals to execute
		List<String> goals = new ArrayList<String>();
		goals.add("clean");
		goals.add("compile");
		goals.add("eu.stamp-project:botsing-maven:botsing");
		//goals.add("eu.stamp-project:botsing-maven:botsing");

		try {
			// execute goals
			verifier.executeGoals(goals);

			// check maven log
			verifier.verifyErrorFreeLog();

			// check botsing default output folder
			File outputDirToCheck = Paths.get(baseDir, "crash-reproduction-tests").toFile();
			String message = "outputDir '"+outputDirToCheck.getAbsolutePath()+"' is empty";
			assertNotNull(message, outputDirToCheck);
			assertNotNull(message, outputDirToCheck.list());
			assertTrue(message, outputDirToCheck.list().length > 0);


		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

		verifier.resetStreams();
	}

}
