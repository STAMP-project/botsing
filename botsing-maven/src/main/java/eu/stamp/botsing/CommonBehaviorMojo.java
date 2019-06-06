package eu.stamp.botsing;

import java.io.File;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
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

import eu.stamp.botsing.setup.EvoSuiteConfiguration;
import eu.stamp.botsing.setup.ModelGenerationConfiguration;

@Mojo(name = "common-behavior", defaultPhase = LifecyclePhase.VERIFY, requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true)
public class CommonBehaviorMojo extends AbstractMojo {

	/**
	 * Folder with dependencies to run the project
	 */
	@Parameter(property = ModelGenerationConfiguration.PROJECT_CP_OPT)
	private String projectCP;

	/**
	 * To run the test cases that are in the package
	 */
	@Parameter(property = ModelGenerationConfiguration.PROJECT_PREFIX_OPT)
	private String projectPrefix;

	/**
	 * To specify the output directory for the models
	 */
	@Parameter(property = ModelGenerationConfiguration.OUT_DIR_OPT)
	private String outDir;

	@Parameter(property = ModelGenerationConfiguration.GLOBAL_TIMEOUT_OPT, defaultValue = "1800")
	private Integer globalTimeout;

	@Parameter(property = EvoSuiteConfiguration.CLASS_OPT)
	private String clazz;

	// optional parameters
	@Parameter(property = EvoSuiteConfiguration.TEST_DIR_OPT)
	private String testDir;

	@Parameter(property = EvoSuiteConfiguration.REPORT_DIR_OPT)
	private String reportDir;

	@Parameter(property = EvoSuiteConfiguration.ALGORITHM_OPT, defaultValue = "DynaMOSA")
	private String algorithm;

	@Parameter(property = EvoSuiteConfiguration.SEARCH_BUDGET_OPT, defaultValue = "60")
	private Integer searchBudget;

	@Parameter(property = EvoSuiteConfiguration.SEED_CLONE_OPT, defaultValue = "0.5")
	private Float seedClone;

	@Parameter(property = EvoSuiteConfiguration.ONLINE_MODEL_SEEDING_OPT, defaultValue = "TRUE")
	private Boolean onlineModelSeeding;

	@Parameter(property = EvoSuiteConfiguration.NO_RUNTIME_DEPENDENCY_OPT, defaultValue = "TRUE")
	private Boolean noRuntimeDependency;

	/**
	 * Botsing Model Generation version to use
	 */
	@Parameter(property = "model_geneation_version", defaultValue = "1.0.6-SNAPSHOT")
	private String modelGenerationVersion;

	@Parameter(property = "evosuite_master_version", defaultValue = "1.0.7")
	private String evosuiteMasterVersion;

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

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("Starting Common Behavior");

		ModelGenerationConfiguration modelGenerationConf = new ModelGenerationConfiguration(projectCP, projectPrefix,
				outDir, getLog());

		// The outDir folder contains the models
		if (testDir == null) {
			testDir = outDir + File.separator + "evosuite-test";
		}
		if (reportDir == null) {
			reportDir = outDir + File.separator + "evosuite-report";
		}

		// We are using the same classpath for both configurations
		EvoSuiteConfiguration evoSuiteConf = new EvoSuiteConfiguration(clazz, projectCP,
				outDir + File.separator + "models", testDir, reportDir, algorithm, searchBudget, seedClone,
				onlineModelSeeding, noRuntimeDependency, getLog());

		if (!modelGenerationConf.isRequiredOptions() && !evoSuiteConf.isRequiredOptions()) {

			File botsingModelGenerationJar = getArtifactFile(new DefaultArtifact("eu.stamp-project",
					"botsing-model-generation", "jar-with-dependencies", "jar", modelGenerationVersion));

			getLog().info("botsingModelGenerationJar file: " + botsingModelGenerationJar.getAbsolutePath());

			try {

				boolean success = ProcessRunner.executeBotsingModelGeneration(project.getBasedir(),
						botsingModelGenerationJar, modelGenerationConf, globalTimeout, getLog());

				if (success) {
					// get file EvoSuite jar
					File evoSuiteJar = getArtifactFile(
							new DefaultArtifact("org.evosuite", "evosuite-master", "", "jar", evosuiteMasterVersion));

					if (evoSuiteJar.exists()) {
						getLog().info("evoSuiteJar file: " + evoSuiteJar.getAbsolutePath());

						ProcessRunner.executeEvoSuite(project.getBasedir(), evoSuiteJar, evoSuiteConf, globalTimeout,
								getLog());

					} else {
						throw new MojoFailureException("EvoSuite JAR file does not exist in the repository yet");
					}
				}
			} catch (Exception e) {
				throw new MojoExecutionException("Error executing Common Behavior", e);
			}

		} else {
			throw new MojoFailureException("Error executing Common Behavior: missing required options");
		}

		getLog().info("Results generated with successful in " + outDir + " folder");
	}

	private File getArtifactFile(DefaultArtifact aetherArtifact) throws MojoExecutionException {

		ArtifactRequest req = new ArtifactRequest().setRepositories(this.repositories).setArtifact(aetherArtifact);
		ArtifactResult resolutionResult;
		try {
			resolutionResult = this.repoSystem.resolveArtifact(this.repoSession, req);

		} catch (ArtifactResolutionException e) {
			throw new MojoExecutionException("Artifact " + aetherArtifact.getArtifactId() + " could not be resolved.",
					e);
		}

		// The file should exists, but we never know.
		File file = resolutionResult.getArtifact().getFile();
		if (file == null || !file.exists()) {
			getLog().warn("Artifact " + aetherArtifact.getArtifactId()
					+ " has no attached file. Its content will not be copied in the target model directory.");
		}

		return file;
	}
}
