package eu.stamp.botsing_model_generation.testcase.carving;

import eu.stamp.botsing_model_generation.BotsingTestGenerationContext;
import eu.stamp.botsing_model_generation.Instrumentation.BotsingBytecodeInstrumentation;
import eu.stamp.botsing_model_generation.testcase.carving.codegeneration.CaptureLogAnalyzer;
import eu.stamp.botsing_model_generation.testcase.carving.codegeneration.ExistingTestCaseCodeGenerator;
import org.evosuite.Properties;
import org.evosuite.setup.TestUsageChecker;
import org.evosuite.testcarver.capture.CaptureLog;
import org.evosuite.testcarver.capture.Capturer;
import org.evosuite.testcarver.testcase.CarvedTestCase;
import org.evosuite.testcase.TestCase;
import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CarvingRunListener extends RunListener {
    private static final Logger LOG = LoggerFactory.getLogger(CarvingRunListener.class);
    private final Map<Class<?>, List<TestCase>> carvedTestCases = new LinkedHashMap<>();

    public Map<Class<?>, List<TestCase>> getTestCases() {
        return carvedTestCases;
    }

    // Run this when new test started
    @Override
    public void testStarted(Description description){
        Capturer.startCapture();
    }

    // Run this after finishing a test
    @Override
    public void testFinished(Description description){
        LOG.debug("DESC: "+ description.toString());

        final CaptureLog log = Capturer.stopCapture();
        LOG.info(" - Carving test {}.{}", description.getClassName(), description.getMethodName());
        this.processLog(description, log);
        Capturer.clear();
    }

    private void processLog(Description description, final CaptureLog log){
        final CaptureLogAnalyzer analyzer = new CaptureLogAnalyzer();
        final ExistingTestCaseCodeGenerator codeGen = new ExistingTestCaseCodeGenerator();
        LOG.debug("Start analyzing following log: {}",log);
        List<Class<?>> observedClasses = getObservedClasses(log);
        for(Class<?> targetClass : observedClasses) {
            if(!carvedTestCases.containsKey(targetClass)){
                carvedTestCases.put(targetClass, new ArrayList<TestCase>());
            }
            Class<?>[] targetClassesArr = {targetClass};
            analyzer.analyze(log, codeGen, targetClassesArr);
            CarvedTestCase test = (CarvedTestCase) codeGen.getCode();
            if (test == null){
                LOG.warn("Failed to carve class {}",targetClass.toString());
                codeGen.clear();
                continue;
            }
            test.setName(description.getMethodName());
            test.changeClassLoader(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
            LOG.info("Carved test is: \n {} " ,test.toCode());



            carvedTestCases.get(targetClass).add(test);
            codeGen.clear();

        }
    }


    private List<Class<?>> getObservedClasses(final CaptureLog log) {
        List<Class<?>> targetClasses = new ArrayList<Class<?>>();

        List<String> testSuites = new LinkedList<>();
        String selectedJunitProp = Properties.SELECTED_JUNIT;
        if (selectedJunitProp == null || selectedJunitProp.trim().isEmpty()){
            throw new IllegalStateException(
                    "Properties.SELECTED_JUNIT is empty. test carving is failed.");
        }
        for (String testSuiteCP: selectedJunitProp.split(":")){
            testSuites.add(testSuiteCP.trim());
        }

        // carving
        Set<String> uniqueClassesInTest = new LinkedHashSet<String>(log.getObservedClasses());
        for (String classname: uniqueClassesInTest){
            if (testSuites.contains(classname)){
                // we are collecting objects in the source code. We do not have interest on the observed used tests in another test.
                continue;
            }

            if(BotsingBytecodeInstrumentation.checkIfCanInstrument(classname)){
                try {
                    Class<?> clazz = Class.forName(classname, true, BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
                    if(TestUsageChecker.canUse(clazz) && !clazz.isArray()) {
                        if(!targetClasses.contains(clazz)) {
                            targetClasses.add(clazz);
                        }
                    }else{
                        LOG.debug("Class {} is not accessible.",classname);
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }else{
                LOG.debug("Class {} is not Instrumentable.",classname);
            }
        }



        return targetClasses;
    }
}
