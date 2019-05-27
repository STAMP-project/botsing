package eu.stamp.botsing.coverage.branch;

import org.evosuite.coverage.branch.BranchCoverageTestFitness;

public class IntegrationTestingBranchCoverageTestFitness extends BranchCoverageTestFitness {
    public IntegrationTestingBranchCoverageTestFitness(IntegrationTestingBranchCoverageGoal goal) throws IllegalArgumentException {
        super(goal);
    }
}
