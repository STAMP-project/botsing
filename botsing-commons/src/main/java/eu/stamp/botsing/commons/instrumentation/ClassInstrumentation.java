package eu.stamp.botsing.commons.instrumentation;

import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ClassInstrumentation {
    private static final Logger LOG = LoggerFactory.getLogger(ClassInstrumentation.class);
    public List<Class> instrumentClasses(List<String> interestingClasses){
        List<Class> instrumentedClasses = new ArrayList<>();
        for(String clazz: interestingClasses){
            LOG.debug("Instrumenting class "+ clazz);
            Class<?> cls;
            try {
                cls = Class.forName(clazz,false, BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
                instrumentedClasses.add(cls);
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                LOG.warn("Error in loading {}",clazz);
            }
        }
        return instrumentedClasses;
    }
}
