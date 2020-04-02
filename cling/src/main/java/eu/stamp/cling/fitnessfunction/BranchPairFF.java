package eu.stamp.cling.fitnessfunction;

import eu.stamp.cling.coverage.branch.BranchPair;
import eu.stamp.cling.coverage.branch.IntegrationTestingBranchCoverageFactory;
import org.evosuite.coverage.ControlFlowDistance;
import org.evosuite.coverage.branch.Branch;
import org.evosuite.coverage.branch.BranchCoverageFactory;
import org.evosuite.coverage.branch.BranchCoverageTestFitness;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.ControlDependency;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testcase.execution.ExecutionTrace;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

        if (isBranchPairCovered(executionResult)){
            return 0;
        }

        if(firstBranchFF != null){
            firstBranchValue = firstBranchFF.getFitness(testChromosome,executionResult);
        }

        if(secondBranchFF != null){
            secondBranchValue = secondBranchFF.getFitness(testChromosome,executionResult);
        }
        // Sum of branch fitness functions (approach level + branch distance) of branch1 and branch2
        return firstBranchValue + secondBranchValue;
    }


//    @Override
//    public double getFitness(TestChromosome testChromosome, ExecutionResult executionResult) {
//        double firstBranchValue = 0;
//        double secondBranchValue = 0;
//
//        if (isBranchPairCovered(executionResult)){
//            return 0;
//        }
//
//        if(firstBranchFF != null){
//            firstBranchValue = firstBranchFF.getFitness(testChromosome,executionResult);
//        }
//
//        if(firstBranchValue > 0){
//            return normalize(firstBranchValue) + 2;
//        }
//
//        double callSiteCoverage = this.getLineCoverage(executionResult);
//        if (callSiteCoverage > 0 ){
//            return normalize(callSiteCoverage) + 1;
//        }
//
//        if(secondBranchFF != null){
//            secondBranchValue = secondBranchFF.getFitness(testChromosome,executionResult);
//        }
//
//        // Sum of branch fitness functions (approach level + branch distance) of branch1 and branch2
//        return normalize(secondBranchValue);
//    }


    private double getLineCoverage(ExecutionResult result){
        String className = this.getCallSite().getClassName();
        int lineNumber = this.getCallSite().getLineNumber();
        List<BranchCoverageTestFitness> branchFitnesses = setupDependencies();
        double lineCoverageFitness = 1.0;
        if (result.getTrace().getCoveredLines(className).contains(lineNumber)) {
            lineCoverageFitness = 0.0;
        } else {
            lineCoverageFitness = Double.MAX_VALUE;
            // Indicate minimum distance
            for (BranchCoverageTestFitness branchFitness : branchFitnesses) {
                // let's calculate the branch distance
                double distance = computeBranchDistance(branchFitness, result);
                lineCoverageFitness = Math.min(lineCoverageFitness, distance);
            }
        }

        return lineCoverageFitness;
    }

    protected double computeBranchDistance(BranchCoverageTestFitness branchFitness, ExecutionResult result){
        ControlFlowDistance distance = branchFitness.getBranchGoal().getDistance(result);
        double value = distance.getResultingBranchFitness();

        if (value == 0.0) {
            // If the control dependency was covered, then likely
            // an exception happened before the line was reached
            value = 1.0;
        } else {
            value = normalize(value);
        }
        return value;
    }

    private List<BranchCoverageTestFitness> setupDependencies() {
        BytecodeInstruction goalInstruction = this.getCallSite();
        Set<ControlDependency> deps = goalInstruction.getControlDependencies();

        List<BranchCoverageTestFitness> branchCoverages = new ArrayList<>();
        if(goalInstruction == null){
            return branchCoverages;
        }
//        // get control dependencies of the target node
//        Set<ControlDependency> deps = goalInstruction.getControlDependencies();

        // Add control dependencies for calculating branch distances + approach level
        for (ControlDependency cd : deps) {
            BranchCoverageTestFitness singlefitness = BranchCoverageFactory.createBranchCoverageTestFitness(cd) ;
//            if(CrashProperties.integrationTesting){
//                singlefitness = IntegrationTestingBranchCoverageFactory.createBranchCoverageTestFitness(cd);
//            }else{
//                singlefitness = BranchCoverageFactory.createBranchCoverageTestFitness(cd);
//            }

            branchCoverages.add(singlefitness);
        }
        if (goalInstruction.isRootBranchDependent()) {
            branchCoverages.add(BranchCoverageFactory.createRootBranchTestFitness(goalInstruction));
//            if(CrashProperties.integrationTesting){
//                branchCoverages.add(IntegrationTestingBranchCoverageFactory.createRootBranchTestFitness(goalInstruction));
//            }else{
//                branchCoverages.add(BranchCoverageFactory.createRootBranchTestFitness(goalInstruction));
//            }

        }

        if (deps.isEmpty() && !goalInstruction.isRootBranchDependent()) {
            throw new IllegalStateException(
                    "expect control dependencies to be empty only for root dependent instructions: "
            );
        }

        if (branchCoverages.isEmpty()){
            throw new IllegalStateException(
                    "an instruction is at least on the root branch of it's method");
        }

        branchCoverages.sort((a,b) -> a.compareTo(b));

        return branchCoverages;
    }


    private boolean isBranchPairCovered(ExecutionResult executionResult) {
            ExecutionTrace trace = executionResult.getTrace();

            if(isBranchCoveredByTrace(trace,firstBranchFF) && isBranchCoveredByTrace(trace,secondBranchFF)){
                Set<Integer> coveredLines = trace.getCoveredLines(this.getCallSite().getClassName());
                if (coveredLines.contains(this.getCallSite().getLineNumber()) || firstBranchFF == null){
                    return true;
                }
            }
        return false;
    }


    private boolean isBranchCoveredByTrace(ExecutionTrace trace, BranchCoverageTestFitness branchFF) {
        if (branchFF == null){
            return true;
        }
        boolean branchExpressionValue = branchFF.getBranchExpressionValue();
        Branch targetBranch = branchFF.getBranch();

        Set<Integer> branchIDsToCheck;
        if (branchExpressionValue){
            // check covered true
            branchIDsToCheck = trace.getCoveredTrueBranches();
        }else{
            // check covered false
            branchIDsToCheck = trace.getCoveredFalseBranches();
        }

        if(branchIDsToCheck.contains(targetBranch.getActualBranchId())){
            return true;
        }
        return false;
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
        return secondBranchFF;
    }

    public BranchPair getBranchPair() {
        return branchPair;
    }

    public BytecodeInstruction getCallSite(){
        return branchPair.getCallSite();
    }
}
