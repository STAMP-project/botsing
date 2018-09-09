package eu.stamp.botsing.testgeneration.strategy;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.fitnessfunction.WeightedSum;
import eu.stamp.botsing.fitnessfunction.testcase.factories.RootMethodTestChromosomeFactory;
import eu.stamp.botsing.ga.strategy.SingleObjectiveGGA;
import org.evosuite.Properties;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.ga.stoppingconditions.GlobalTimeStoppingCondition;
import org.evosuite.ga.stoppingconditions.MaxTimeStoppingCondition;
import org.evosuite.ga.stoppingconditions.StoppingCondition;
import org.evosuite.ga.stoppingconditions.ZeroFitnessStoppingCondition;
import org.evosuite.strategy.TestGenerationStrategy;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionTracer;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// This strategy selects one coverage goal. In the current version, this single goal is crash coverage
public class BotsingIndividualStrategy extends TestGenerationStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(BotsingIndividualStrategy.class);

    @Override
    public TestSuiteChromosome generateTests() {
        LOG.info("test generation strategy: BotSing individual");
        LOG.info("The single goal is crash coverage.");
        TestSuiteChromosome suite = new TestSuiteChromosome();
        StoppingCondition stoppingCondition = getStoppingCondition();
        try {
            stoppingCondition.setLimit(CrashProperties.getLongValue("search_budget"));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (Properties.NoSuchParameterException e) {
            e.printStackTrace();
        }
        ExecutionTracer.enableTraceCalls();
        GeneticAlgorithm ga = getGA();

        if (Properties.CHECK_BEST_LENGTH) {
            org.evosuite.testcase.RelativeTestLengthBloatControl bloat_control = new org.evosuite.testcase.RelativeTestLengthBloatControl();
            ga.addBloatControl(bloat_control);
            ga.addListener(bloat_control);
        }
        ga.addStoppingCondition(stoppingCondition);
        ga.addStoppingCondition(new ZeroFitnessStoppingCondition());

        if (!(stoppingCondition instanceof MaxTimeStoppingCondition)) {
            ga.addStoppingCondition(new GlobalTimeStoppingCondition());
        }
        TestFitnessFunction ff = getFF();
        ga.addFitnessFunction(ff);
        ga.generateSolution();



        if (ga.getBestIndividual().getFitness() == 0.0) {
            TestChromosome solution = (TestChromosome) ga.getBestIndividual();
            LOG.info("* The target crash is covered. The generated test is: "+solution.getTestCase().toCode());
            suite.addTest(solution);


        }else{
            LOG.info("* The target crash is not covered! The best solution has "+ga.getBestIndividual().getFitness()+" fitness value.");
        }

        // after finishing the search check: ga.getBestIndividual().getFitness() == 0.0
        // add best individual to final result and return result (TestSuiteChromosome)


        return suite;
    }

    private GeneticAlgorithm getGA(){
        switch (CrashProperties.searchAlgorithm){
            case Single_Objective_GGA:
                return new SingleObjectiveGGA(getChromosomeFactory());
            default:
                return new SingleObjectiveGGA(getChromosomeFactory());
        }
    }

    private ChromosomeFactory<TestChromosome> getChromosomeFactory() {
        return new RootMethodTestChromosomeFactory();
    }

    private TestFitnessFunction getFF(){
        return new WeightedSum();
    }
}
