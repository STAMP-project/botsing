package eu.stamp.cbc.extraction;

import eu.stamp.cbc.instrument.CBClassAdapter;
import eu.stamp.cbc.instrument.Instrumenter;
import org.evosuite.TestGenerationContext;
import org.evosuite.classpath.ResourceList;
import org.evosuite.runtime.instrumentation.RuntimeInstrumentation;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ClassLoader  extends java.lang.ClassLoader {
    private static final Logger LOG = LoggerFactory.getLogger(ClassLoader.class);

    final java.lang.ClassLoader javaClassLoader = ClassLoader.class.getClassLoader();
    private final Map<String, Class<?>> instrumentedClasses = new HashMap<>();
    private final Instrumenter instrumenter = new Instrumenter();


    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {

        if (!RuntimeInstrumentation.checkIfCanInstrument(name)) {
            LOG.debug("Class {} cannot be instrumented!",name);
            Class<?> clazz = findLoadedClass(name);
            if(clazz == null){
                return javaClassLoader.loadClass(name);
            }

            return clazz;
        }

        Class<?> result = instrumentedClasses.get(name);

        if (result != null) {
            return result;
        }

        LOG.info("Class {} is fresh and ready to be instrumented! " , name);
        Class<?> instrumentedClass = instrument(name);
        return instrumentedClass;
    }

    private Class<?> instrument(String dottedClassName){
        LOG.debug("Instrumenting class {}",dottedClassName);


        try{
            // get class as stream
            String internalClassName = dottedClassName.replace('.', '/');
            InputStream inputStream = ResourceList.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT()).getClassAsStream(internalClassName);

            if(inputStream == null){
                throw new ClassNotFoundException("Could not find class '" + internalClassName);
            }

            ClassReader reader = new ClassReader(inputStream);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, ClassReader.SKIP_FRAMES);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

            classNode.accept(new CBClassAdapter(writer,classNode.name));

            byte[] byteBuffer = writer.toByteArray();

            Class<?> result = defineClass(dottedClassName, byteBuffer, 0,
                    byteBuffer.length);
            instrumentedClasses.put(dottedClassName, result);
            return result;

        } catch (Throwable t) {
            LOG.debug("Error: " + t);
            for(StackTraceElement e : t.getStackTrace()) {
                LOG.debug(e.toString());
            }
        }
        return null;
    }



}
