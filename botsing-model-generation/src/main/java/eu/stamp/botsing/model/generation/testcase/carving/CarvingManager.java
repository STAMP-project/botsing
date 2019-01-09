package eu.stamp.botsing.model.generation.testcase.carving;

import org.evosuite.Properties;
import org.evosuite.classpath.ResourceList;
import org.evosuite.testcarver.capture.FieldRegistry;
import org.evosuite.testcase.TestCase;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CarvingManager {
    private static final Logger LOG = LoggerFactory.getLogger(CarvingManager.class);

    private static CarvingManager instance = null;

    private boolean carvingFinished = false;

    private Map<Class<?>, List<TestCase>> carvedTestCases = new LinkedHashMap<>();

    private CarvingManager(){}

    public static CarvingManager getInstance(){
        if(instance == null){
            instance = new CarvingManager();
        }

        return instance;
    }

    public Map<Class<?>, List<TestCase>> getCarvedTestCases(){
        if (!carvingFinished) {
            carveTestCases();
        }

        return carvedTestCases;
    }

    private void carveTestCases() {
        List<String> testSuites = getListOftestSuites();
        // run test suites
        final org.evosuite.testcarver.extraction.CarvingClassLoader classLoader = new org.evosuite.testcarver.extraction.CarvingClassLoader();
        final List<Class<?>> junitTestClasses = new ArrayList<Class<?>>();

        final JUnitCore runner = new JUnitCore();
        final CarvingRunListener listener = new CarvingRunListener();
        runner.addListener(listener);
        // Set carving class loader
        FieldRegistry.carvingClassLoader = classLoader;

        for (String testSuiteName : testSuites) {
            String classNameWithDots = ResourceList.getClassNameFromResourcePath(testSuiteName);
            try {
                final Class<?> junitClass = classLoader.loadClass(classNameWithDots);
                junitTestClasses.add(junitClass);
            } catch (ClassNotFoundException e) {
                LOG.error("Failed to load JUnit test class {}: {}", classNameWithDots, e);
            }
        }

        final Class<?>[] classes = new Class<?>[junitTestClasses.size()];
        junitTestClasses.toArray(classes);
        Result result = runner.run(classes);
        carvedTestCases = listener.getTestCases();


        LOG.info("Result: {}/{}", result.getFailureCount(), result.getRunCount());
        for(Failure failure : result.getFailures()) {
            LOG.info("Failure: {}", failure.getMessage());
            LOG.info("Exception: {}", failure.getException());
        }




    }

    private List<String> getListOftestSuites() {
        List<String> testSuites = new LinkedList<>();
        String selectedJunitProp = Properties.SELECTED_JUNIT;
        if (selectedJunitProp == null || selectedJunitProp.trim().isEmpty()){
            throw new IllegalStateException(
                    "Properties.SELECTED_JUNIT is empty. test carving is failed.");
        }
        for (String testSuiteCP: selectedJunitProp.split(":")){
            testSuites.add(testSuiteCP.trim());
        }


        return testSuites;
    }


}
