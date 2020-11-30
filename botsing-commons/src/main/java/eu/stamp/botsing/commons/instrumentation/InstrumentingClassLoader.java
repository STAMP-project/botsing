package eu.stamp.botsing.commons.instrumentation;

import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import org.evosuite.Properties;
import org.evosuite.classpath.ResourceList;
import org.evosuite.runtime.instrumentation.RuntimeInstrumentation;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InstrumentingClassLoader extends ClassLoader {
    private static final Logger LOG = LoggerFactory.getLogger(InstrumentingClassLoader.class);
    private final Map<String, Class<?>> visitedClasses = new HashMap<>();

    private final BotsingBytecodeInstrumentation instrumentation;

    private final ClassLoader classLoader;
    public InstrumentingClassLoader() {
        this(new BotsingBytecodeInstrumentation());
    }

    public InstrumentingClassLoader(BotsingBytecodeInstrumentation instrumentation) {
        super(InstrumentingClassLoader.class.getClassLoader());
        classLoader = InstrumentingClassLoader.class.getClassLoader();
        this.instrumentation = instrumentation;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
//        if(!Properties.TARGET_CLASS.equals(name)){
//            Properties.TARGET_CLASS = name;
//        }
        if (!RuntimeInstrumentation.checkIfCanInstrument(name)){
            Class<?> result = visitedClasses.get(name);
            if (result != null) {
                return result;
            }
            result = classLoader.loadClass(name);
            return result;
        }
        Class<?> result = visitedClasses.get(name);
        if (result != null) {
            return result;
        } else {
            Class<?> instrumentedClass = instrumentClass(name);
            return instrumentedClass;
        }
    }

    private Class<?> instrumentClass(String fullyQualifiedTargetClass) {
        String className = fullyQualifiedTargetClass.replace('.', '/');
        InputStream is = null;
        try {
            is = ResourceList.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).getClassAsStream(fullyQualifiedTargetClass);
            if (is == null) {
                LOG.warn("Class '" + className + ".class" + "' should be in target project!");
            }
            byte[] byteBuffer = getTransformedBytes(className,is);
            createPackageDefinition(fullyQualifiedTargetClass);
            try{
                Class<?> result = defineClass(fullyQualifiedTargetClass, byteBuffer, 0,byteBuffer.length);
                visitedClasses.put(fullyQualifiedTargetClass, result);
                LOG.debug("Loaded class: " + fullyQualifiedTargetClass);
                return result;
            }catch(ClassFormatError cfe){
                return null;
            }





        } catch (Throwable t) {
            LOG.error("Error while loading class: "+t);
            return null;
        } finally {
            if(is != null){
                try {
                    is.close();
                } catch (IOException e) {
                    LOG.warn(e.getMessage());
                    return null;
                }
            }
        }
    }


    protected byte[] getTransformedBytes(String className, InputStream is) throws IOException {
        return instrumentation.transformBytes(this, className, new ClassReader(is));
    }

    private void createPackageDefinition(String className){
        int i = className.lastIndexOf('.');
        // check if className is valid
        if (i != -1) {
            String packageName = className.substring(0, i);
            // Check if package already loaded.
            Package pkg = getPackage(className.substring(0, i));
            if(pkg==null){
                // If it is not loadeed we will define it to the classloder
                definePackage(packageName, null, null, null, null, null, null, null);
                LOG.debug("Defined package (3): "+getPackage(packageName)+", "+getPackage(packageName).hashCode());
            }
        }
    }

    public Set<String> getLoadedClasses() {
        HashSet<String> loadedClasses = new HashSet<String>(this.visitedClasses.keySet());
        return loadedClasses;
    }
}