package eu.stamp.botsing.testgeneration;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import org.evosuite.TestGenerationContext;

public class TestGenerationContextUtility {

    public static ClassLoader getTestGenerationContextClassLoader(){
        if(CrashProperties.integrationTesting){
            return BotsingTestGenerationContext.getInstance().getClassLoaderForSUT();
        }else{
            return TestGenerationContext.getInstance().getClassLoaderForSUT();
        }
    }

}
