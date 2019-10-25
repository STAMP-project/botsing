package eu.stamp.botsing.commons.ga.strategy.mosa;

import eu.stamp.botsing.commons.fitnessfunction.FitnessFunctions;
import eu.stamp.botsing.commons.ga.strategy.operators.Mutation;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.operators.crossover.CrossOverFunction;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MosaTest {


    @Test
    public void testFullFillAllGoals() {

        // Mock chromosome factory
        ChromosomeFactory factory = Mockito.mock(ChromosomeFactory.class);
        TestChromosome ch1 = Mockito.mock(TestChromosome.class,  Mockito.RETURNS_DEEP_STUBS);
        TestChromosome ch2 = Mockito.mock(TestChromosome.class);


        TestCase testCase = Mockito.mock(TestCase.class);
        Mockito.doNothing().when(testCase).addCoveredGoal(Mockito.any());
        Iterator testIt = Mockito.mock(Iterator.class);
        Mockito.when(testIt.hasNext()).thenReturn(false);
        Mockito.when(testCase.iterator()).thenReturn(testIt);
        Mockito.when(ch1.getTestCase()).thenReturn(testCase);
        Mockito.when(ch1.size()).thenReturn(10);
        Mockito.when(ch1.clone()).thenReturn(ch1);

        Mockito.doNothing().when(testCase).addCoveredGoal(Mockito.any());
        Mockito.when(ch2.getTestCase()).thenReturn(testCase);
        Mockito.when(ch2.size()).thenReturn(9);
        Mockito.when(ch2.clone()).thenReturn(ch2);

        Mockito.when(factory.getChromosome()).thenReturn(ch1).thenReturn(ch2);



        // Mock list of fitness functions
        List<TestFitnessFunction> ffList = new ArrayList<>();
        // ff1
        TestFitnessFunction ff1 = Mockito.mock(TestFitnessFunction.class);
        Mockito.doReturn(0.0).when(ff1).getFitness(Mockito.any());

        TestFitnessFunction ff2 = Mockito.mock(TestFitnessFunction.class);
        Mockito.doReturn(0.0).when(ff1).getFitness(Mockito.any());

        ffList.add(ff1);
        ffList.add(ff2);

        FitnessFunctions fitnessFunctions = Mockito.mock(FitnessFunctions.class);
        Mockito.doReturn(ffList).when(fitnessFunctions).getFitnessFunctionList();




        // Mock crossover operator
        CrossOverFunction crossOver =  Mockito.mock(CrossOverFunction.class);
//        Mockito.doNothing().when(crossOver).crossOver(Mockito.any(),Mockito.any());

        // Mock mutation operator
        Mutation mutation =  Mockito.mock(Mutation.class);

        AbstractMOSA MOSA = new MOSA(factory,crossOver,mutation,fitnessFunctions);
        MOSA.addFitnessFunctions(ffList);

        MOSA.generateSolution();
    }

    @Test
    public void testFullFillOneGoal() {
        // Mock chromosome factory
        ChromosomeFactory factory = Mockito.mock(ChromosomeFactory.class);
        TestChromosome ch1 = Mockito.mock(TestChromosome.class,  Mockito.RETURNS_DEEP_STUBS);
        TestChromosome ch2 = Mockito.mock(TestChromosome.class);


        TestCase testCase = Mockito.mock(TestCase.class);
        Mockito.doNothing().when(testCase).addCoveredGoal(Mockito.any());
        Iterator testIt = Mockito.mock(Iterator.class);
        Mockito.when(testIt.hasNext()).thenReturn(false);
        Mockito.when(testCase.iterator()).thenReturn(testIt);
        Mockito.when(ch1.getTestCase()).thenReturn(testCase);
        Mockito.when(ch1.size()).thenReturn(10);
        Mockito.when(ch1.clone()).thenReturn(ch1);

        Mockito.doNothing().when(testCase).addCoveredGoal(Mockito.any());
        Mockito.when(ch2.getTestCase()).thenReturn(testCase);
        Mockito.when(ch2.size()).thenReturn(9);
        Mockito.when(ch2.clone()).thenReturn(ch2);

        Mockito.when(factory.getChromosome()).thenReturn(ch1).thenReturn(ch2);



        // Mock list of fitness functions
        List<TestFitnessFunction> ffList = new ArrayList<>();
        // ff1
        TestFitnessFunction ff1 = Mockito.mock(TestFitnessFunction.class);
        Mockito.when(ff1.getFitness(Mockito.any())).thenReturn(4.0,3.0,1.0,0.0);
//
        TestFitnessFunction ff2 = Mockito.mock(TestFitnessFunction.class);
        Mockito.when(ff2.getFitness(Mockito.any())).thenReturn(1.0);

        ffList.add(ff1);
        ffList.add(ff2);

        FitnessFunctions fitnessFunctions = Mockito.mock(FitnessFunctions.class);
        Mockito.doReturn(ffList).when(fitnessFunctions).getFitnessFunctionList();




        // Mock crossover operator
        CrossOverFunction crossOver =  Mockito.mock(CrossOverFunction.class);
//        Mockito.doNothing().when(crossOver).crossOver(Mockito.any(),Mockito.any());

        // Mock mutation operator
        Mutation mutation =  Mockito.mock(Mutation.class);

        AbstractMOSA MOSA = new MOSA(factory,crossOver,mutation,fitnessFunctions);
        MOSA.addFitnessFunctions(ffList);

        MOSA.generateSolution();
    }
}
