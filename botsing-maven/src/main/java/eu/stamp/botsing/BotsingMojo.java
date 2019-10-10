package eu.stamp.botsing;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
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
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;

import eu.stamp.botsing.setup.BotsingConfiguration;
import eu.stamp.botsing.setup.FileUtility;

/**
 * Mojo class to run Botsing
 *
 * @author Luca Andreatta
 */
@Mojo(name = "botsing", defaultPhase = LifecyclePhase.VERIFY, requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true)
public class BotsingMojo extends AbstractMojo {

	private enum DependencyInputType {
		FOLDER, POM, ARTIFACT
	}

	/*
	 * botsing-preprocessing parameters
	 */

	/**
	 * Package regex to specify which log lines consider for stacktrace reproduction
	 */
	@Parameter(property = "package_filter")
	private String packageFilter;

	/*
	 * botsing-reproduction parameters
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
	 * The frame level up to which parse the stack trace
	 */
	@Parameter(property = "max_target_frame")
	private Integer maxTargetFrame;

	/**
	 * Maximum value of target_frame to consider if frame is read from stacktrace file
	 */
	private static final Integer MAX_FRAME_LIMIT = 10;

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
	@Parameter(property = "global_timeout", defaultValue = "1800")
	private Integer globalTimeout;

	/**
	 * the directory where the tests are generated with a default value of
	 * `crash-reproduction-tests`
	 */
	@Parameter(property = "test_dir", defaultValue = "crash-reproduction-tests")
	private String testDir;

	/**
	 * Botsing version to use, if not specified the highest version available will be used
	 */
	@Parameter(property = "botsing_version")
	private String botsingVersion;

	/**
	 * the seed used to initialize the random number generator. This value allows to
	 * have deterministic behavior and should be set when performing evaluations
	 */
	@Parameter(property = "random_seed")
	private Long randomSeed;

	@Parameter(property = "no_runtime_dependency", defaultValue = "false")
	private String noRuntimeDependency;

	/*
	 * Parameters to get dependencies from artifactId
	 */

	/**
	 * Group id to search artifact in Maven
	 */
	@Parameter(property = "group_id")
	private String groupId;

	/**
	 * Artifact id to search artifact in Maven
	 */
	@Parameter(property = "artifact_id")
	private String artifactId;

	/**
	 * Classifier to search artifact in Maven
	 */
	@Parameter(property = "classifier")
	private String classifier;

	/**
	 * Extension to search artifact in Maven (default value is "jar")
	 */
	@Parameter(property = "extension", defaultValue = "jar")
	private String extension;

	/**
	 * Version to search artifact in Maven, if not specified the highest version available will be used
	 */
	@Parameter(property = "version")
	private String version;

	/*
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

	@Component
	private ProjectBuilder projectBuilder;

	@Override
	public void execute() throws MojoExecutionException {
		getLog().info("Starting Botsing to generate tests with EvoSuite");

		checkParameters();

		// Run botsing-preprocessing to clean crash log
		String cleanedCrashLog = null;
		try {

			// create clean stacktrace temporary file
			File cleanedCrashLogTmpFile =  File.createTempFile(crashLog, extension);
			cleanedCrashLogTmpFile.deleteOnExit();
			cleanedCrashLog = cleanedCrashLogTmpFile.getAbsolutePath();

			File botsingPreprocessingJar = getArtifactFile(
					new DefaultArtifact("eu.stamp-project", "botsing-preprocessing", "jar-with-dependencies", "jar", botsingVersion));

			// Run botsing-preprocessing
			boolean success = ProcessRunner.executeBotsingPreprocessing(project.getBasedir(), botsingPreprocessingJar,
					crashLog, cleanedCrashLog, packageFilter, globalTimeout, getLog());

			if (!success) {
				throw new MojoFailureException("Error cleaning the stacktrace.");
			}

		} catch (Exception e) {
			throw new MojoExecutionException("Error executing botsing-preprocessing", e);
		}

		// set Botsing configuration
		BotsingConfiguration configuration = new BotsingConfiguration(cleanedCrashLog, targetFrame, getDependencies(),
				population, searchBudget, globalTimeout, testDir, randomSeed, noRuntimeDependency, getLog());

		// Start botsing-reproduction
		try {

			File botsingReproductionJar = getArtifactFile(
					new DefaultArtifact("eu.stamp-project", "botsing-reproduction", "", "jar", botsingVersion));

			Integer actualTargetFrame = ProcessRunner.executeBotsingReproduction(project.getBasedir(), botsingReproductionJar,
					configuration, getMaxTargetFrame(cleanedCrashLog), getLog());

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

	private void checkParameters() throws MojoExecutionException {
		// check log file first: must be with read permissions and not empty
		// TODO check also after pre-processing!
		File f = new File(crashLog);
		if (f == null || !f.exists() || !f.isFile() || !f.canRead()) {
			throw new MojoExecutionException("Cannot read log file '" + crashLog + "'");
		}

		// TODO check other mandatory properties
	}

	/**
	 * if maxTargetFrame is not set, set maxTargetFrame from crashLog rows
	 * @return
	 * @throws MojoExecutionException
	 */
	private Integer getMaxTargetFrame(String crashLog) throws MojoExecutionException {
		if (maxTargetFrame != null) {
			return maxTargetFrame;

		} else {
			try {

				// get row number from log file
				long rowNumber = FileUtility.getRowNumber(crashLog);

				if (rowNumber > MAX_FRAME_LIMIT) {
					getLog().warn("target_frame set to " + MAX_FRAME_LIMIT + " because it exceed the maximum.");
					return MAX_FRAME_LIMIT;
				}

				// remove the first line from the count
				return (new Long(rowNumber-1)).intValue();

			} catch (IOException e) {
				throw new MojoExecutionException("Cannot read file '" + crashLog + "' line number");
			}
		}
	}

	/**
	 * Return if the dependencies should be found in a folder or from pom.xml or searching via artifactId looking at the input
	 * @return
	 */
	private DependencyInputType getDependencyType() {

		if (projectCP != null) {

			getLog().info("Reading dependencies from folder");
			return DependencyInputType.FOLDER;

		} else if (groupId != null && artifactId != null) {

			getLog().info("Reading dependencies from artifact");
			return DependencyInputType.ARTIFACT;

		} else {

			getLog().info("Reading dependencies from pom");
			return DependencyInputType.POM;
		}
	}

	/**
	 * build a list of dependencies to run Botsing
	 *
	 * @return
	 * @throws MojoExecutionException
	 */
	public String getDependencies() throws MojoExecutionException {
		String dependencies = null;

		// check where the dependency should be found
		DependencyInputType dependencyType = getDependencyType();

		if (dependencyType == DependencyInputType.FOLDER) {
			dependencies = getDependenciesFromFolder(projectCP);

		} else {
			dependencies = getDependenciesWithMaven(dependencyType);
		}

		// print dependencies for debug
		getLog().info("Collected dependencies: " + dependencies);

		return dependencies;
	}

	public String getDependenciesWithMaven(DependencyInputType dependencyType) throws MojoExecutionException {
		String result = "";

		if (dependencyType == DependencyInputType.POM) {

			// add project artifact
			result += getArtifactFile(project.getArtifact()).getAbsolutePath() + File.pathSeparator;

		} else if (dependencyType == DependencyInputType.ARTIFACT) {

			// artifact to get
			DefaultArtifact artifact = new DefaultArtifact(groupId, artifactId, classifier, extension, version);

			// add input artifact if not a war or zip file
			if (!artifact.getExtension().equals("war") || !artifact.getExtension().equals("zip")) {
				File artifactFile = getArtifactFile(artifact);
				result += artifactFile.getAbsolutePath() + File.pathSeparator;
			}

			// download pom artifact
			downloadArtifactDescriptorFile(artifact);

		} else {
			getLog().warn("Dependency type '"+dependencyType+"' not supported!");
		}

		// Add dependencies
		for (Artifact unresolvedArtifact : getDependencyTree(dependencyType)) {
			File file = getArtifactFile(unresolvedArtifact);

			result += file.getAbsolutePath() + File.pathSeparator;
		}

		return result;
	}

	public List<Artifact> getDependencyTree(DependencyInputType dependencyType) throws MojoExecutionException {
		try {
			ProjectBuildingRequest buildingRequest = getProjectbuildingRequest(dependencyType);

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

	private ProjectBuildingRequest getProjectbuildingRequest(DependencyInputType dependencyType) throws MojoExecutionException {
		ProjectBuildingRequest buildingRequest = null;

		if (dependencyType == DependencyInputType.POM) {

			// building request for project pom
			buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
			buildingRequest.setProject(project);

		} else if (dependencyType == DependencyInputType.ARTIFACT) {

			// building request for input artifact
			buildingRequest = new DefaultProjectBuildingRequest();
			buildingRequest.setProcessPlugins( false );
			buildingRequest.setRepositorySession( repoSession );
			buildingRequest.setRemoteRepositories(project.getRemoteArtifactRepositories());
			org.apache.maven.artifact.Artifact artifact = new org.apache.maven.artifact.DefaultArtifact(groupId,
					artifactId, version, "compile", extension, classifier, new DefaultArtifactHandler());

			try {
				MavenProject project = projectBuilder.build(artifact, buildingRequest).getProject();
				buildingRequest.setProject(project);

			} catch (ProjectBuildingException e) {
				throw new MojoExecutionException("Failed to build the POM for the artifact " + groupId + ":" + artifactId
						+ ":" + extension + ":" + version + ".", e);
			}

		} else {
			getLog().warn("Dependency type '"+dependencyType+"' not supported!");
		}

		return buildingRequest;
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
			// search for Highest version of the artifact if none is declared
			if (aetherArtifact.getVersion() == null || aetherArtifact.getVersion().isEmpty()) {

				VersionRangeRequest request = new VersionRangeRequest().setRepositories(this.repositories)
						.setArtifact(aetherArtifact.setVersion("(0,]"));
				VersionRangeResult versionResult = repoSystem.resolveVersionRange(repoSession, request);
				getLog().debug("Highest version found for " + aetherArtifact + " is: " + versionResult.getHighestVersion());

				// Add the artifact with the highest version to the request
				req = new ArtifactRequest().setRepositories(this.repositories)
						.setArtifact(aetherArtifact.setVersion(versionResult.getHighestVersion().toString()));
			}

			resolutionResult = this.repoSystem.resolveArtifact(this.repoSession, req);

		} catch (VersionRangeResolutionException e) {
			throw new MojoExecutionException("Latest version of artifact " + aetherArtifact.getArtifactId() + " could not be resolved.", e);

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

	private void downloadArtifactDescriptorFile(DefaultArtifact aetherArtifact) throws MojoExecutionException {

		ArtifactDescriptorRequest descReq = new ArtifactDescriptorRequest().setRepositories(this.repositories).setArtifact(aetherArtifact);
		try {
			this.repoSystem.readArtifactDescriptor(this.repoSession, descReq);

		} catch ( ArtifactDescriptorException e) {
			throw new MojoExecutionException("Artifact Descriptor for " + aetherArtifact.getArtifactId() + " could not be resolved.", e);
		}
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