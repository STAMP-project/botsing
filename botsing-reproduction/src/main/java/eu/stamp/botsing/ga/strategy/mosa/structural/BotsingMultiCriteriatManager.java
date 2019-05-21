package eu.stamp.botsing.ga.strategy.mosa.structural;

import eu.stamp.botsing.coverage.branch.IntegrationTestingBranchCoverageFactory;
import org.evosuite.coverage.branch.BranchCoverageTestFitness;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.metaheuristics.mosa.structural.MultiCriteriatManager;


import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class BotsingMultiCriteriatManager<T extends Chromosome> extends MultiCriteriatManager<T> {

    public BotsingMultiCriteriatManager(List<FitnessFunction<T>> fitnessFunctions) {
        super(fitnessFunctions);
    }

    @Override
    protected void initializeDependenciesForOtherTargets(){

    }

    @Override
    public BranchFitnessGraph getControlDepencies4Branches(List<FitnessFunction<T>> fitnessFunctions){
        Set<FitnessFunction<T>> setOfBranches = new LinkedHashSet<FitnessFunction<T>>();
        this.dependencies = new LinkedHashMap();

        List<BranchCoverageTestFitness> branches = new IntegrationTestingBranchCoverageFactory().getCoverageGoals(0);
        for (BranchCoverageTestFitness branch : branches){
            setOfBranches.add((FitnessFunction<T>) branch);
            this.dependencies.put(branch, new LinkedHashSet<FitnessFunction<T>>());
        }

        // initialize the maps
        this.initializeMaps(setOfBranches);

        return new BranchFitnessGraph<T, FitnessFunction<T>>(setOfBranches);
    }


}
