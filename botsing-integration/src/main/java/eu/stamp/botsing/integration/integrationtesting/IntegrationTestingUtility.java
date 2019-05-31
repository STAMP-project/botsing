package eu.stamp.botsing.integration.integrationtesting;

import eu.stamp.botsing.commons.testgeneration.strategy.MOSuiteStrategy;
import eu.stamp.botsing.integration.IntegrationTestingProperties;
import eu.stamp.botsing.integration.fitnessfunction.FitnessFunctions;
import eu.stamp.botsing.integration.testgeneration.strategy.TestGenerationUtility;
import org.evosuite.strategy.TestGenerationStrategy;

public class IntegrationTestingUtility {
    public static TestGenerationStrategy getTestGenerationFactory(){
        switch (IntegrationTestingProperties.searchAlgorithm){
            case MOSA:
                return new MOSuiteStrategy(new TestGenerationUtility(),new FitnessFunctions());
            default:
                return new MOSuiteStrategy(new TestGenerationUtility(),new FitnessFunctions());
        }
    }
}
