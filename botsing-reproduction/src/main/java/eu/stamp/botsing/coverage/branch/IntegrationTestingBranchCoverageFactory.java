package eu.stamp.botsing.coverage.branch;

import org.evosuite.coverage.branch.Branch;
import org.evosuite.coverage.branch.BranchCoverageTestFitness;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.ControlDependency;

public class IntegrationTestingBranchCoverageFactory {

    public static BranchCoverageTestFitness createBranchCoverageTestFitness(ControlDependency cd) {
        return createBranchCoverageTestFitness(cd.getBranch(), cd.getBranchExpressionValue());
    }



    public static BranchCoverageTestFitness createBranchCoverageTestFitness(Branch b, boolean branchExpressionValue) {
        return new IntegrationTestingBranchCoverageTestFitness(new IntegrationTestingBranchCoverageGoal(b, branchExpressionValue, b.getClassName(), b.getMethodName()));
    }


    public static BranchCoverageTestFitness createRootBranchTestFitness(String className, String method) {
        return new IntegrationTestingBranchCoverageTestFitness(new IntegrationTestingBranchCoverageGoal(className, method.substring(method.lastIndexOf(".") + 1)));
    }

    public static BranchCoverageTestFitness createRootBranchTestFitness(BytecodeInstruction instruction) {
        if (instruction == null) {
            throw new IllegalArgumentException("null given");
        } else {
            return createRootBranchTestFitness(instruction.getClassName(), instruction.getMethodName());
        }
    }
}
