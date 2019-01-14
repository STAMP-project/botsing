package eu.stamp.botsing.ga.strategy.operators;

import eu.stamp.botsing.ga.strategy.GuidedGeneticAlgorithm;
import org.evosuite.ga.operators.crossover.SinglePointCrossOver;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ConstructionFailedException;
import org.evosuite.ga.operators.crossover.CrossOverFunction;
import org.evosuite.utils.Randomness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GuidedSinglePointCrossover extends CrossOverFunction {

    private static final long serialVersionUID = 2881387570766261795L;

    private static final Logger LOG = LoggerFactory.getLogger(GuidedGeneticAlgorithm.class);

    private static GuidedSearchUtility utility =  new GuidedSearchUtility();

    private SinglePointCrossOver crossover = new SinglePointCrossOver();


    public void crossOver(Chromosome parent1, Chromosome parent2) {
        Chromosome clone1 = parent1.clone();
        Chromosome clone2 = parent2.clone();

        try{
            singlePointCrossover(parent1, parent2);
        } catch (ConstructionFailedException | Error e){
            LOG.debug("construction failed when doing crossover!");
        } catch (Exception e) {
            LOG.warn("Exception during the crossover!");
        }
        // if the test case is missing of the target method call,
        // we ignore the results of the single point crossover
        if (!isValid(parent1)){
            ((TestChromosome) parent1).setTestCase(((TestChromosome) clone1).getTestCase());
        }
        if (!isValid(parent2)){
            ((TestChromosome) parent2).setTestCase(((TestChromosome) clone2).getTestCase());
        }
    }

    protected boolean isValid(Chromosome chromosome){
        return utility.includesPublicCall(chromosome);
    }

    private void singlePointCrossover(Chromosome parent1, Chromosome parent2)
            throws ConstructionFailedException {

        if (parent1.size() < 2 || parent2.size() < 2) {
            return;
        }
        // Choose a position in the middle
        int point1 = Randomness.nextInt(parent1.size() - 1) + 1;
        int point2 = Randomness.nextInt(parent2.size() - 1) + 1;

        parent1.crossOver(parent1, point1, point2);
        parent2.crossOver(parent2, point2, point1);
    }

}
