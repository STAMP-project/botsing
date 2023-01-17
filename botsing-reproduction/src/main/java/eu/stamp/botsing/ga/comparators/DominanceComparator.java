package eu.stamp.botsing.ga.comparators;

import org.evosuite.ga.Chromosome;
import org.evosuite.ga.FitnessFunction;


public class DominanceComparator <T extends Chromosome<T>> extends org.evosuite.ga.comparators.DominanceComparator<T> {


    public boolean isEqual(Chromosome c1, Chromosome c2){
        if (this.objectives == null) {
            this.objectives = c1.getFitnessValues().keySet();
        }

        for (FitnessFunction<?> ff : this.objectives) {
            int flag = Double.compare(c1.getFitness(ff), c2.getFitness(ff));
            if (flag != 0){
                return false;
            }
        }

        return true;
    }
}
