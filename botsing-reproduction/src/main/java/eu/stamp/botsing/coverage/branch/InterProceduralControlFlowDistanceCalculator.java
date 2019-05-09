package eu.stamp.botsing.coverage.branch;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import org.evosuite.coverage.ControlFlowDistance;
import org.evosuite.coverage.TestCoverageGoal;
import org.evosuite.coverage.branch.Branch;
import org.evosuite.coverage.branch.ControlFlowDistanceCalculator;
import org.evosuite.graphs.GraphPool;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.ControlDependency;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testcase.execution.MethodCall;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InterProceduralControlFlowDistanceCalculator extends ControlFlowDistanceCalculator {


    public static ControlFlowDistance getDistance(ExecutionResult result, Branch branch, boolean value, String className, String methodName) {
        if (result != null && className != null && methodName != null) {
            if (branch == null && !value) {
                throw new IllegalArgumentException("expect distance for a root branch to always have value set to true");
            } else if (branch != null && (!branch.getMethodName().equals(methodName) || !branch.getClassName().equals(className))) {
                throw new IllegalArgumentException("expect explicitly given information about a branch to coincide with the information given by that branch");
            } else if (TestCoverageGoal.hasTimeout(result)) {
                return getTimeoutDistance(result, branch);
            } else if (branch == null) {
                return getRootDistance(result, className, methodName);
            } else {
                if (value) {
                    if (result.getTrace().getCoveredTrueBranches().contains(branch.getActualBranchId())) {
                        return new ControlFlowDistance(0, 0.0D);
                    }
                } else if (result.getTrace().getCoveredFalseBranches().contains(branch.getActualBranchId())) {
                    return new ControlFlowDistance(0, 0.0D);
                }

                ControlFlowDistance nonRootDistance = getNonRootDistance(result, branch, value);
                if (nonRootDistance == null) {
                    throw new IllegalStateException("expect getNonRootDistance to never return null");
                } else {
                    return nonRootDistance;
                }
            }
        } else {
            throw new IllegalArgumentException("null given");
        }
    }



    protected static ControlFlowDistance getNonRootDistance(ExecutionResult result,
                                                            Branch branch, boolean value) {

        if (branch == null){
            throw new IllegalStateException(
                    "expect this method only to be called if this goal does not try to cover the root branch");
        }


        String className = branch.getClassName();
        String methodName = branch.getMethodName();


        ControlFlowDistance r = new ControlFlowDistance();
        if(CrashProperties.integrationTesting){
            r.setApproachLevel(GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).getActualCFG("IntegrationTestingGraph","methodsIntegration").getDiameter() + 1);
        }else{
            r.setApproachLevel(branch.getInstruction().getActualCFG().getDiameter() + 1);
        }

//
        // Currently r is in the worst state (r set to MAX)

        // Minimal distance between target node and path
        for (MethodCall call : result.getTrace().getMethodCalls()) {
            // This loop is for all of the observed calls in the trace of the generated test execution.
                ControlFlowDistance d2;
                Set<Branch> handled = new HashSet<Branch>();
                d2 = getNonRootDistance(result, call, branch, value, className, methodName, handled);
                // if d2 is better than r
                if (d2.compareTo(r) < 0) {
                    r = d2;
                }
        }

        return r;
    }




    protected static ControlFlowDistance getNonRootDistance(ExecutionResult result,
                                                            MethodCall call, Branch branch, boolean value, String className,
                                                            String methodName, Set<Branch> handled) {

        nonRootDistancePrechecks(call,branch);


        if (handled.contains(branch)) {
            return worstPossibleDistanceForMethod(branch);
        }

        handled.add(branch);

        List<Double> trueDistances = call.trueDistanceTrace;
        List<Double> falseDistances = call.falseDistanceTrace;


        Set<Integer> branchTracePositions = determineBranchTracePositions(call, branch);

        if (!branchTracePositions.isEmpty()) {

            // branch was traced in given path
            ControlFlowDistance r = new ControlFlowDistance(0, Double.MAX_VALUE);

            for (Integer branchTracePosition : branchTracePositions)
                if (value)
                    r.setBranchDistance(Math.min(r.getBranchDistance(),
                            trueDistances.get(branchTracePosition)));
                else
                    r.setBranchDistance(Math.min(r.getBranchDistance(),
                            falseDistances.get(branchTracePosition)));

            if (r.getBranchDistance() == Double.MAX_VALUE)
                throw new IllegalStateException("should be impossible");

            //			result.intermediateDistances.put(branch, r);
            return r;
        }

        ControlFlowDistance controlDependenceDistance = getControlDependenceDistancesFor(result,
                call,
                branch.getInstruction(),
                className,
                methodName,
                handled);

        controlDependenceDistance.increaseApproachLevel();


        return controlDependenceDistance;
    }

    protected static void nonRootDistancePrechecks(MethodCall call,Branch branch){
        if (branch == null){
            throw new IllegalStateException(
                    "expect getNonRootDistance() to only be called if this goal's branch is not a root branch");
        }

        if (call == null){
            throw new IllegalArgumentException("null given");
        }

    }


    protected static ControlFlowDistance getControlDependenceDistancesFor(ExecutionResult result, MethodCall call, BytecodeInstruction instruction, String className, String methodName, Set<Branch> handled) {
        Set<ControlFlowDistance> cdDistances = getDistancesForControlDependentBranchesOf(result, call, instruction, className, methodName, handled);
        if (cdDistances == null) {
            throw new IllegalStateException("expect cdDistances to never be null");
        } else {
            return Collections.min(cdDistances);
        }
    }


    protected static Set<ControlFlowDistance> getDistancesForControlDependentBranchesOf(
            ExecutionResult result, MethodCall call, BytecodeInstruction instruction,
            String className, String methodName, Set<Branch> handled) {

        Set<ControlFlowDistance> r = new HashSet<ControlFlowDistance>();
        Set<ControlDependency> nextToLookAt = instruction.getControlDependencies();

        for (ControlDependency next : nextToLookAt) {
            if (instruction.equals(next.getBranch().getInstruction()))
                continue; // avoid loops

            boolean nextValue = next.getBranchExpressionValue();
            ControlFlowDistance nextDistance = getNonRootDistance(result, call,
                    next.getBranch(),
                    nextValue, className,
                    methodName, handled);
            assert (nextDistance != null);
            r.add(nextDistance);
        }

        if (r.isEmpty()) {
            r.add(new ControlFlowDistance());
        }

        return r;
    }

}
