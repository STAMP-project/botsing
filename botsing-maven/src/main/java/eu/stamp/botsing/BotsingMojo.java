package eu.stamp.botsing;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Generate tests using stacktrace log.
 *
 * @author Luca Andreatta
 */
@Mojo(name = "evocrash")
public class BotsingMojo extends AbstractMojo {

	
	/**
	 * folder to save the tests
	 */
	@Parameter(defaultValue = "CrashReproduction-tests", property = "test_dir")
	private String testDir;

	/**
	 * log file with the stacktrace
	 */
	@Parameter(defaultValue = "sample.log", property = "log_file")
	private String logFile;

	/**
	 * Folder with binary files to reproduce the stacktrace
	 */
	@Parameter(defaultValue = "sample-dependency", property = "bin_dir")
	private String binDir;
	
	@Parameter(defaultValue = "50", property = "max_recursion")
	private Integer maxRecursion;
	
	@Parameter(defaultValue = "80", property = "population")
	private Integer population;
	
	@Parameter(defaultValue = "1800", property = "search_budget")
	private Integer searchBudget;
	
	/**
	 * The frame level up to which parse the stack trace
	 */
	@Parameter(defaultValue = "3", property = "target_frame_level")
	private Integer targetFrameLevel;

	public static final String SEPARATOR = System.getProperty("path.separator");


	public void execute() throws MojoExecutionException {
		getLog().info("Starting EvoSuite to generate tests with EvoCrash");
		getLog().info("test_dir: " + testDir);
		getLog().info("user_dir: " + binDir);
		getLog().info("log_file: " + logFile);

		Botsing botsing = new Botsing();
		
		String[] prop = { 
				"-Dcrash_log=" + logFile,
				"-Dtarget_frame=" + targetFrameLevel, 
				"-projectCP",
				getDependenciesFromFolder(binDir), };
		botsing.parseCommandLine(prop);

		getLog().info("Stopping EvoSuite");
	}

	public static String getDependenciesFromFolder(String dependenciesFolder) {
		String result = "";

		if (dependenciesFolder == null) {
			return result;
		}

		File depFolder = new File(dependenciesFolder);
		File[] listOfFilesInSourceFolder = depFolder.listFiles();

		for (int i = 0; i < listOfFilesInSourceFolder.length; i++) {

			if (listOfFilesInSourceFolder[i].getName().charAt(0) != '.') {
				Path depPath = Paths.get(depFolder.getAbsolutePath(), listOfFilesInSourceFolder[i].getName());
				String dependency = depPath.toString();

				result += (dependency + SEPARATOR);
			}
		}

		result = result.substring(0, result.length() - 1);

		return result;
	}

}