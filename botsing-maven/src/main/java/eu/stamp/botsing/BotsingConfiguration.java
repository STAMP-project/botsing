package eu.stamp.botsing;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.logging.Log;

public class BotsingConfiguration {

	/**
	 * Botsing configuration is stored in a List of String
	 */
	private List<String> properties;

	private Log log;

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

	public BotsingConfiguration(String crashLog, Integer targetFrame, String projectClasspath, Log log) {

		properties = new ArrayList<String>();
		this.log = log;

		// mandatory parameters
		addProjectClasspath(projectClasspath);
		addCrashLog(crashLog);
		addTargetFrame(targetFrame);
	}

	public BotsingConfiguration(String crashLog, Integer targetFrame, String projectClasspath, Integer population, Integer searchBudget,
			Integer globalTimeout, String testDir, Long randomSeed, String noRuntimeDependency, Log log) {

		properties = new ArrayList<String>();
		this.log = log;

		// mandatory parameters
		addProjectClasspath(projectClasspath);
		addCrashLog(crashLog);
		addTargetFrame(targetFrame);

		// optional parameters
		addOptionalProperty("population", population);
		addOptionalProperty("search_budget", searchBudget);
		addOptionalProperty("global_timeout", globalTimeout);
		addOptionalProperty("test_dir", testDir);
		addOptionalProperty("random_seed", randomSeed);
		addOptionalProperty("no_runtime_dependency", noRuntimeDependency);
	}

	public void addProjectClasspath(String projectClasspath) {
		addMandatoryProperty(BotsingConfiguration.PROJECT_CP_OPT, projectClasspath);
	}

	public void addCrashLog(String crashLog) {
		addMandatoryProperty(BotsingConfiguration.CRASH_LOG_OPT, crashLog);
	}

	public void addTargetFrame(Integer targetFrame) {
		addMandatoryProperty(BotsingConfiguration.TARGET_FRAME_OPT, targetFrame);
	}

	public void addMandatoryProperty(String name, Object value) {

		if (value != null && value.toString().length() > 0) {
			if (properties.contains("-" + name)) {
				log.debug("Updating mandatory property '" + name + "'");
				int i = properties.indexOf("-" + name);

				// remove old value
				properties.remove(i+1);

				// insert new value
				properties.add(i+1, value.toString());

			} else {

				// insert parameter and value
				properties.add("-" + name);
				properties.add(value.toString());
			}

		} else {
			log.warn("Tryng to insert mandatory property '" + name + "' with empty value.");
		}
	}

	public void addOptionalProperty(String name, Object value) {
		if (value != null && value.toString().length() > 0) {
			properties.add("-D" + name + "=" + value);
		}
	}

	public String getTestDir() {
		return getParameterValue("test_dir");
	}

	private String getParameterValue(String parameterName) {
		String value = null;

		for (int i = 0; i < properties.size(); i++) {
			if (properties.get(i).startsWith("-" + parameterName)) {

				// parameter stored as "-PARAM_NAME PARAM_VALUE" (two strings in the list)
				value = properties.get(i + 1);
				break;

			} else if (properties.get(i).startsWith("-D" + parameterName)) {

				// parameter stored as "-DPARAM_NAME=PARAM_VALUE" (one string in the list)
				int eqIndex = properties.get(i).indexOf("=");
				value = properties.get(i).substring(eqIndex + 1);
				break;
			}
		}

		return value;
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

				// parameter stored as "-PARAM_NAME PARAM_VALUE" (two strings in the list)
				value = Integer.parseInt(properties.get(i + 1));

				if (decrease) {
					value = value - 1;
					properties.add(i + 1, (value) + "");
				}

				break;

			} else if (properties.get(i).startsWith("-D" + parameterName)) {

				// parameter stored as "-DPARAM_NAME=PARAM_VALUE" (one string in the list)
				int eqIndex = properties.get(i).indexOf("=");
				value = Integer.parseInt(properties.get(i).substring(eqIndex));

				if (decrease) {
					value = value - 1;
					properties.add(i, "-D" + parameterName + "=" + value);
				}

				break;
			}
		}

		return value;
	}

	public List<String> getProperties() {
		return properties;
	}

}
