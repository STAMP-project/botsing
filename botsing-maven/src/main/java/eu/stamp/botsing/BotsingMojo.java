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
	 * Properties taken from eu.stamp.botsing.CommandLineParameters.java
	 */
	private static final String PROJECT_CP_OPT = "project_cp";
	private static final String CRASH_LOG_OPT = "crash_log";
	private static final String TARGET_FRAME_OPT = "target_frame";

	/**
	 * To see all the properties available take a look at org.evosuite.Properties.java
	 */

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
	* the size of the population that evolves during the search with a default value of 100
	*/
	@Parameter(property = "population")
	private Integer population;

	/**
	* the search budget in seconds with a default value of 1800
	*/
	@Parameter(property = "search_budget")
	private Integer searchBudget;

	/**
	* the global timeout in seconds, after which the execution stops if the search is
	* stuck with a default value of 1800 (the timeout is only reached if the search does not improve after 1800 seconds)
	*/
	@Parameter(property = "global_timeout")
	private Integer globalTimeout;

	/**
	* the directory where the tests are generated with a default value of `crashreproduction-tests`
	*/
	@Parameter(property = "test_dir")
	private String testDir;

	/**
	 * Botsing version to use
	 */
	@Parameter(property = "botsing_version", defaultValue="1.0.5-SNAPSHOT")
	private String botsingVersion;

	/**
	* the seed used to initialize the random number generator. This value allows to have deterministic
	* behavior and should be set when performing evaluations
	*/
	@Parameter(property = "random_seed")
	private Long randomSeed;


	@Parameter(property = "no_runtime_dependency", defaultValue="false")
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

	@Parameter( defaultValue = "${session}", required = true, readonly = true )
	private MavenSession session;

	@Component
	private RepositorySystem repoSystem;

	/**
	 * Contains the full list of projects in the reactor.
	 */
	@Parameter( defaultValue = "${reactorProjects}", readonly = true, required = true )
	private List<MavenProject> reactorProjects;

	/**
	 * The dependency tree builder to use.
	 */
	@Component( hint = "default" )
	private DependencyGraphBuilder dependencyGraphBuilder;

	@Override
	public void execute() throws MojoExecutionException {
		getLog().info("Starting Botsing to generate tests with EvoSuite");

		// get properties
		List<String> properties = getPropertyList();

		// build dependencies List
		String dependencies = null;
		if (projectCP != null) {
			dependencies = getDependenciesFromFolder(projectCP);
		} else {
			dependencies = getDependenciesFromPom();
		}

		// add dependencies
		getLog().debug("dependencies: " + dependencies);
		properties.add("-"+PROJECT_CP_OPT);
		properties.add(dependencies);

		// Start Botsing
		try {
			// TODO Maven build succeeds while botsing fails
			// see issue https://github.com/STAMP-project/botsing/issues/72

			// TODO find a way to get the latest version of botsing-reproduction
			// tried "[1.0.4, )" for version but
			// Could not find artifact eu.stamp-project:botsing-reproduction:jar:[1.0.4, ) in central (https://repo.maven.apache.org/maven2) -> [Help 1]

			File botsingReproductionJar = getArtifactFile(
					new DefaultArtifact("eu.stamp-project", "botsing-reproduction", "", "jar", botsingVersion));

			ProcessRunner.executeBotsing(project.getBasedir(), botsingReproductionJar, properties, getLog());

		} catch (Exception e) {
			throw new MojoExecutionException("Error executing Botsing", e);
		}

		getLog().info("Stopping Botsing");
	}

	private List<String> getPropertyList() {
		List<String> result = new ArrayList<String>();

		// mandatory parameters
		result.add("-" + CRASH_LOG_OPT);
		result.add(crashLog);

		result.add("-" + TARGET_FRAME_OPT);
		result.add(targetFrame + "");

		// optional parameters
		if (population != null) {
			result.add("-Dpopulation=" + population);
		}

		if (searchBudget != null) {
			result.add("-Dsearch_budget=" + searchBudget);
		}

		if (globalTimeout != null) {
			result.add("-Dglobal_timeout=" + globalTimeout);
		}

		if (testDir != null) {
			result.add("-Dtest_dir=" + testDir);
		}

		if (randomSeed != null) {
			result.add("-Drandom_seed=" + randomSeed);
		}

		if (noRuntimeDependency != null) {
			result.add("-Dno_runtime_dependency=" + noRuntimeDependency);
		}

		return result;
	}

	public String getDependenciesFromPom() throws MojoExecutionException {
		String result = "";

		// Add ./target/classes
		result += project.getModel().getBuild().getDirectory() + File.separator + "classes" + File.pathSeparator;

		// Add ./target/test-classes
		String testClasses = project.getModel().getBuild().getDirectory() + File.separator + "test-classes";
		if (new File(testClasses).exists()) {
			result += testClasses + File.pathSeparator;
		}

		// Add pom project dependencies
		for (Artifact unresolvedArtifact : getDependencyTree()) {
			File file = getArtifactFile(unresolvedArtifact);

			result += file.getAbsolutePath() + File.pathSeparator;
		}

		return result;
	}

	public List<Artifact> getDependencyTree() throws MojoExecutionException {
		try {
			ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
			buildingRequest.setProject( project );

			// TODO check if it is necessary to specify an artifact filter
			DependencyNode rootNode = dependencyGraphBuilder.buildDependencyGraph( buildingRequest, null, reactorProjects );

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
		DefaultArtifact aetherArtifact = new DefaultArtifact(artifact.getGroupId(),
				artifact.getArtifactId(), artifact.getClassifier(), artifact.getType(), artifact.getVersion());

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