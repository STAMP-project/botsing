package eu.stamp.botsing.ga.strategy.mosa.structural;

import eu.stamp.botsing.coverage.branch.IntegrationTestingBranchCoverageFactory;
import org.evosuite.coverage.branch.BranchCoverageTestFitness;
import org.evosuite.ga.metaheuristics.mosa.structural.MultiCriteriaManager;
import org.evosuite.testcase.TestFitnessFunction;


import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class BotsingMultiCriteriatManager extends MultiCriteriaManager {

    public BotsingMultiCriteriatManager(List<TestFitnessFunction> fitnessFunctions) {
        super(fitnessFunctions);
    }

    @Override
    protected void initializeDependenciesForOtherTargets(){

    }

    @Override
    public BranchFitnessGraph getControlDependencies4Branches(List<TestFitnessFunction> fitnessFunctions){
        Set<TestFitnessFunction> setOfBranches = new LinkedHashSet<>();
        this.dependencies = new LinkedHashMap();

        List<BranchCoverageTestFitness> branches = new IntegrationTestingBranchCoverageFactory().getCoverageGoals(0);
        for (BranchCoverageTestFitness branch : branches){
            setOfBranches.add((TestFitnessFunction) branch);
            this.dependencies.put(branch, new LinkedHashSet<TestFitnessFunction>());
        }

        // initialize the maps
        this.initializeMaps(setOfBranches);

        return new BranchFitnessGraph(setOfBranches);
    }


}
