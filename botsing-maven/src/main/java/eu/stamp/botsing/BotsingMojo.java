package eu.stamp.botsing;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

/**
 * Mojo class to run Botsing
 *
 * @author Luca Andreatta
 */
@Mojo(name = "botsing", defaultPhase = LifecyclePhase.VERIFY, requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true)
public class BotsingMojo extends AbstractMojo {

	/**
	 * To see all the properties available take a look at org.evosuite.Properties.java
	 */

	/**
	 * Folder with dependencies to run the project
	 */
	@Parameter(property = "projectCP")
	private String projectCP;

	/**
	 * Log file with the stacktrace
	 */
	@Parameter(defaultValue = "sample.log", property = "crash_log")
	private String crashLog;

	/**
	 * The frame level up to which parse the stack trace
	 */
	@Parameter(defaultValue = "3", property = "target_frame")
	private Integer targetFrame;

	/**
	 * Maven variables
	 */
	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	private MavenProject project;

	@Component
	private RepositorySystem repoSystem;

	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
	private RepositorySystemSession repoSession;

	@Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
	private List<RemoteRepository> repositories;

	@Override
	public void execute() throws MojoExecutionException {
		getLog().info("Starting Botsing to generate tests with EvoSuite");

		Botsing botsing = new Botsing();
		List<String> propertiesList = new ArrayList<String>();

		propertiesList.add("-crash_log");
		propertiesList.add(crashLog);

		propertiesList.add("-target_frame");
		propertiesList.add(targetFrame.toString());

		String dependencies = null;
		if (projectCP != null) {
			dependencies = getDependenciesFromFolder(projectCP);
		} else {
			dependencies = getDependenciesFromPom();
		}

		getLog().debug("dependencies: " + dependencies);
		propertiesList.add("-projectCP");
		propertiesList.add(dependencies);

		try {
			// Start Botsing
			botsing.parseCommandLine(propertiesList.toArray(new String[0]));

		} catch (Exception e) {
			throw new MojoExecutionException("Error executing Botsing", e);
		}

		getLog().info("Stopping Botsing");
	}

	public String getDependenciesFromPom() throws MojoExecutionException {
		String result = "";

		// Add ./target/classes
		result += project.getModel().getBuild().getDirectory() + File.separator + "classes" + File.pathSeparator;

		// Add pom project dependencies
		for (Artifact unresolvedArtifact : this.project.getDependencyArtifacts()) {
			File file = getArtifactFile(unresolvedArtifact);

			result += file.getAbsolutePath() + File.pathSeparator;
		}

		return result;
	}

	private File getArtifactFile(Artifact artifact) throws MojoExecutionException {
		/**
		 * Taken from https://gist.github.com/vincent-zurczak/282775f56d27e12a70d3
		 */

		// We ask Maven to resolve the artifact's location.
		// It may imply downloading it from a remote repository,
		// searching the local repository or looking into the reactor's cache.

		// To achieve this, we must use Aether
		// (the dependency mechanism behind Maven).
		String artifactId = artifact.getArtifactId();
		org.eclipse.aether.artifact.Artifact aetherArtifact = new DefaultArtifact(artifact.getGroupId(),
				artifact.getArtifactId(), artifact.getClassifier(), artifact.getType(), artifact.getVersion());

		ArtifactRequest req = new ArtifactRequest().setRepositories(this.repositories).setArtifact(aetherArtifact);
		ArtifactResult resolutionResult;
		try {
			resolutionResult = this.repoSystem.resolveArtifact(this.repoSession, req);

		} catch (ArtifactResolutionException e) {
			throw new MojoExecutionException("Artifact " + artifactId + " could not be resolved.", e);
		}

		// The file should exists, but we never know.
		File file = resolutionResult.getArtifact().getFile();
		if (file == null || !file.exists()) {
			getLog().warn("Artifact " + artifactId
					+ " has no attached file. Its content will not be copied in the target model directory.");
		}

		return file;
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

				result += (dependency + File.pathSeparator);
			}
		}

		result = result.substring(0, result.length() - 1);

		return result;
	}

}