package eu.stamp.botsing_model_generation.testcase.carving;

import eu.stamp.botsing_model_generation.BotsingTestGenerationContext;
import eu.stamp.botsing_model_generation.testcase.carving.instrument.Instrumenter;
import org.evosuite.classpath.ResourceList;
import org.evosuite.runtime.instrumentation.JSRInlinerClassVisitor;
import org.evosuite.runtime.instrumentation.RuntimeInstrumentation;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class CarvingClassLoader extends ClassLoader {
    private final static Logger logger = LoggerFactory.getLogger(CarvingClassLoader.class);
    private final Instrumenter instrumenter = new Instrumenter();
    private final ClassLoader classLoader;
    private final Map<String, Class<?>> classes = new HashMap<String, Class<?>>();

    /**
     * <p>
     * Constructor for InstrumentingClassLoader.
     * </p>
     */
    public CarvingClassLoader() {
        classLoader = CarvingClassLoader.class.getClassLoader();
    }



    /** {@inheritDoc} */
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (!RuntimeInstrumentation.checkIfCanInstrument(name)) {
            Class<?> result = findLoadedClass(name);
            if (result != null) {
                return result;
            }
            result = classLoader.loadClass(name);
            return result;

        }
        if(name.equals("eu.stamp.botsing_model_generation.testcase.carving.capture.CaptureUtil")){
            return eu.stamp.botsing_model_generation.testcase.carving.capture.CaptureUtil.class;
        }
        Class<?> result = classes.get(name);
        if (result != null) {
            return result;
        } else {

            logger.info("Seeing class for first time: " + name);
            Class<?> instrumentedClass = instrumentClass(name);
            return instrumentedClass;
        }
    }



    private Class<?> instrumentClass(String fullyQualifiedTargetClass)
            throws ClassNotFoundException {
        logger.warn("Instrumenting class '" + fullyQualifiedTargetClass + "'.");

        try {
            String className = fullyQualifiedTargetClass.replace('.', '/');

            InputStream is = ResourceList.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).getClassAsStream(fullyQualifiedTargetClass);
            if(is == null){
                throw new ClassNotFoundException("Class '" + className + ".class"
                        + "' should be in target project, but could not be found!");
            }

            ClassReader reader = new ClassReader(is);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, ClassReader.SKIP_FRAMES);
            instrumenter.transformClassNode(classNode, className);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            classNode.accept(new JSRInlinerClassVisitor(writer));
            //classNode.accept(writer);
            byte[] byteBuffer = writer.toByteArray();
            Class<?> result = defineClass(fullyQualifiedTargetClass, byteBuffer, 0,
                    byteBuffer.length);
            if(Modifier.isPrivate(result.getModifiers())) {
                logger.info("REPLACING PRIVATE CLASS "+fullyQualifiedTargetClass);
                result = super.loadClass(fullyQualifiedTargetClass);
            }
            classes.put(fullyQualifiedTargetClass, result);
            logger.info("Keeping class: " + fullyQualifiedTargetClass);
            return result;
        } catch (Throwable t) {
            logger.info("Error: " + t);
            for(StackTraceElement e : t.getStackTrace()) {
                logger.info(e.toString());
            }
            throw new ClassNotFoundException(t.getMessage(), t);
        }
    }
}
