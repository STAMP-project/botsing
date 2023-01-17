package eu.stamp.botsing.commons.testgeneration.strategy;

import eu.stamp.botsing.commons.fitnessfunction.FitnessFunctions;
import eu.stamp.botsing.commons.ga.strategy.mosa.MOSA;
import eu.stamp.botsing.commons.ga.strategy.operators.Mutation;
import org.evosuite.Properties;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.operators.crossover.CrossOverFunction;
import org.evosuite.ga.stoppingconditions.MaxTimeStoppingCondition;
import org.evosuite.ga.stoppingconditions.StoppingCondition;
import org.evosuite.ga.stoppingconditions.ZeroFitnessStoppingCondition;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.junit.Assert;
import org.junit.Test;
import org.evosuite.shaded.org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MOSuiteStrategyTest {

    @Test
    public void test0(){


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

//        AbstractMOSA MOSA = new MOSA(factory,crossOver,mutation,fitnessFunctions);

        MOSA mosaStrategy = Mockito.spy(new MOSA(factory,crossOver,mutation,fitnessFunctions));

        AbstractTestGenerationUtility utility = Mockito.mock(AbstractTestGenerationUtility.class);
        Mockito.when(utility.getGA()).thenReturn(mosaStrategy);
        Properties.STOPPING_CONDITION = Properties.StoppingCondition.MAXTIME;
        Properties.STOP_ZERO = true;
        FitnessFunctions fitnessFunctionCollector = Mockito.mock(FitnessFunctions.class);
        Mockito.when(fitnessFunctionCollector.getFitnessFunctionList()).thenReturn(ffList);


        MOSuiteStrategy suiteStrategy = new MOSuiteStrategy(utility,fitnessFunctionCollector);
        TestSuiteChromosome testSuite = suiteStrategy.generateTests();
        Assert.assertEquals(2, mosaStrategy.getStoppingConditions().size());

        boolean maxTimeFlag = false;
        boolean zeroFitnessFlag = false;

        Iterator scIter = mosaStrategy.getStoppingConditions().iterator();
        while(scIter.hasNext()){
            StoppingCondition sc = (StoppingCondition) scIter.next();
            if (sc instanceof MaxTimeStoppingCondition){
                maxTimeFlag = true;
            }else if (sc instanceof ZeroFitnessStoppingCondition){
                zeroFitnessFlag = true;
            }
        }

        Assert.assertTrue(maxTimeFlag);
        Assert.assertTrue(zeroFitnessFlag);
    }
}
