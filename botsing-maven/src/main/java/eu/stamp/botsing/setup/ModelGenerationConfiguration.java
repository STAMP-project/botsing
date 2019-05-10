package eu.stamp.botsing.setup;

import java.util.ArrayList;

import org.apache.maven.plugin.logging.Log;

public class ModelGenerationConfiguration extends AbstractConfiguration {

	/**
	 * Properties taken from CommandLine
	 */
	public static final String PROJECT_CP_OPT = "project_cp";
	public static final String PROJECT_PREFIX_OPT = "project_prefix";
	public static final String OUT_DIR_OPT = "out_dir";

	public ModelGenerationConfiguration(String projectClasspath, String projectPrefix, String outDir, Log log) {

		this.properties = new ArrayList<String>();
		this.log = log;

		// mandatory parameters
		addMandatoryProperty(ModelGenerationConfiguration.PROJECT_CP_OPT, getDependencies(projectClasspath));
		addMandatoryProperty(ModelGenerationConfiguration.PROJECT_PREFIX_OPT, projectPrefix);
		addMandatoryProperty(ModelGenerationConfiguration.OUT_DIR_OPT, outDir);

	}

}
