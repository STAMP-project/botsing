package eu.stamp.botsing.integration.testgeneration.strategy;

import eu.stamp.botsing.commons.ga.strategy.mosa.MOSA;
import eu.stamp.botsing.commons.testgeneration.strategy.AbstractTestGenerationUtility;
import eu.stamp.botsing.integration.IntegrationTestingProperties;
import eu.stamp.botsing.integration.fitnessfunction.FitnessFunctions;
import eu.stamp.botsing.integration.fitnessfunction.testcase.factories.BranchPairsChromosomeFactory;
import eu.stamp.botsing.integration.ga.strategy.operators.IntegrationTestingMutation;
import eu.stamp.botsing.integration.ga.strategy.operators.IntegrationTestingSinglePointCrossover;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.ga.operators.crossover.CrossOverFunction;
import org.evosuite.ga.operators.crossover.SinglePointCrossOver;
import org.evosuite.testcase.TestChromosome;


public class TestGenerationUtility extends AbstractTestGenerationUtility {


    public GeneticAlgorithm getGA(){
        switch (IntegrationTestingProperties.searchAlgorithm){
                case MOSA:
                    // Create chromosome factory
                    ChromosomeFactory<TestChromosome> chromosomeFactory = getChromosomeFactory();
                    // Create CrossOver operator
                    CrossOverFunction crossOver = new IntegrationTestingSinglePointCrossover();
                    // Create mutation operator
                    IntegrationTestingMutation mutation = new IntegrationTestingMutation();
                    // Create fitness function collector
                    FitnessFunctions ffCollector = new FitnessFunctions();
                    return new MOSA(chromosomeFactory,crossOver,mutation,ffCollector);
                default:
                    return new MOSA(getChromosomeFactory(),new SinglePointCrossOver(),new IntegrationTestingMutation(),new FitnessFunctions());
        }
    }


    protected ChromosomeFactory<TestChromosome> getChromosomeFactory() {
        return new BranchPairsChromosomeFactory();
    }





}
