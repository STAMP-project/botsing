package eu.stamp.botsing;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.it.Verifier;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Integration Test that runs botsing-maven on authzforce-ce-core-pdp-testutils. (It takes about 10 minutes to complete)
 *
 * @author luca
 *
 */
public class BotsingMojoTestIT {

	private final String crashLog = "java.lang.RuntimeException: Failed to load XML schemas: [classpath:pdp.xsd]\n" +
			"	at org.ow2.authzforce.core.pdp.impl.SchemaHandler.createSchema(SchemaHandler.java:541)\n" +
			"	at org.ow2.authzforce.core.pdp.impl.PdpModelHandler.<init>(PdpModelHandler.java:159)\n" +
			"	at org.ow2.authzforce.core.pdp.impl.PdpEngineConfiguration.getInstance(PdpEngineConfiguration.java:682)\n" +
			"	at org.ow2.authzforce.core.pdp.impl.PdpEngineConfiguration.getInstance(PdpEngineConfiguration.java:699)\n" +
			"	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\n" +
			"	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n" +
			"	at java.lang.reflect.Method.invoke(Method.java:498)\n" +
			"	at org.springframework.beans.factory.support.SimpleInstantiationStrategy.instantiate(SimpleInstantiationStrategy.java:162)";

	public static final String POM_FOR_BOTSING = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
			"<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
			"    <modelVersion>4.0.0</modelVersion>\n" +
			"    <groupId>eu.stamp-project</groupId>\n" +
			"    <artifactId>botsing-maven-working-project</artifactId>\n" +
			"    <version>1.0.0-SNAPSHOT</version>\n" +
			"    <packaging>pom</packaging>\n" +
			"    <name>Project to run Botsing Maven</name>\n" +
			"    <description>Project to run botsing-maven.</description>\n" +
			"</project>";

	private final String groupId="org.ow2.authzforce";
	private final String artifactId="authzforce-ce-core-pdp-testutils";
	private final String version="13.3.1";
	private final String searchBudget="60";
	private final String globalTimeout="90";

	@Rule
	public TemporaryFolder tmpFolder = new TemporaryFolder();

	@Test
	public void shouldExecuteBotsingMojoOnAuthzforceAndGenerateTestWithMavenVerifier() throws Exception {

		// prepare folder
		File workingDir = tmpFolder.newFolder();

		// create dummy pom file
		File pomFile = new File(workingDir + (File.separator + "pom.xml"));
		FileUtils.writeStringToFile(pomFile, POM_FOR_BOTSING, Charset.defaultCharset());

		// create crashLog file
		File crashLogFile = new File(workingDir + (File.separator + "crash.log"));
		FileUtils.writeStringToFile(crashLogFile, crashLog, Charset.defaultCharset());

		// use local Maven setting.xml configuration file
		String user_home = System.getProperty("user.home");
		String settingsFile = Paths.get(user_home, ".m2/conf/settings.xml").toString();

		// init verifier
		Verifier verifier;
		verifier = new Verifier(workingDir.getAbsolutePath(), settingsFile);

		// add CLI parameters
		verifier.addCliOption("-Dcrash_log=" + crashLogFile.getAbsolutePath());
		verifier.addCliOption("-Dgroup_id=" + groupId);
		verifier.addCliOption("-Dartifact_id=" + artifactId);
		verifier.addCliOption("-Dversion=" + version);
		verifier.addCliOption("-Dsearch_budget=" + searchBudget);
		verifier.addCliOption("-Dglobal_timeout=" + globalTimeout);

		//verifier.addCliOption("-Dorg.slf4j.simpleLogger.log.org.evosuite=off ");
		//verifier.addCliOption("-Dorg.slf4j.simpleLogger.showLogName=true");

		// add goals to execute
		// TODO add current project version
		List<String> goals = new ArrayList<String>();
		goals.add("eu.stamp-project:botsing-maven:botsing");

		try {
			// execute goals
			verifier.executeGoals(goals);

			// check botsing default output folder
			File outputDirToCheck = Paths.get(workingDir.getAbsolutePath(), "crash-reproduction-tests").toFile();
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
