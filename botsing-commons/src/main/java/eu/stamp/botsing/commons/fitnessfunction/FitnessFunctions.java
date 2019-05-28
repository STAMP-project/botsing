package eu.stamp.botsing.commons.fitnessfunction;

import org.evosuite.ga.Chromosome;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.testcase.TestFitnessFunction;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class FitnessFunctions {
    public abstract List<TestFitnessFunction> getFitnessFunctionList();

    public abstract <T extends Chromosome> boolean isCriticalGoalsAreCovered(Set<FitnessFunction<T>> uncoveredGoals);

    public abstract void printCriticalTargets(Map<FitnessFunction<?>,Double> front0);
}
