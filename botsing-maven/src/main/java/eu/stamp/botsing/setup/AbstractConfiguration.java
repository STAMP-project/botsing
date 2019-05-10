package eu.stamp.botsing.setup;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.logging.Log;

public abstract class AbstractConfiguration {

	protected List<String> properties;
	protected Log log;
	private boolean requiredOptions = false;

	public boolean isRequiredOptions() {
		return requiredOptions;
	}

	public void setRequiredOptions(boolean requiredOptions) {
		this.requiredOptions = requiredOptions;
	}

	public List<String> getProperties() {
		return properties;
	}

	public String getOptionValue(String value) {
		return getParameterValue(value);
	}

	private String getParameterValue(String parameterName) {
		String value = null;

		for (int i = 0; i < properties.size(); i++) {
			if (properties.get(i).startsWith("-" + parameterName)) {

				// parameter stored as "-PARAM_NAME PARAM_VALUE" (two strings in
				// the list)
				value = properties.get(i + 1);
				break;

			} else if (properties.get(i).startsWith("-D" + parameterName)) {

				// parameter stored as "-DPARAM_NAME=PARAM_VALUE" (one string in
				// the list)
				int eqIndex = properties.get(i).indexOf("=");
				value = properties.get(i).substring(eqIndex + 1);
				break;
			}
		}

		return value;
	}

	public void addMandatoryProperty(String name, String value) {
		if (value != null && value.length() > 0) {
			if (properties.contains("-" + name)) {
				log.debug("Updating mandatory property '" + name + "'");
				int i = properties.indexOf("-" + name);
				// remove old value
				properties.remove(i + 1);
				// insert new value
				properties.add(i + 1, value.toString());
			} else {
				// insert parameter and value
				properties.add("-" + name);
				properties.add(value.toString());
			}
		} else {
			log.error("Trying to insert mandatory property '" + name + "' with empty value.");
			setRequiredOptions(true);
		}
	}

	protected void addOptionalProperty(String name, Object value) {
		if (value != null && value.toString().length() > 0) {
			properties.add("-D" + name + "=" + value);
		}
	}

	protected String getDependencies(String projectCP) {
		String result = "";

		if (projectCP != null) {
			result = projectCP;
			File prjFolder = new File(projectCP);
			if (prjFolder != null) {

				File dirDependency = new File(prjFolder.getParent() + File.separator + "dependency");

				if (dirDependency.exists()) {
					File[] files = dirDependency.listFiles();
					for (int i = 0; i < files.length; i++) {
						result += File.pathSeparator + prjFolder.getParent() + File.separator + "dependency"
								+ File.separator + files[i].getName();
					}
				}
			}
		}
		return result;
	}
}
