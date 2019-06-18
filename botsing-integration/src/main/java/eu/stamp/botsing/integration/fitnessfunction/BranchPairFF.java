package eu.stamp.botsing.integration.fitnessfunction;

import eu.stamp.botsing.integration.coverage.branch.BranchPair;
import eu.stamp.botsing.integration.coverage.branch.IntegrationTestingBranchCoverageFactory;
import org.evosuite.coverage.branch.BranchCoverageTestFitness;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;

public class BranchPairFF extends TestFitnessFunction {

    private BranchPair branchPair;
    private BranchCoverageTestFitness firstBranchFF;
    private BranchCoverageTestFitness secondBranchFF;

    public BranchPairFF(BranchPair branchPair, boolean firstBranchExpression, boolean secondBranchExpression){
        this.branchPair = branchPair;
        /* If first branch fitness function is empty, it means that we either
         had only one path, from the entry points of the public calls of the caller, to the call site
         or
         the involved methods from callee (for one of the call sites) are branchless.
        */
        if(branchPair.getFirstBranch() == null){
            firstBranchFF = null;
        }else{
            firstBranchFF = IntegrationTestingBranchCoverageFactory.EvoSuitecreateBranchCoverageTestFitness(branchPair.getFirstBranch(),firstBranchExpression);
        }
        /* If the second branch fitness function is empty, it means that we either
         did not have any branch after returning to the call site
         or
         the involved methods from callee (for one of the call sites) are branchless.
        */
        if(branchPair.getSecondBranch() == null){
            secondBranchFF = null;
        }else{
            secondBranchFF = IntegrationTestingBranchCoverageFactory.EvoSuitecreateBranchCoverageTestFitness(branchPair.getSecondBranch(),secondBranchExpression);
        }
    }

    @Override
    public double getFitness(TestChromosome testChromosome, ExecutionResult executionResult) {
        double firstBranchValue = 0;
        double secondBranchValue = 0;

        if(firstBranchFF != null){
            firstBranchValue = firstBranchFF.getFitness(testChromosome,executionResult);
        }

        if(secondBranchFF != null){
            secondBranchValue = secondBranchFF.getFitness(testChromosome,executionResult);
        }
        // Sum of branch fitness functions (approach level + branch distance) of branch1 and branch2
        return firstBranchValue + secondBranchValue;
    }

    @Override
    public int compareTo(TestFitnessFunction testFitnessFunction) {
        return 0;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (branchPair.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        return getClass() == obj.getClass();
    }

    @Override
    public String getTargetClass() {
        return this.branchPair.getFirstBranch().getClassName();
    }

    @Override
    public String getTargetMethod() {
        return this.branchPair.getFirstBranch().getMethodName();
    }
}
