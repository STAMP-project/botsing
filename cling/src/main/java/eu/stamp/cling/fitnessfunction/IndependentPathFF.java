package eu.stamp.cling.fitnessfunction;

import eu.stamp.cling.coverage.branch.IntegrationTestingBranchCoverageFactory;
import org.evosuite.coverage.ControlFlowDistance;
import org.evosuite.coverage.branch.BranchCoverageTestFitness;
import org.evosuite.graphs.cfg.BasicBlock;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IndependentPathFF  extends TestFitnessFunction {
     private List<BasicBlock> pathToIntegrationPoint;
     private List<BasicBlock> pathAfterIntegrationPoint;

    public IndependentPathFF(List<BasicBlock> pathToIntegrationPoint, List<BasicBlock> pathAfterIntegrationPoint){
        this.pathToIntegrationPoint = pathToIntegrationPoint;
        this.pathAfterIntegrationPoint = pathAfterIntegrationPoint;
    }

    @Override
    public double getFitness(TestChromosome testChromosome, ExecutionResult executionResult) {
        double uncoveredNodes = pathToIntegrationPoint.size() + pathAfterIntegrationPoint.size();
        double branchDistance = 0;
        boolean firstPartIsCovered = true;
        Map<Integer,Integer> coveredinPath = new HashMap<>();
        for(BasicBlock node:this.pathToIntegrationPoint){
            if(isBlockCovered(executionResult, node,coveredinPath)){
                uncoveredNodes--;
            }else{
                firstPartIsCovered = false;
                branchDistance = calculateBranchDistance(node,executionResult);
                break;
            }
        }

        if(firstPartIsCovered){
            for(BasicBlock node:this.pathAfterIntegrationPoint){
                if(isBlockCovered(executionResult, node,coveredinPath)){
                    uncoveredNodes--;
                }else{
                    branchDistance = calculateBranchDistance(node,executionResult);
                    break;
                }
            }
        }


        return (uncoveredNodes + branchDistance);
    }

    private double calculateBranchDistance(BasicBlock node, ExecutionResult executionResult) {
//        Properties.TARGET_CLASS = IntegrationTestingProperties.TARGET_CLASSES[0];
        BranchCoverageTestFitness coverageTestFitness = IntegrationTestingBranchCoverageFactory.createRootBranchTestFitness(node.getLastInstruction());
        return computeBranchDistance(coverageTestFitness,executionResult);
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

    private boolean isBlockCovered(ExecutionResult executionResult, BasicBlock node, Map<Integer,Integer> coveredinPath) {
        if(node.getFirstInstruction() == null){
            // node is a helping class we will return true
            return true;
        }
        BytecodeInstruction firstInst = node.getLastInstruction();
        BytecodeInstruction lastInst = node.getLastInstruction();
        if(executionResult.getTrace().getCoverageData().containsKey(node.getClassName()) && executionResult.getTrace().getCoverageData().get(node.getClassName()).containsKey(node.getMethodName())){
            Map<Integer,Integer> coveredLines = executionResult.getTrace().getCoverageData().get(node.getClassName()).get(node.getMethodName());

            // Check if the first instruction of the basic block is covered in the execution result
            boolean isFirstInstCovered = true;
//            if(!coveredLines.containsKey(firstInst.getLineNumber()) ){
//                isFirstInstCovered = false;
//            }else if (coveredLines.get(firstInst.getLineNumber()) > coveredinPath.get(firstInst.getLineNumber())){
//                isFirstInstCovered = true;
//            }else{
//                isFirstInstCovered = false;
//            }

            if(!coveredinPath.containsKey(lastInst.getLineNumber())){
                coveredinPath.put(lastInst.getLineNumber(),0);
            }

            // If first instruction is covered, lets check the last one.
            if(isFirstInstCovered){
                if(coveredLines.containsKey(lastInst.getLineNumber()) && coveredLines.get(lastInst.getLineNumber()) > coveredinPath.get(lastInst.getLineNumber())){
                    coveredinPath.put(lastInst.getLineNumber(),coveredinPath.get(lastInst.getLineNumber())+1);
                    return true;
                }
            }
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
        int result = 1;
        result = prime * result + (pathToIntegrationPoint.get(0).hashCode()+ pathAfterIntegrationPoint.get(0).hashCode());
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
        return pathToIntegrationPoint.get(0).getClassName();
    }

    @Override
    public String getTargetMethod() {
        return pathToIntegrationPoint.get(0).getMethodName();
    }
}
