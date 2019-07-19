package eu.stamp.botsing.setup;

import java.util.ArrayList;

import org.apache.maven.plugin.logging.Log;

public class BotsingConfiguration extends AbstractConfiguration {

	/**
	 * To see all the properties available take a look at
	 * org.evosuite.Properties.java
	 */

	/**
	 * Properties taken from eu.stamp.botsing.CommandLineParameters.java
	 */
	public static final String PROJECT_CP_OPT = "project_cp";
	public static final String CRASH_LOG_OPT = "crash_log";
	public static final String TARGET_FRAME_OPT = "target_frame";
	public static final String MAX_TARGET_FRAME_OPT = "max_target_frame";

	// optional parameters
	public static final String POPULATION_OPT = "population";
	public static final String SEARCH_BUDGET_OPT = "search_budget";
	public static final String GLOBAL_TIMEOUT_OPT = "global_timeout";
	public static final String TEST_DIR_OPT = "test_dir";
	public static final String RANDOM_SEED_OPT = "random_seed";
	public static final String NO_RUNTIME_DEPENDENCY_OPT = "no_runtime_dependency";

	public BotsingConfiguration(String crashLog, Integer targetFrame, String projectClasspath, Log log) {

		properties = new ArrayList<String>();
		this.log = log;

		// mandatory parameters
		addMandatoryProperty(BotsingConfiguration.PROJECT_CP_OPT, projectClasspath);
		addMandatoryProperty(BotsingConfiguration.CRASH_LOG_OPT, crashLog);
		addMandatoryProperty(BotsingConfiguration.TARGET_FRAME_OPT, targetFrame.toString());
	}

	public BotsingConfiguration(String crashLog, Integer targetFrame, String projectClasspath, Integer population,
			Integer searchBudget, Integer globalTimeout, String testDir, Long randomSeed, String noRuntimeDependency,
			Log log) {

		properties = new ArrayList<String>();
		this.log = log;

		// mandatory parameters
		addMandatoryProperty(BotsingConfiguration.PROJECT_CP_OPT, projectClasspath);
		addMandatoryProperty(BotsingConfiguration.CRASH_LOG_OPT, crashLog);
		addMandatoryProperty(BotsingConfiguration.TARGET_FRAME_OPT, targetFrame.toString());

		// add D optional parameters
		addDProperty(BotsingConfiguration.POPULATION_OPT, population.toString());
		addDProperty(BotsingConfiguration.SEARCH_BUDGET_OPT, searchBudget.toString());
		addDProperty(BotsingConfiguration.GLOBAL_TIMEOUT_OPT, globalTimeout.toString());
		addDProperty(BotsingConfiguration.TEST_DIR_OPT, testDir);
		addDProperty(BotsingConfiguration.RANDOM_SEED_OPT, randomSeed.toString());
		addDProperty(BotsingConfiguration.NO_RUNTIME_DEPENDENCY_OPT, noRuntimeDependency);
	}

	public Integer getGlobalTimeout() {
		return getOrDecreaseParameterValue(BotsingConfiguration.GLOBAL_TIMEOUT_OPT, false);
	}

	public Integer getTargetFrame() {
		return getOrDecreaseParameterValue(TARGET_FRAME_OPT, false);
	}

	public Integer decreaseTargetFrame() {
		return getOrDecreaseParameterValue(TARGET_FRAME_OPT, true);
	}

	private Integer getOrDecreaseParameterValue(String parameterName, boolean decrease) {
		Integer value = null;

		for (int i = 0; i < properties.size(); i++) {
			if (properties.get(i).startsWith("-" + parameterName)) {

				// parameter stored as "-PARAM_NAME PARAM_VALUE" (two strings in
				// the list)
				value = Integer.parseInt(properties.get(i + 1));

				if (decrease) {
					value = value - 1;
					properties.remove(i + 1);
					properties.add(i + 1, (value) + "");
				}

				break;

			} else if (properties.get(i).startsWith("-D" + parameterName)) {

				// parameter stored as "-DPARAM_NAME=PARAM_VALUE" (one string in
				// the list)
				int eqIndex = properties.get(i).indexOf("=");
				value = Integer.parseInt(properties.get(i).substring(eqIndex + 1));

				if (decrease) {
					value = value - 1;
					properties.remove(i);
					properties.add(i, "-D" + parameterName + "=" + value);
				}

				break;
			}
		}

		return value;
	}

}
