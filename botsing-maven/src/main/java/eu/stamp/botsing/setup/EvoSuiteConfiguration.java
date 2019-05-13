package eu.stamp.botsing.setup;

import java.util.ArrayList;

import org.apache.maven.plugin.logging.Log;

public class EvoSuiteConfiguration extends AbstractConfiguration {

	/**
	 * Properties taken from CommandLine
	 */

	public static final String EVO_SUITE_JAR = "evosuite";

	public static final String CLASS_OPT = "class";
	public static final String PROJECT_CP_OPT = "projectCP";
	public static final String MODEL_PATH_OPT = "model_path";

	public static final String TEST_DIR_OPT = "test_dir";
	public static final String REPORT_DIR_OPT = "report_dir";

	// default
	public static final String ALGORITHM_OPT = "algorithm";
	public static final String SEARCH_BUDGET_OPT = "search_budget";
	public static final String SEED_CLONE_OPT = "seed_clone";
	public static final String ONLINE_MODEL_SEEDING_OPT = "online_model_seeding";
	public static final String NO_RUNTIME_DEPENDENCY_OPT = "no_runtime_dependency";

	public EvoSuiteConfiguration(String clazz, String projectClasspath, String modelPath, String testDir,
			String reportDir, String algorithm, Integer searchBudget, Float seedClone, Boolean onlineModelSeeding,
			Boolean noRuntimeDependency, Log log) {

		this.properties = new ArrayList<String>();
		this.log = log;

		// mandatory parameters
		addMandatoryProperty(EvoSuiteConfiguration.CLASS_OPT, clazz);
		addMandatoryProperty(EvoSuiteConfiguration.PROJECT_CP_OPT, getDependencies(projectClasspath));

		addDProperty(EvoSuiteConfiguration.MODEL_PATH_OPT, modelPath);
		addDProperty(EvoSuiteConfiguration.TEST_DIR_OPT, testDir);
		addDProperty(EvoSuiteConfiguration.REPORT_DIR_OPT, reportDir);

		// default (if not provided)
		addDProperty(EvoSuiteConfiguration.ALGORITHM_OPT, algorithm);
		addDProperty(EvoSuiteConfiguration.SEARCH_BUDGET_OPT, searchBudget.toString());
		addDProperty(EvoSuiteConfiguration.SEED_CLONE_OPT, seedClone.toString());
		addDProperty(EvoSuiteConfiguration.ONLINE_MODEL_SEEDING_OPT, onlineModelSeeding.toString());
		addDProperty(EvoSuiteConfiguration.NO_RUNTIME_DEPENDENCY_OPT, noRuntimeDependency.toString());

	}

}
