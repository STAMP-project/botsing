package eu.stamp.botsing.reproduction;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.testgeneration.strategy.BotsingIndividualStrategy;
import org.evosuite.strategy.TestGenerationStrategy;

public class CrashReproductionHelper {

    public static TestGenerationStrategy getTestGenerationFactory(){
        switch (CrashProperties.testGenerationStrategy){
            case Single_GA:
                return new BotsingIndividualStrategy();
            default:
                return new BotsingIndividualStrategy();
        }
    }
}
