package eu.stamp.botsing.integration.testgeneration.strategy;

import eu.stamp.botsing.commons.ga.strategy.mosa.MOSA;
import eu.stamp.botsing.commons.testgeneration.strategy.AbstractTestGenerationUtility;
import eu.stamp.botsing.integration.IntegrationTestingProperties;
import eu.stamp.botsing.integration.fitnessfunction.FitnessFunctions;
import eu.stamp.botsing.integration.ga.strategy.operators.SimpleMutation;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.archive.ArchiveTestChromosomeFactory;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.ga.operators.crossover.SinglePointCrossOver;
import org.evosuite.testcase.TestChromosome;


public class TestGenerationUtility extends AbstractTestGenerationUtility {


    public GeneticAlgorithm getGA(){
        switch (IntegrationTestingProperties.searchAlgorithm){
                case MOSA:
                    // Create chromosome factory
                    ChromosomeFactory<TestChromosome> chromosomeFactory = getChromosomeFactory();
                    // Create CrossOver operator
                    SinglePointCrossOver crossOver = new SinglePointCrossOver();
                    // Create mutation operator
                    SimpleMutation mutation = new SimpleMutation();
                    // Create fitness function collector
                    FitnessFunctions ffCollector = new FitnessFunctions();
                    return new MOSA(chromosomeFactory,crossOver,mutation,ffCollector);
                default:
                    return new MOSA(getChromosomeFactory(),new SinglePointCrossOver(),new SimpleMutation(),new FitnessFunctions());
        }
    }


    protected ChromosomeFactory<TestChromosome> getChromosomeFactory() {
        // ToDo: create a new chromosome factory according to our problem requirements
        return new ArchiveTestChromosomeFactory();
    }





}
