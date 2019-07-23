package eu.stamp.botsing;

import java.io.IOException;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import eu.stamp.botsing.setup.BotsingConfiguration;

public class BotsingConfigurationTest {

	private BotsingConfiguration configuration;

	@Before
	public void before() {
		Log log = new SystemStreamLog();
		configuration = new BotsingConfiguration("crash.log", 1, "bin/botsing-reproduction.jar", log);
	}

	@Test
	public void targetFrameShouldBeUpdated() throws IOException {
		configuration.addMandatoryProperty(BotsingConfiguration.TARGET_FRAME_OPT, "3");
		Assert.assertEquals(new Integer(3), configuration.getTargetFrame());
	}

	@Test
	public void targetFrameShouldBeDecreased() throws IOException {
		configuration.addMandatoryProperty(BotsingConfiguration.TARGET_FRAME_OPT, "3");
		Assert.assertEquals(new Integer(2), configuration.decreaseTargetFrame());
	}

}
