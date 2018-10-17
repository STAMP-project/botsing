package eu.stamp.botsing.reproduction;

import eu.stamp.botsing.CrashProperties;
import org.evosuite.strategy.TestGenerationStrategy;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CrashReproductionHelperTest {

    @Test
    public void testGetTestGenerationFactory(){
        CrashReproductionHelper helper = new CrashReproductionHelper();
        CrashProperties.testGenerationStrategy = CrashProperties.TestGenerationStrategy.Single_GA;
        TestGenerationStrategy strategy = helper.getTestGenerationFactory();
        String strategyClassName = strategy.getClass().toString().substring(strategy.getClass().toString().indexOf("class ")+6);
        assertEquals("eu.stamp.botsing.testgeneration.strategy.BotsingIndividualStrategy",strategyClassName);

        CrashProperties.testGenerationStrategy = CrashProperties.TestGenerationStrategy.Multi_GA;
        strategy = helper.getTestGenerationFactory();
        strategyClassName = strategy.getClass().toString().substring(strategy.getClass().toString().indexOf("class ")+6);
        // TODO: After implementing multi objectivization we should change this assertion
        assertEquals("eu.stamp.botsing.testgeneration.strategy.BotsingIndividualStrategy",strategyClassName);
    }
}
