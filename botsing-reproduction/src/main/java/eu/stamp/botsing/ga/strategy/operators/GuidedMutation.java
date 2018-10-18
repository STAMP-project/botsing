package eu.stamp.botsing.ga.strategy.operators;

import eu.stamp.botsing.ga.strategy.GuidedGeneticAlgorithm;
import org.evosuite.testcase.TestChromosome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.evosuite.ga.Chromosome;

public class GuidedMutation<T extends Chromosome> {

    private static final Logger LOG = LoggerFactory.getLogger(GuidedGeneticAlgorithm.class);

    private static GuidedSearchUtility utility = new GuidedSearchUtility();

    public void mutateOffspring(T offspring) {
        boolean isValid = false;
        // let's try one single mutation
        try {
            offspring.mutate();
            // if the chromosome has no public call, we insert new random statements
            isValid = utility.includesPublicCall(offspring);
        } catch (AssertionError e) {
            LOG.warn("First try for insertion mutation was unsuccessful.");
        }
        int nTrials = 0; // we try maximum 50 insertion mutations (to avoid infinite loop)
        while (!isValid && nTrials < 50) {
            try {
                ((TestChromosome) offspring).mutationInsert();
                isValid = utility.includesPublicCall(offspring);
                nTrials++;
            } catch (AssertionError e) {
                LOG.warn("Random insertion mutation was unsuccessful.");
            }

        }
        offspring.updateAge(offspring.getAge() + 1);
    }
}
