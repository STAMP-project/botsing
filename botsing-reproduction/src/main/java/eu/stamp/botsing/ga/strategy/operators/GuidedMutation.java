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
        int nTrials = 0; // we try maximum 50 insertion mutations (to avoid infinite loop)
        while (!isValid && nTrials < 5) {
            try {
                    doRandomMutation(offspring);
                isValid = utility.includesPublicCall(offspring);
            } catch (AssertionError e) {
                LOG.debug("Random insertion mutation was unsuccessful.");
            } finally {
                nTrials++;
            }
        }
        offspring.setChanged(true);
        offspring.updateAge(offspring.getAge() + 1);
    }

    protected void doRandomMutation(Chromosome offspring) {
        offspring.mutate();
    }

    protected void insertRandomStatement(Chromosome chromosome) {
        ((TestChromosome) chromosome).mutationInsert();
    }
}
