package eu.stamp.botsing.reproduction;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.StackTrace;
import eu.stamp.botsing.fitnessfunction.FitnessFunctionHelper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.BufferedReader;

import java.io.StringReader;


import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;


public class CrashReproductionGoalFactoryTest {

    private static final Logger LOG = LoggerFactory.getLogger(CrashReproductionGoalFactoryTest.class);

    @Rule
    public TestRule watcher = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            LOG.info(String.format("Starting test: %s()...",
                    description.getMethodName()));
        }
    };

    @Mock
    private FitnessFunctionHelper fitnessFunctionHelper;

    @InjectMocks
    private CrashReproductionGoalFactory crashReproductionGoalFactory;


    @Before
    public void setUp() throws Exception {
        BufferedReader givenStackTrace = new BufferedReader(new StringReader("java.lang.IllegalArgumentException:\n" +
                "\tat eu.stamp.ClassA.method2(ClassA.java:10)\n" +
                "\tat eu.stamp.ClassB.method1(ClassB.java:20)"));
        StackTrace target = Mockito.spy(new StackTrace());
        Mockito.doReturn(givenStackTrace).when(target).readFromFile(anyString());
        target.setup("", 2);
        CrashProperties.getInstance().setupStackTrace(target);
        CrashProperties.testGenerationStrategy = CrashProperties.TestGenerationStrategy.Single_GA;


        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testCrashReproductionGoalFactory() {

        assertEquals("class eu.stamp.botsing.fitnessfunction.WeightedSum", crashReproductionGoalFactory.getCoverageGoals().get(0).getClass().toString());

        CrashProperties.testGenerationStrategy = CrashProperties.TestGenerationStrategy.Multi_GA;
        crashReproductionGoalFactory = new CrashReproductionGoalFactory();
        assertEquals("class eu.stamp.botsing.fitnessfunction.WeightedSum", crashReproductionGoalFactory.getCoverageGoals().get(0).getClass().toString());

        CrashProperties.fitnessFunctions = new CrashProperties.FitnessFunction[]{CrashProperties.FitnessFunction.WeightedSum, CrashProperties.FitnessFunction.LineDistance, CrashProperties.FitnessFunction.ExceptionDistance, CrashProperties.FitnessFunction.TraceDistance};
        crashReproductionGoalFactory = new CrashReproductionGoalFactory();
        assertEquals("class eu.stamp.botsing.fitnessfunction.WeightedSum", crashReproductionGoalFactory.getCoverageGoals().get(0).getClass().toString());
    }
}