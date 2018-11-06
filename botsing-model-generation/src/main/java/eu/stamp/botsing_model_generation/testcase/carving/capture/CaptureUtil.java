package eu.stamp.botsing_model_generation.testcase.carving.capture;

import eu.stamp.botsing_model_generation.BotsingTestGenerationContext;
import org.evosuite.classpath.ResourceList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class CaptureUtil {
    private static final Logger LOG = LoggerFactory.getLogger(CaptureUtil.class);
    private CaptureUtil(){}

    public static Class<?> loadClass(final String internalClassName) {
        String className = ResourceList.getClassNameFromResourcePath(internalClassName);

        try {
            // Load class from  the botsing class loader
            return Class.forName(className,true,BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
        }catch (final ClassNotFoundException e) {
            LOG.error("class {} is not available in the botsing class loader", className);
            throw new RuntimeException(e);
        }
    }

    public static final Class<?> getClassFromDesc(final String desc) {
        return org.evosuite.testcarver.capture.CaptureUtil.getClassFromDesc(desc);
    }


}
