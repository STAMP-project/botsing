package eu.stamp.botsing;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.logging.Log;

public class BotsingConfiguration {

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

	public Integer getTargetFrame() {

		return getOrDecreaseTargetFrame(false);
	}

	public Integer decreaseTargetFrame() {

		return getOrDecreaseTargetFrame(true);
	}

	private Integer getOrDecreaseTargetFrame(boolean decrease) {

		Integer targetFrame = null;

		for (int i = 0; i < properties.size(); i++) {
			if (properties.get(i).startsWith("-" + TARGET_FRAME_OPT)) {
				targetFrame = Integer.parseInt(properties.get(i + 1));

				if (decrease) {
					targetFrame = targetFrame - 1;
					properties.add(i + 1, (targetFrame) + "");
				}

				break;
			}
		}

		return targetFrame;
	}

	public List<String> getProperties() {
		return properties;
	}

}
