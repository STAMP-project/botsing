package eu.stamp.botsing.commons;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class BotsingTestGenerationContextTest {



    @Test
    public void testExecutingSUT(){
        BotsingTestGenerationContext.getInstance().goingToExecuteSUTCode();
        assertEquals(Thread.currentThread().getContextClassLoader(),BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());

        BotsingTestGenerationContext.getInstance().doneWithExecutingSUTCode();
        assertEquals(Thread.currentThread().getContextClassLoader(),BotsingTestGenerationContext.getInstance().getClass().getClassLoader());

    }
}
