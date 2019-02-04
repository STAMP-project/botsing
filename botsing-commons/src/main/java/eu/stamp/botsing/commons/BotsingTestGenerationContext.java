package eu.stamp.botsing.commons;

import eu.stamp.botsing.commons.instrumentation.InstrumentingClassLoader;


public class BotsingTestGenerationContext {

    private static final BotsingTestGenerationContext instance = new BotsingTestGenerationContext();

    private InstrumentingClassLoader classLoader;
    private ClassLoader originalClassLoader;

    private BotsingTestGenerationContext(){
        originalClassLoader = this.getClass().getClassLoader();
        classLoader = new InstrumentingClassLoader();
    }


    public static BotsingTestGenerationContext getInstance() {
        return instance;
    }

    public void goingToExecuteSUTCode() {

        Thread.currentThread().setContextClassLoader(classLoader);
    }


    public void doneWithExecutingSUTCode() {
        Thread.currentThread().setContextClassLoader(originalClassLoader);
    }

    public InstrumentingClassLoader getClassLoaderForSUT() {
        return classLoader;
    }
}
