package eu.stamp.botsing.reproduction;

import static org.junit.Assert.assertEquals;

import org.evosuite.strategy.TestGenerationStrategy;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CrashReproductionHelperTest {

    private static final Logger LOG = LoggerFactory.getLogger(CrashReproductionHelperTest.class);

    @Rule
    public TestRule watcher = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            LOG.info(String.format("Starting test: %s()...",
                    description.getMethodName()));
        }
    };

    @Test
    public void testGetTestGenerationFactory() {
        CrashReproductionHelper helper = new CrashReproductionHelper();
//        CrashProperties.testGenerationStrategy = CrashProperties.TestGenerationStrategy.Single_GA;
        TestGenerationStrategy strategy = CrashReproductionHelper.getTestGenerationFactory();
        String strategyClassName = strategy.getClass().toString().substring(strategy.getClass().toString().indexOf("class ") + 6);
        assertEquals("eu.stamp.botsing.testgeneration.strategy.BotsingIndividualStrategy", strategyClassName);

        // ToDo: We should update this test with the acceptance of list of crashes feature
//        CrashProperties.testGenerationStrategy = CrashProperties.TestGenerationStrategy.Multi_GA;
        strategy = CrashReproductionHelper.getTestGenerationFactory();
        strategyClassName = strategy.getClass().toString().substring(strategy.getClass().toString().indexOf("class ") + 6);
        // TODO: After implementing multi objectivization we should change this assertion
        assertEquals("eu.stamp.botsing.testgeneration.strategy.BotsingIndividualStrategy", strategyClassName);
    }
}
