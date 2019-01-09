package eu.stamp.botsing.model.generation.testcase.carving;

import eu.stamp.botsing.model.generation.BotsingTestGenerationContext;
import eu.stamp.botsing.model.generation.instrumentation.BotsingBytecodeInstrumentation;
import eu.stamp.botsing.model.generation.testusage.TestUsagePoolManager;
import org.evosuite.Properties;
import org.evosuite.testcarver.capture.CaptureLog;
import org.evosuite.testcarver.capture.Capturer;
import org.evosuite.testcarver.codegen.CaptureLogAnalyzer;
import org.evosuite.testcarver.testcase.CarvedTestCase;
import org.evosuite.testcarver.testcase.EvoTestCaseCodeGenerator;
import org.evosuite.testcase.TestCase;
import org.evosuite.utils.generic.GenericTypeInference;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

public class CarvingRunListener extends org.evosuite.testcarver.extraction.CarvingRunListener {
    private static final Logger LOG = LoggerFactory.getLogger(CarvingRunListener.class);
    private final Map<Class<?>, List<TestCase>> carvedTests = new LinkedHashMap<>();
    @Override
    public void testFinished(Description description) throws Exception {
        final CaptureLog log = Capturer.stopCapture();
        LOG.info("Carving test {}.{}", description.getClassName(), description.getMethodName());
        List<Class<?>> observedClasses = this.processLog(description, log);
        for(Class<?> clazz : observedClasses){
            TestUsagePoolManager.getInstance().addTest(clazz.getName(), description.getClassName());
        }

        Capturer.clear();
    }


    private List<Class<?>> processLog(Description description, final CaptureLog log) {
        LOG.debug("Current log: "+log);
        List<Class<?>> observedClasses = getObservedClasses(log);

        final CaptureLogAnalyzer analyzer = new CaptureLogAnalyzer();
        final EvoTestCaseCodeGenerator codeGen = new EvoTestCaseCodeGenerator();
        for(Class<?> observedClass : observedClasses) {
            Class<?>[] targetClasses = new Class<?>[1];
            targetClasses[0] = observedClass;
            if(!carvedTests.containsKey(observedClass)){
                carvedTests.put(observedClass, new ArrayList<TestCase>());
            }

            analyzer.analyze(log, codeGen, targetClasses);
            CarvedTestCase carvedTest = (CarvedTestCase) codeGen.getCode();

            if(carvedTest == null) {
                LOG.debug("Failed to carve test for "+Arrays.asList(targetClasses));
                codeGen.clear();
                continue;
            }

            carvedTest.setName(description.getMethodName());

            try {
                carvedTest.changeClassLoader(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
                GenericTypeInference inference = new GenericTypeInference();
                inference.inferTypes(carvedTest);

                carvedTests.get(observedClass).add(carvedTest);
            }catch (Throwable t) {
                LOG.info("Exception during carving: " + t);
                for(StackTraceElement elem : t.getStackTrace()) {
                    LOG.info(elem.toString());
                }
                LOG.info(carvedTest.toCode());
            }
            codeGen.clear();
        }
        return observedClasses;
    }


    private List<Class<?>> getObservedClasses(final CaptureLog log){
        List<Class<?>> observedClasses = new ArrayList<>();
        String[] testSuitePaths = Properties.SELECTED_JUNIT.split(":");
        List<String> jUnitClassesNames = new ArrayList<>();
        for (String s : testSuitePaths) {
            jUnitClassesNames.add(s.trim());
        }
        Set<String> uniqueObservedClasses = new LinkedHashSet<String>(log.getObservedClasses());

        for(String className : uniqueObservedClasses) {
            if (!jUnitClassesNames.contains(className) && BotsingBytecodeInstrumentation.checkIfCanInstrument(className)) {
                    try {
                        Class<?> clazz = Class.forName(className, true, BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
                        if(!observedClasses.contains(clazz)){
                            observedClasses.add(clazz);
                        }
                    } catch(ClassNotFoundException e) {
                        LOG.info("Error in instrumenting class "+className+" after carving: "+e);
                    }
            }
        }
        return observedClasses;
    }

    @Override
    public Map<Class<?>, List<TestCase>> getTestCases() {
        return carvedTests;
    }

}
