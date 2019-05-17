package eu.stamp.botsing.coverage.branch;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import eu.stamp.botsing.testgeneration.TestGenerationContextUtility;
import org.evosuite.coverage.branch.Branch;
import org.evosuite.coverage.branch.BranchCoverageTestFitness;
import org.evosuite.graphs.GraphPool;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.BytecodeInstructionPool;
import org.evosuite.graphs.cfg.ControlDependency;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class IntegrationTestingBranchCoverageFactory {

    public static long goalComputationTime = 0l;

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

    public List<BranchCoverageTestFitness> getCoverageGoals(int traceNumber) {
        return computeCoverageGoals(traceNumber);
    }

    private List<BranchCoverageTestFitness> computeCoverageGoals(int traceNumber) {
        long start = System.currentTimeMillis();
        List<BranchCoverageTestFitness> goals = new ArrayList<BranchCoverageTestFitness>();
        String className = CrashProperties.getInstance().getStackTrace(traceNumber).getTargetClass();
        String methodName = CrashProperties.getInstance().getStackTrace(traceNumber).getTargetMethod();
        int lineNumber = CrashProperties.getInstance().getStackTrace(traceNumber).getTargetLine();
        BytecodeInstruction goalInstruction = BytecodeInstructionPool.getInstance(TestGenerationContextUtility.getTestGenerationContextClassLoader()).getFirstInstructionAtLineNumber(className, methodName, lineNumber);
        // collect branches in the path to the target node
        Set<ControlDependency> targetCDs = GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).getCDG("IntegrationTestingGraph","methodsIntegration").getControlDependentBranches(goalInstruction.getBasicBlock());
        for(ControlDependency cd: targetCDs){
            goals.add(createBranchCoverageTestFitness(cd.getBranch(),cd.getBranchExpressionValue()));
        }
        goalComputationTime = System.currentTimeMillis() - start;
        return goals;
    }
}
