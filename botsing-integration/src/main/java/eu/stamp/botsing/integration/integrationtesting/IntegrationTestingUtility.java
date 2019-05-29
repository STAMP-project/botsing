package eu.stamp.botsing.integration.integrationtesting;

import eu.stamp.botsing.integration.IntegrationTestingProperties;
import org.evosuite.strategy.TestGenerationStrategy;

public class IntegrationTestingUtility {
    public static TestGenerationStrategy getTestGenerationFactory(){
        switch (IntegrationTestingProperties.searchAlgorithm){
            case MOSA:
//                return new MOSuiteStrategy();
            default:
//                return new MOSuiteStrategy();
        }
        // ToDo: implement MOSUITEStrategy and remove return null
        return null;
    }
}
