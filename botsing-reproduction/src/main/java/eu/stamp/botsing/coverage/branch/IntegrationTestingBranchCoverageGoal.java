package eu.stamp.botsing.coverage.branch;

import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import org.evosuite.coverage.ControlFlowDistance;
import org.evosuite.coverage.branch.Branch;
import org.evosuite.coverage.branch.BranchCoverageGoal;
import org.evosuite.graphs.cfg.BytecodeInstructionPool;
import org.evosuite.testcase.execution.ExecutionResult;

public class IntegrationTestingBranchCoverageGoal extends BranchCoverageGoal {

    public IntegrationTestingBranchCoverageGoal(String className, String methodName) {
        this.branch = null;
        this.value = true;

        this.className = className;
        this.methodName = methodName;
        lineNumber = BytecodeInstructionPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT())
                .getFirstLineNumberOfMethod(className,  methodName);
    }


    public IntegrationTestingBranchCoverageGoal(Branch branch, boolean value, String className, String methodName) {
        super(branch,value,className,methodName);
    }

    @Override
    public ControlFlowDistance getDistance(ExecutionResult result) {
        ControlFlowDistance r = InterProceduralControlFlowDistanceCalculator.getDistance(result, this.branch, this.value, this.className, this.methodName);
        return r;
    }


}
