package eu.stamp.cling.fitnessfunction;

import eu.stamp.cling.coverage.branch.BranchPair;
import eu.stamp.cling.coverage.branch.IntegrationTestingBranchCoverageFactory;
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
        int result = prime;
        if(branchPair.getFirstBranch() != null){
            result+=branchPair.getFirstBranch().hashCode();
        }

        if(branchPair.getSecondBranch() != null){
            result+=branchPair.getSecondBranch().hashCode();
        }

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

        if(obj instanceof BranchPairFF){
            BranchPairFF ff2 = (BranchPairFF) obj;
            if(ff2.isOnePairIsNull() && this.isOnePairIsNull() && this.getNonNullPairFF().equals(ff2.getNonNullPairFF())){
                    return true;
            }else if(!ff2.isOnePairIsNull() && !this.isOnePairIsNull() && ff2.firstBranchFF.equals(this.firstBranchFF) && ff2.secondBranchFF.equals(this.secondBranchFF)){
                return true;
            }
        }

        return false;

    }

    @Override
    public String getTargetClass() {
        return this.branchPair.getFirstBranch().getClassName();
    }

    @Override
    public String getTargetMethod() {
        return this.branchPair.getFirstBranch().getMethodName();
    }

    public boolean isOnePairIsNull(){
        return (firstBranchFF == null || secondBranchFF == null);
    }

    public BranchCoverageTestFitness getNonNullPairFF(){
        if(!isOnePairIsNull()){
            throw new IllegalStateException("Pairs are not null!");
        }

        if(firstBranchFF != null){
            return firstBranchFF;
        }

        if(secondBranchFF != null){
            return secondBranchFF;
        }

        throw new IllegalStateException("Both pairs are null!");
    }
    public BranchCoverageTestFitness getFirstBranchFF(){
        return firstBranchFF;
    }

    public BranchCoverageTestFitness getSecondBranchFF(){
        return firstBranchFF;
    }
}
