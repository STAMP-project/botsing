package eu.stamp.botsing.reproduction;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.StackTrace;
import eu.stamp.botsing.fitnessfunction.FitnessFunctionHelper;
import eu.stamp.botsing.fitnessfunction.WeightedSum;
import org.junit.Before;
import org.junit.Test;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;


import java.io.BufferedReader;

import java.io.StringReader;


import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;


public class CrashReproductionGoalFactoryTest {


    @Mock
    private FitnessFunctionHelper fitnessFunctionHelper ;

    @InjectMocks
    private CrashReproductionGoalFactory crashReproductionGoalFactory ;


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

        WeightedSum goal = Mockito.mock(WeightedSum.class);
        Mockito.when(goal.getTargetClass()).thenReturn("eu.stamp.ClassB");
        Mockito.when(goal.getTargetMethod()).thenReturn("method1");
        Mockito.doReturn(goal).when(fitnessFunctionHelper).getSingleObjective(0);
        assertEquals("class eu.stamp.botsing.fitnessfunction.WeightedSum",crashReproductionGoalFactory.getCoverageGoals().get(0).getClass().toString());


        CrashProperties.testGenerationStrategy = CrashProperties.TestGenerationStrategy.Multi_GA;
        crashReproductionGoalFactory= new CrashReproductionGoalFactory();
        assertEquals("class eu.stamp.botsing.fitnessfunction.WeightedSum",crashReproductionGoalFactory.getCoverageGoals().get(0).getClass().toString());

        CrashProperties.fitnessFunctions = new CrashProperties.FitnessFunction[]{CrashProperties.FitnessFunction.WeightedSum, CrashProperties.FitnessFunction.SimpleSum};
        crashReproductionGoalFactory= new CrashReproductionGoalFactory();
        assertEquals("class eu.stamp.botsing.fitnessfunction.WeightedSum",crashReproductionGoalFactory.getCoverageGoals().get(0).getClass().toString());
    }
}
