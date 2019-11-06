package eu.stamp.botsing.ga.strategy.operators;

import eu.stamp.botsing.commons.ga.strategy.operators.Mutation;
import eu.stamp.botsing.ga.strategy.GuidedGeneticAlgorithm;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.utils.generic.GenericAccessibleObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.evosuite.ga.Chromosome;

import java.util.HashSet;
import java.util.Set;

public class GuidedMutation<T extends Chromosome> extends Mutation<T> {

    private static final Logger LOG = LoggerFactory.getLogger(GuidedGeneticAlgorithm.class);
    private static GuidedSearchUtility utility = new GuidedSearchUtility();

    Set<GenericAccessibleObject<?>> publicCalls = new HashSet<>();
    @Override
    public void mutateOffspring(T offspring) {
        if(this.publicCalls.isEmpty()){
            throw new IllegalStateException("set of public calls is empty");
        }
        boolean isValid = false;
        int nTrials = 0; // we try maximum 50 insertion mutations (to avoid infinite loop)
        while (!isValid && nTrials < 5) {
            try {
                    doRandomMutation(offspring);
                isValid = utility.includesPublicCall(offspring,publicCalls);
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

    protected void insertRandomStatement(Chromosome chromosome) {
        ((TestChromosome) chromosome).mutationInsert();
    }

    public void updatePublicCalls(Set<GenericAccessibleObject<?>> publicCalls){
        this.publicCalls.clear();
        this.publicCalls.addAll(publicCalls);
    }
}
