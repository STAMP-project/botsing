package eu.stamp.botsing_model_generation.testcase.carving.capture;

import eu.stamp.botsing_model_generation.BotsingTestGenerationContext;
import org.evosuite.classpath.ResourceList;
import org.objectweb.asm.Type;


public final class CaptureUtil {

    private CaptureUtil(){}

    public static Class<?> loadClass(final String internalClassName) {
        final String className = ResourceList.getClassNameFromResourcePath(internalClassName);

        try {
            return Class.forName(className,true,BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
        }catch (final ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Class<?> getClassFromDesc(final String desc) {
        return org.evosuite.testcarver.capture.CaptureUtil.getClassFromDesc(desc);
    }


}
