package eu.stamp.botsing.commons.testgeneration.strategy;

import eu.stamp.botsing.commons.fitnessfunction.FitnessFunctions;
import eu.stamp.botsing.commons.ga.strategy.mosa.MOSA;
import eu.stamp.botsing.commons.ga.strategy.operators.Mutation;
import eu.stamp.botsing.commons.testutil.ChromosomeUtil;
import org.evosuite.Properties;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.operators.crossover.CrossOverFunction;
import org.evosuite.ga.stoppingconditions.MaxTimeStoppingCondition;
import org.evosuite.ga.stoppingconditions.StoppingCondition;
import org.evosuite.ga.stoppingconditions.ZeroFitnessStoppingCondition;
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
        TestChromosome ch1 =  ChromosomeUtil.createTestChromosome(new ArrayList<Integer>(){{add(10);}});
        TestChromosome ch2 =  ChromosomeUtil.createTestChromosome(new ArrayList<Integer>(){{add(9);}});


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
