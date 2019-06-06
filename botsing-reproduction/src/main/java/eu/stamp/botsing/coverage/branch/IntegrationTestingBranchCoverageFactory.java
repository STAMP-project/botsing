package eu.stamp.botsing.coverage.branch;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import eu.stamp.botsing.commons.coverage.branch.IntegrationTestingBranchCoverageGoal;
import eu.stamp.botsing.commons.coverage.branch.IntegrationTestingBranchCoverageTestFitness;
import eu.stamp.botsing.commons.testgeneration.TestGenerationContextUtility;
import org.evosuite.coverage.branch.Branch;
import org.evosuite.coverage.branch.BranchCoverageTestFitness;
import org.evosuite.graphs.GraphPool;
import org.evosuite.graphs.cdg.ControlDependenceGraph;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.BytecodeInstructionPool;
import org.evosuite.graphs.cfg.ControlDependency;

import java.util.*;

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
        List<Branch> addedBranches = new ArrayList<>();

        // Get deepest frame instruction
        String className = CrashProperties.getInstance().getStackTrace(traceNumber).getFrame(1).getClassName();
        int lineNumber = CrashProperties.getInstance().getStackTrace(traceNumber).getFrame(1).getLineNumber();
        String methodName = TestGenerationContextUtility.derivingMethodFromBytecode(CrashProperties.integrationTesting, className, lineNumber);
        BytecodeInstruction deepestFrameInstruction = BytecodeInstructionPool.getInstance(TestGenerationContextUtility.getTestGenerationContextClassLoader(CrashProperties.integrationTesting)).getFirstInstructionAtLineNumber(className, methodName, lineNumber);

        // Collect branches in the path to the target node
        ControlDependenceGraph IntegrationCDG = GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).getCDG("IntegrationTestingGraph","methodsIntegration");
        LinkedList<BytecodeInstruction> unhandledInstructions = new LinkedList<>();
        unhandledInstructions.add(deepestFrameInstruction);

        while(!unhandledInstructions.isEmpty()){
            BytecodeInstruction currentInstruction = unhandledInstructions.pop();
            Set<ControlDependency> currentCDs = IntegrationCDG.getControlDependentBranches(currentInstruction.getBasicBlock());
            for(ControlDependency cd: currentCDs){
                if(addedBranches.contains(cd.getBranch())){
                    continue;
                }
                goals.add(createBranchCoverageTestFitness(cd.getBranch(),cd.getBranchExpressionValue()));
                addedBranches.add(cd.getBranch());
                Set<ControlDependency> nextCDs = IntegrationCDG.getControlDependentBranches(cd.getBranch().getInstruction().getBasicBlock());
                for(ControlDependency next: nextCDs){
//                    if(!addedBranches.contains(next.getBranch())){
                        unhandledInstructions.add(next.getBranch().getInstruction());
//                    }
                }
            }
        }
        goalComputationTime = System.currentTimeMillis() - start;
        return goals;
    }
}
