package eu.stamp.botsing.fitnessfunction.fitnessCalculator;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.StackTrace;
import org.evosuite.TestGenerationContext;
import org.evosuite.coverage.ControlFlowDistance;
import org.evosuite.coverage.branch.BranchCoverageFactory;
import org.evosuite.coverage.branch.BranchCoverageTestFitness;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.BytecodeInstructionPool;
import org.evosuite.graphs.cfg.ControlDependency;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.utils.LoggingUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CrashCoverageFitnessCalculator {

    public static double getLineCoverageFitness(ExecutionResult result , int lineNumber) {
        int targetFrameLevel = CrashProperties.getInstance().getStackTrace().getNumberOfFrames();
        StackTraceElement targetFrame = CrashProperties.getInstance().getStackTrace().getFrame(targetFrameLevel);

        CrashProperties.getTargetException();

        String methodName = derivingMethodFromBytecode(targetFrame.getClassName(), targetFrame.getMethodName(), targetFrame.getLineNumber());
        List<BranchCoverageTestFitness> branchFitnesses = setupDependencies(targetFrame.getClassName(), methodName, targetFrame.getLineNumber());
        double lineCoverageFitness = 1.0;
        if (result.getTrace().getCoveredLines().contains(lineNumber)) {
            lineCoverageFitness = 0.0;
        } else {
            double min = Double.MAX_VALUE;
            // Indicate minimum distance
            for (BranchCoverageTestFitness branchFitness : branchFitnesses) {
                // let's calculate the branch distance
                ControlFlowDistance distance = branchFitness.getBranchGoal().getDistance(result);
                double temp = distance.getResultingBranchFitness();

                if (temp == 0.0) {
                    // If the control dependency was covered, then likely
                    // an exception happened before the line was reached
                    temp = 1.0;
                } else {
                    temp = normalize(temp);
                }
                if (temp < min){
                    min = temp;}

            }

            lineCoverageFitness = min;

        }
        return lineCoverageFitness;
    }


    public static double calculateFrameSimilarity(StackTraceElement[] trace) {
        int startPoint = 0;
        double result = 0.0;
        //iterating on the target stack trace
        StackTrace targetTrace = CrashProperties.getInstance().getStackTrace();
        int numberOfFrames = targetTrace.getNumberOfFrames();
        for (int frame_level=1;frame_level<=numberOfFrames;frame_level++){
            if (!targetTrace.getFrame(frame_level).getClassName().contains("reflect") || !targetTrace.getFrame(frame_level).getClassName().contains("invoke")){
                // Check the selected frame from target stack trace on The generated stack trace frames
                StackTraceElement selectedFrame = targetTrace.getFrame(frame_level);
                double minDistance=1;
                for (int pos = startPoint ; pos < trace.length; pos++){
                    StackTraceElement frameOfGeneratedTrace = trace[pos];
                    if (!frameOfGeneratedTrace.getClassName().contains("evosuite")){
                        double tempDist = getFrameDistance(selectedFrame, frameOfGeneratedTrace) ;
                        if (tempDist < minDistance){
                            minDistance = tempDist;
                            startPoint = pos;
                        }
                    }
                }
                result += minDistance;
            }
        }
        return normalize(result);
    }

    public static double getFrameDistance(StackTraceElement targetFrame, StackTraceElement generatedFrame){


        if (!targetFrame.getClassName().equals(generatedFrame.getClassName())){
            return 3.0;
        }

        if (!targetFrame.getMethodName().equals(generatedFrame.getMethodName())) {
            return 2.0;
        }
        return normalize(Math.abs(targetFrame.getLineNumber() - generatedFrame.getLineNumber()));

    }



    private static List<BranchCoverageTestFitness> setupDependencies(String className , String methodName, int lineNumber ) {
        BytecodeInstruction goalInstruction = BytecodeInstructionPool.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT()).getFirstInstructionAtLineNumber(className, methodName, lineNumber);
        List<BranchCoverageTestFitness> branchCoverages = new ArrayList<>();
        if(goalInstruction == null){
            return branchCoverages;
        }

        Set<ControlDependency> deps = goalInstruction.getControlDependencies();

        for (ControlDependency cd : deps) {
            BranchCoverageTestFitness singlefitness = BranchCoverageFactory.createBranchCoverageTestFitness(cd);
            branchCoverages.add(singlefitness);
        }
        if (goalInstruction.isRootBranchDependent())
            branchCoverages.add(BranchCoverageFactory.createRootBranchTestFitness(goalInstruction));

        if (deps.isEmpty() && !goalInstruction.isRootBranchDependent())
            throw new IllegalStateException(
                    "expect control dependencies to be empty only for root dependent instructions: "
            );

        if (branchCoverages.isEmpty())
            throw new IllegalStateException(
                    "an instruction is at least on the root branch of it's method");

        branchCoverages.sort((a,b) -> a.compareTo(b));

        return branchCoverages;
    }


    private static double normalize(double value) throws IllegalArgumentException {
        if (value < 0d) {
            throw new IllegalArgumentException("Values to normalize cannot be negative");
        }
        if (Double.isInfinite(value)) {
            return 1.0;
        }
        return value / (1.0 + value);
    }


    private static  String derivingMethodFromBytecode(String className, String methodName, int lineNumber){
        List<BytecodeInstruction> instructions = BytecodeInstructionPool.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT()).getInstructionsIn(className);
        if (instructions != null) {
            for (BytecodeInstruction ins : instructions) {
                if(ins != null) {
                    if (ins.getLineNumber() == lineNumber){
                        String bytecodeMethodName = ins.getMethodName();
                        //						if (bytecodeMethodName.contains(methodName))
                        return bytecodeMethodName;
                    }
                } else {
                    LoggingUtils.getEvoLogger().error("CrashCoverageTestfitness.derivingMethodFromBytecode: instruction for this line number " + lineNumber+" was null!");
                }
            }
        } else {
            LoggingUtils.getEvoLogger().error("CrashCoverageTestfitness.derivingMethodFromBytecode: instruction for this class " + className +" was null!");
        }
        return null;
    }

}
