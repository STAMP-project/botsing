package eu.stamp.botsing;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
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
	 * Folder with dependencies to run the project
	 */
	@Parameter(property = "project_cp")
	private String projectCP;

	/**
	 * Log file with the stacktrace
	 */
	@Parameter(property = "crash_log")
	private String crashLog;

	/**
	 * The frame level up to which parse the stack trace
	 */
	@Parameter(property = "target_frame")
	private Integer targetFrame;

	/**
	 * The frame level up to which parse the stack trace
	 */
	@Parameter(property = "max_target_frame")
	private Integer maxTargetFrame;

	/**
	 * the size of the population that evolves during the search with a default
	 * value of 100
	 */
	@Parameter(property = "population")
	private Integer population;

	/**
	 * the search budget in seconds with a default value of 1800
	 */
	@Parameter(property = "search_budget")
	private Integer searchBudget;

	/**
	 * the global timeout in seconds, after which the execution stops if the search
	 * is stuck with a default value of 1800 (the timeout is only reached if the
	 * search does not improve after 1800 seconds)
	 */
	@Parameter(property = "global_timeout")
	private Integer globalTimeout;

	/**
	 * the directory where the tests are generated with a default value of
	 * `crashreproduction-tests`
	 */
	@Parameter(property = "test_dir")
	private String testDir;

	/**
	 * Botsing version to use
	 */
	@Parameter(property = "botsing_version", defaultValue = "1.0.5-SNAPSHOT")
	private String botsingVersion;

	/**
	 * the seed used to initialize the random number generator. This value allows to
	 * have deterministic behavior and should be set when performing evaluations
	 */
	@Parameter(property = "random_seed")
	private Long randomSeed;

	@Parameter(property = "no_runtime_dependency", defaultValue = "false")
	private String noRuntimeDependency;

	/**
	 * Maven variables
	 */
	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	private MavenProject project;

	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
	private RepositorySystemSession repoSession;

	@Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
	private List<RemoteRepository> repositories;

	@Parameter(defaultValue = "${session}", required = true, readonly = true)
	private MavenSession session;

	@Component
	private RepositorySystem repoSystem;

	/**
	 * Contains the full list of projects in the reactor.
	 */
	@Parameter(defaultValue = "${reactorProjects}", readonly = true, required = true)
	private List<MavenProject> reactorProjects;

	/**
	 * The dependency tree builder to use.
	 */
	@Component(hint = "default")
	private DependencyGraphBuilder dependencyGraphBuilder;

	@Override
	public void execute() throws MojoExecutionException {
		getLog().info("Starting Botsing to generate tests with EvoSuite");

		// TODO check properties

		// build dependencies List
		String dependencies = null;
		if (projectCP != null) {
			dependencies = getDependenciesFromFolder(projectCP);
		} else {
			dependencies = getDependenciesFromPom();
		}

		// print dependencies for debug
		getLog().debug("dependencies: " + dependencies);

		// add dependencies
		BotsingConfiguration configuration = new BotsingConfiguration(crashLog, maxTargetFrame, dependencies,
				population, searchBudget, globalTimeout, testDir, randomSeed, noRuntimeDependency, getLog());

		// Start Botsing
		try {

			// TODO find a way to get the latest version of botsing-reproduction
			// tried "[1.0.4, )" for version but
			// Could not find artifact eu.stamp-project:botsing-reproduction:jar:[1.0.4, )
			// in central (https://repo.maven.apache.org/maven2) -> [Help 1]

			File botsingReproductionJar = getArtifactFile(
					new DefaultArtifact("eu.stamp-project", "botsing-reproduction", "", "jar", botsingVersion));

			Integer actualTargetFrame = ProcessRunner.executeBotsing(project.getBasedir(), botsingReproductionJar,
					configuration, maxTargetFrame, getLog());

			if (actualTargetFrame <= 0) {
				throw new MojoFailureException("Failed to reproduce the stacktrace.");

			} else {
				getLog().info("Botsing executed succesfully");
			}

		} catch (Exception e) {
			throw new MojoExecutionException("Error executing Botsing", e);
		}

		getLog().info("Stopping Botsing");
	}

	public String getDependenciesFromPom() throws MojoExecutionException {
		String result = "";

		// add project artifact
		result += getArtifactFile(project.getArtifact()).getAbsolutePath() + File.pathSeparator;

		// Add pom project dependencies
		for (Artifact unresolvedArtifact : getDependencyTree()) {
			File file = getArtifactFile(unresolvedArtifact);

			result += file.getAbsolutePath() + File.pathSeparator;
		}

		return result;
	}

	public List<Artifact> getDependencyTree() throws MojoExecutionException {
		try {
			ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(
					session.getProjectBuildingRequest());
			buildingRequest.setProject(project);

			// TODO check if it is necessary to specify an artifact filter
			DependencyNode rootNode = dependencyGraphBuilder.buildDependencyGraph(buildingRequest, null,
					reactorProjects);

			List<Artifact> artifactList = new ArrayList<Artifact>();
			addChildDependencies(rootNode, artifactList);

			return artifactList;

		} catch (DependencyGraphBuilderException e) {
			throw new MojoExecutionException("Couldn't download artifact: " + e.getMessage(), e);
		}
	}

	private void addChildDependencies(DependencyNode node, List<Artifact> list) {
		List<DependencyNode> children = node.getChildren();

		if (children != null) {
			for (DependencyNode child : children) {
				list.add(child.getArtifact());
				addChildDependencies(child, list);
			}
		}
	}

	private File getArtifactFile(Artifact artifact) throws MojoExecutionException {
		/**
		 * Taken from https://gist.github.com/vincent-zurczak/282775f56d27e12a70d3
		 */
		DefaultArtifact aetherArtifact = new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(),
				artifact.getClassifier(), artifact.getType(), artifact.getVersion());

		return getArtifactFile(aetherArtifact);
	}

	private File getArtifactFile(DefaultArtifact aetherArtifact) throws MojoExecutionException {
		/**
		 * Taken from https://gist.github.com/vincent-zurczak/282775f56d27e12a70d3
		 */

		// We ask Maven to resolve the artifact's location.
		// It may imply downloading it from a remote repository,
		// searching the local repository or looking into the reactor's cache.

		// To achieve this, we must use Aether
		// (the dependency mechanism behind Maven).

		ArtifactRequest req = new ArtifactRequest().setRepositories(this.repositories).setArtifact(aetherArtifact);
		ArtifactResult resolutionResult;
		try {
			resolutionResult = this.repoSystem.resolveArtifact(this.repoSession, req);

		} catch (ArtifactResolutionException e) {
			throw new MojoExecutionException("Artifact " + aetherArtifact.getArtifactId() + " could not be resolved.", e);
		}

		// The file should exists, but we never know.
		File file = resolutionResult.getArtifact().getFile();
		if (file == null || !file.exists()) {
			getLog().warn("Artifact " + aetherArtifact.getArtifactId()
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