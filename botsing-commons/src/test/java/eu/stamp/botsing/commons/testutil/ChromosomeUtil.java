package eu.stamp.botsing.commons.testutil;

import org.evosuite.shaded.org.mockito.Mockito;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testsuite.TestSuiteChromosome;

import java.util.ArrayList;
import java.util.Iterator;

public class ChromosomeUtil {


    public static TestSuiteChromosome createTestSuiteChromosome(int size){

        TestSuiteChromosome testSuiteChromosome = new TestSuiteChromosome();

        for (int testIndex = 0; testIndex < size ; testIndex++){
            TestCase testCase = Mockito.mock(TestCase.class);
            testSuiteChromosome.addTest(testCase);
        }

        return testSuiteChromosome;
    }

    public static TestChromosome createTestChromosome(ArrayList<Integer> testSizes){
        TestChromosome chromosome = new TestChromosome();
        for (Integer testSize : testSizes) {
            TestCase testCase = Mockito.mock(TestCase.class);
            Mockito.doNothing().when(testCase).addCoveredGoal(Mockito.any());
            Iterator testIt = Mockito.mock(Iterator.class);
            Mockito.when(testIt.hasNext()).thenReturn(false);
            Mockito.when(testCase.iterator()).thenReturn(testIt);
            Mockito.when(testCase.size()).thenReturn(testSize);
            Mockito.when(testCase.clone()).thenReturn(testCase);
            chromosome.setTestCase(testCase);
        }


        return chromosome;
    }
}
