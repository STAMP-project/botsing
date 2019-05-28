package eu.stamp.botsing.commons.ga.strategy.operators;

import org.evosuite.ga.Chromosome;

public abstract class Mutation<T extends Chromosome> {

    public abstract void mutateOffspring(T offspring);
}
