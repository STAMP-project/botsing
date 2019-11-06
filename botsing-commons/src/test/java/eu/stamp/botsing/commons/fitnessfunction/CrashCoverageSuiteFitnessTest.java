package eu.stamp.botsing.commons.fitnessfunction;

import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testsuite.AbstractTestSuiteChromosome;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

public class CrashCoverageSuiteFitnessTest {
    @Test
    public void testNullResult(){
        // Mock list of fitness functions
        FitnessFunctions ff = Mockito.mock(FitnessFunctions.class);
        // Spy  list of CrashCoverageSuiteFitness
        CrashCoverageSuiteFitness crashCoverageSuiteFitness = new CrashCoverageSuiteFitness(ff);
        CrashCoverageSuiteFitness spiedCrashCoverageSuiteFitness = Mockito.spy(crashCoverageSuiteFitness);
        // Mock abstractTestSuiteChromosome
        AbstractTestSuiteChromosome abstractTestSuiteChromosome = Mockito.mock(AbstractTestSuiteChromosome.class);

        Mockito.doReturn(null).when(spiedCrashCoverageSuiteFitness).runTestSuite(abstractTestSuiteChromosome);
        // Get fitness
        try{
            spiedCrashCoverageSuiteFitness.getFitness(abstractTestSuiteChromosome);
            Assert.fail("IllegalArgumentException was expected!");
        }catch(IllegalArgumentException exception){
            // do nothing
        }
    }


    @Test
    public void test0(){
        // Mock list of fitness functions
        List<TestFitnessFunction> ffList = new ArrayList<>();
        // ff1
        TestFitnessFunction ff1 = Mockito.mock(TestFitnessFunction.class);
        Mockito.doReturn(1.0).when(ff1).getFitness(Mockito.any());

        TestFitnessFunction ff2 = Mockito.mock(TestFitnessFunction.class);
        Mockito.doReturn(0.0).when(ff1).getFitness(Mockito.any());

        ffList.add(ff1);
        ffList.add(ff2);

        FitnessFunctions fitnessFunctions = Mockito.mock(FitnessFunctions.class);
        Mockito.doReturn(ffList).when(fitnessFunctions).getFitnessFunctionList();
        // Spy  list of CrashCoverageSuiteFitness
        CrashCoverageSuiteFitness crashCoverageSuiteFitness = new CrashCoverageSuiteFitness(fitnessFunctions);
        CrashCoverageSuiteFitness spiedCrashCoverageSuiteFitness = Mockito.spy(crashCoverageSuiteFitness);
        // Mock execution results
        List<ExecutionResult> results = new ArrayList<>();
        ExecutionResult result = Mockito.mock(ExecutionResult.class, Mockito.RETURNS_DEEP_STUBS);
        results.add(result);
        // Mock abstractTestSuiteChromosome
        AbstractTestSuiteChromosome abstractTestSuiteChromosome = Mockito.mock(AbstractTestSuiteChromosome.class);
        Mockito.doReturn(1).when(abstractTestSuiteChromosome).size();

        Mockito.doReturn(results).when(spiedCrashCoverageSuiteFitness).runTestSuite(abstractTestSuiteChromosome);

        // Get fitness
        double fitness = spiedCrashCoverageSuiteFitness.getFitness(abstractTestSuiteChromosome);
        Assert.assertEquals(0.0, fitness, 0);


    }
}
