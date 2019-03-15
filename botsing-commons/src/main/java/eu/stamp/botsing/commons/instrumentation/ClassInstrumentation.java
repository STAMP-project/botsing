package eu.stamp.botsing.commons.instrumentation;

import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import org.evosuite.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ClassInstrumentation {
    private static final Logger LOG = LoggerFactory.getLogger(ClassInstrumentation.class);
    public List<Class> instrumentClasses(List<String> interestingClasses){
        List<Class> instrumentedClasses = new ArrayList<>();
        List<String> instrumentedClassesName = new ArrayList<>();

        for(String clazz: interestingClasses){
            if(instrumentedClassesName.contains(clazz)){
                continue;
            }
            LOG.debug("Instrumenting class "+ clazz);
            Class<?> cls;
            try {
                Properties.TARGET_CLASS=clazz;
                cls = Class.forName(clazz,false, BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
                instrumentedClasses.add(cls);
                instrumentedClassesName.add(clazz);
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                LOG.warn("Error in loading {}",clazz);
            }
        }
        return instrumentedClasses;
    }
}
