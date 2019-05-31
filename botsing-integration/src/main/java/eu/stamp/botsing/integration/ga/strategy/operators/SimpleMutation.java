package eu.stamp.botsing.integration.ga.strategy.operators;

import eu.stamp.botsing.commons.ga.strategy.operators.Mutation;
import org.evosuite.ga.Chromosome;

public class SimpleMutation extends Mutation {
    @Override
    public void mutateOffspring(Chromosome offspring) {
        offspring.mutate();
        offspring.setChanged(true);
        offspring.updateAge(offspring.getAge() + 1);
    }
}
