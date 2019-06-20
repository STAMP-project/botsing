package eu.stamp.botsing.integration.ga.strategy.operators;

import eu.stamp.botsing.commons.ga.strategy.operators.Mutation;
import eu.stamp.botsing.integration.testgeneration.CallableMethodPool;
import org.evosuite.ga.Chromosome;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.utils.generic.GenericAccessibleObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class IntegrationTestingMutation<T extends Chromosome> extends Mutation<T> {
    private static final Logger LOG = LoggerFactory.getLogger(IntegrationTestingMutation.class);

    public void mutateOffspring(T offspring) {
        boolean isValid = false;
        int nTrials = 0; // we try maximum 50 insertion mutations (to avoid infinite loop)
        while (!isValid && nTrials < 5) {
            try {
                doRandomMutation(offspring);
                isValid = CallableMethodPool.getInstance().includesPublicCall((TestChromosome) offspring);
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
        boolean mutated = false;
        while (!mutated){
            try{
                offspring.mutate();
                mutated=true;
            }catch (Exception e){
                LOG.debug("Mutation was unsuccessful!");
            }
        }
    }

//    @Override
//    public void mutateOffspring(Chromosome offspring) {
//        offspring.mutate();
//        offspring.setChanged(true);
//        offspring.updateAge(offspring.getAge() + 1);
//    }
}
