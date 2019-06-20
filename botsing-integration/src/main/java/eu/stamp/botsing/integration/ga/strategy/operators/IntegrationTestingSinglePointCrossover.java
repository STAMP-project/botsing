package eu.stamp.botsing.integration.ga.strategy.operators;

import eu.stamp.botsing.integration.testgeneration.CallableMethodPool;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ConstructionFailedException;
import org.evosuite.ga.operators.crossover.CrossOverFunction;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.utils.Randomness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntegrationTestingSinglePointCrossover extends CrossOverFunction {
    private static final Logger LOG = LoggerFactory.getLogger(IntegrationTestingSinglePointCrossover.class);


    @Override
    public void crossOver(Chromosome parent1, Chromosome parent2){

        CallableMethodPool callableMethodPool = CallableMethodPool.getInstance();

        Chromosome clone1 = parent1.clone();
        Chromosome clone2 = parent2.clone();

        try{
            singlePointCrossover(parent1, parent2);
        } catch (ConstructionFailedException | Error e){
            LOG.debug("construction failed when doing crossover!");
        } catch (Exception e) {
            LOG.warn("Exception during the crossover!");
        }


        if (!callableMethodPool.includesPublicCall((TestChromosome) parent1)){
            ((TestChromosome) parent1).setTestCase(((TestChromosome) clone1).getTestCase());
        }
        if (!callableMethodPool.includesPublicCall((TestChromosome) parent2)){
            ((TestChromosome) parent2).setTestCase(((TestChromosome) clone2).getTestCase());
        }
    }


    private void singlePointCrossover(Chromosome parent1, Chromosome parent2)
            throws ConstructionFailedException {

        if (parent1.size() < 2 || parent2.size() < 2) {
            return;
        }
        // Choose a position in the middle
        int point1 = Randomness.nextInt(parent1.size() - 1) + 1;
        int point2 = Randomness.nextInt(parent2.size() - 1) + 1;

        Chromosome clone1 = parent1.clone();
        parent1.crossOver(parent2, point1, point2);
        parent2.crossOver(clone1, point2, point1);
    }
}
