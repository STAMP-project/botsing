package eu.stamp.botsing.integration.coverage.branch;

import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import eu.stamp.botsing.commons.coverage.branch.IntegrationTestingBranchCoverageGoal;
import eu.stamp.botsing.commons.coverage.branch.IntegrationTestingBranchCoverageTestFitness;
import org.evosuite.coverage.MethodNameMatcher;
import org.evosuite.coverage.branch.Branch;
import org.evosuite.coverage.branch.BranchCoverageGoal;
import org.evosuite.coverage.branch.BranchCoverageTestFitness;
import org.evosuite.coverage.branch.BranchPool;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.ControlDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class IntegrationTestingBranchCoverageFactory {
    private static final Logger LOG = LoggerFactory.getLogger(IntegrationTestingBranchCoverageFactory.class);
    public static long goalComputationTime = 0l;

    public static BranchCoverageTestFitness createBranchCoverageTestFitness(ControlDependency cd) {
        return createBranchCoverageTestFitness(cd.getBranch(), cd.getBranchExpressionValue());
    }



    public static BranchCoverageTestFitness createBranchCoverageTestFitness(Branch b, boolean branchExpressionValue) {
        return new IntegrationTestingBranchCoverageTestFitness(new IntegrationTestingBranchCoverageGoal(b, branchExpressionValue, b.getClassName(), b.getMethodName()));
    }

    public static BranchCoverageTestFitness EvoSuitecreateBranchCoverageTestFitness(Branch b, boolean branchExpressionValue) {
        return new BranchCoverageTestFitness(new BranchCoverageGoal(b, branchExpressionValue, b.getClassName(), b.getMethodName()));
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

    public List<BranchCoverageTestFitness> getCoverageGoals() {
        return computeCoverageGoals();
    }

    private List<BranchCoverageTestFitness> computeCoverageGoals() {
        List<BranchCoverageTestFitness> goals = new ArrayList<BranchCoverageTestFitness>();

        // logger.info("Getting branches");
        for (String className : BranchPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).knownClasses()) {
            final MethodNameMatcher matcher = new MethodNameMatcher();
            // Branchless methods
            for (String method : BranchPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).getBranchlessMethods(className)) {
                if (matcher.fullyQualifiedMethodMatches(method)) {
                    goals.add(createRootBranchTestFitness(className, method));
                }
            }

            // Branches
            for (String methodName : BranchPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).knownMethods(className)) {
                if (!matcher.methodMatches(methodName)) {
                    LOG.info("Method " + methodName + " does not match criteria. ");
                    continue;
                }

                for (Branch b : BranchPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).retrieveBranchesInMethod(className,
                        methodName)) {
                    if(!b.isInstrumented()) {
                        goals.add(createBranchCoverageTestFitness(b, true));
                        goals.add(createBranchCoverageTestFitness(b, false));
                    }
                }
            }
        }
        return goals;
    }
}
