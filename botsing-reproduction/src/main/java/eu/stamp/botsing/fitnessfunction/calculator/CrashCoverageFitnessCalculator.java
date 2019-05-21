package eu.stamp.botsing.fitnessfunction.calculator;

/*-
 * #%L
 * botsing-reproduction
 * %%
 * Copyright (C) 2017 - 2018 eu.stamp-project
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.StackTrace;
import eu.stamp.botsing.coverage.branch.IntegrationTestingBranchCoverageFactory;
import eu.stamp.botsing.testgeneration.TestGenerationContextUtility;
import org.evosuite.coverage.ControlFlowDistance;
import org.evosuite.coverage.branch.BranchCoverageFactory;
import org.evosuite.coverage.branch.BranchCoverageTestFitness;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.BytecodeInstructionPool;
import org.evosuite.graphs.cfg.ControlDependency;
import org.evosuite.testcase.execution.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CrashCoverageFitnessCalculator {

    private static final Logger LOG = LoggerFactory.getLogger(CrashCoverageFitnessCalculator.class);

    StackTrace targetCrash;

    public CrashCoverageFitnessCalculator(StackTrace crash){
        targetCrash = crash;
    }

    public double getLineCoverageFitness( ExecutionResult result , int lineNumber) {
        StackTrace trace = targetCrash;
        return getLineCoverageFitness(result, trace, lineNumber);
    }

    public double getLineCoverageForFrame( ExecutionResult result, int frameLevel){
        StackTrace trace = targetCrash;
        StackTraceElement targetFrame = trace.getFrame(frameLevel);
        String methodName = TestGenerationContextUtility.derivingMethodFromBytecode(targetFrame.getClassName(), targetFrame.getLineNumber());
        int lineNumber = targetFrame.getLineNumber();
        List<BranchCoverageTestFitness> branchFitnesses = setupDependencies(targetFrame.getClassName(), methodName, targetFrame.getLineNumber());
        double lineCoverageFitness;
        if (result.getTrace().getCoverageData().containsKey(targetFrame.getClassName()) && result.getTrace().getCoverageData().get(targetFrame.getClassName()).containsKey(methodName)&& result.getTrace().getCoverageData().get(targetFrame.getClassName()).get(methodName).containsKey(lineNumber)) {
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

//
//    protected Map<String, Map<String, Map<Integer, Integer>>> getStackCoverage(ExecutionResult result){
//        Map<String, Map<String, Map<Integer, Integer>>> finalCoverage =  new HashMap<>();
//        Map<String, Map<String, Map<Integer, Integer>>> untouchedCoverage = new HashMap<>(result.getTrace().getCoverageData());
//        List<MethodCall> finishedMethods = new ArrayList<>(result.getTrace().getMethodCalls());
//        // Finished methods cannot be in the stack coverage
//        for(String className: untouchedCoverage.keySet()) {
//            for (String methodName : untouchedCoverage.get(className).keySet()) {
//                Map<Integer,Integer> finishedCount = countFinishedMethodLines(finishedMethods,className,methodName);
//                for (Integer line : untouchedCoverage.get(className).get(methodName).keySet()) {
//                    int lineCount = untouchedCoverage.get(className).get(methodName).get(line).intValue();
//                    if(finishedCount.containsKey(line)){
//                        if(finishedCount.get(line).intValue() < lineCount){
//                            updateFinalCoverage(finalCoverage,className,methodName,line,lineCount-finishedCount.get(line).intValue());
//                        }
//                    }else{
//                        updateFinalCoverage(finalCoverage,className,methodName,line,lineCount);
//                    }
//                }
//            }
//        }
//
//        return result.getTrace().getCoverageData();
//    }
//
//    private Map<Integer,Integer> countFinishedMethodLines(List<MethodCall> finishedMethods, String className, String methodName) {
//        Map<Integer,Integer> count = new HashMap<>();
//        for(MethodCall method: finishedMethods){
//            if(method.className.equals(className) && method.methodName.equals(methodName)){
//                for(Integer line: method.lineTrace){
//                    if(count.containsKey(line)){
//                        int oldCount = count.get(line).intValue();
//                        count.put(line,new Integer(oldCount+1));
//                    }else{
//                        count.put(line,new Integer(1));
//                    }
//                }
//            }
//        }
//
//        return count;
//    }
//
//    private void updateFinalCoverage(Map<String,Map<String,Map<Integer,Integer>>> finalCoverage, String className, String methodName, Integer line, int count) {
//        if(!finalCoverage.containsKey(className)){
//            finalCoverage.put(className,new HashMap<>());
//        }
//
//        if(!finalCoverage.get(className).containsKey(methodName)){
//            finalCoverage.get(className).put(methodName,new HashMap<>());
//        }
//        finalCoverage.get(className).get(methodName).put(line, new Integer(count));
//    }

    protected double getLineCoverageFitness(ExecutionResult result, StackTrace trace, int lineNumber) {
        int targetFrameLevel = trace.getNumberOfFrames();
        StackTraceElement targetFrame = trace.getFrame(targetFrameLevel);

        String methodName = TestGenerationContextUtility.derivingMethodFromBytecode(targetFrame.getClassName(), targetFrame.getLineNumber());
        List<BranchCoverageTestFitness> branchFitnesses = setupDependencies(targetFrame.getClassName(), methodName, targetFrame.getLineNumber());
        double lineCoverageFitness = 1.0;
        if (result.getTrace().getCoveredLines().contains(lineNumber)) {
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


    public double calculateFrameSimilarity( StackTraceElement[] trace) {
        return calculateFrameSimilarity(trace, targetCrash);
    }

    protected double calculateFrameSimilarity(StackTraceElement[] trace, StackTrace targetTrace) {
        int startPoint = 0;
        double result = 0.0;
        //iterating on the target stack trace

        int numberOfFrames = targetTrace.getNumberOfFrames();
        for (int frame_level=1;frame_level<=numberOfFrames;frame_level++){
            if (!targetTrace.getFrame(frame_level).getClassName().contains("reflect") && !targetTrace.getFrame(frame_level).getClassName().contains("invoke")){
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

    public double getFrameDistance(StackTraceElement targetFrame, StackTraceElement generatedFrame){
        String className =targetFrame.getClassName();
        if(className.contains("$") & !className.equals(generatedFrame.getClassName())){
            className = targetFrame.getClassName().split("\\$")[0];
        }
        double elementDistance;

        if (!className.equals(generatedFrame.getClassName())){
            elementDistance = 3.0;
        }else if (!targetFrame.getMethodName().equals(generatedFrame.getMethodName())) {
            elementDistance = 2.0;
        }else{
            elementDistance = normalize(Math.abs(targetFrame.getLineNumber() - generatedFrame.getLineNumber()));
            }

        return normalize(elementDistance);


    }



    private List<BranchCoverageTestFitness> setupDependencies(String className , String methodName, int lineNumber ) {
        BytecodeInstruction goalInstruction = BytecodeInstructionPool.getInstance(TestGenerationContextUtility.getTestGenerationContextClassLoader()).getFirstInstructionAtLineNumber(className, methodName, lineNumber);
        List<BranchCoverageTestFitness> branchCoverages = new ArrayList<>();
        if(goalInstruction == null){
            return branchCoverages;
        }
        // get control dependencies of the target node
        Set<ControlDependency> deps = goalInstruction.getControlDependencies();

        // Add control dependencies for calculating branch distances + approach level
        for (ControlDependency cd : deps) {
            BranchCoverageTestFitness singlefitness ;
            if(CrashProperties.integrationTesting){
                singlefitness = IntegrationTestingBranchCoverageFactory.createBranchCoverageTestFitness(cd);
            }else{
                singlefitness = BranchCoverageFactory.createBranchCoverageTestFitness(cd);
            }

            branchCoverages.add(singlefitness);
        }
        if (goalInstruction.isRootBranchDependent()) {
            if(CrashProperties.integrationTesting){
                branchCoverages.add(IntegrationTestingBranchCoverageFactory.createRootBranchTestFitness(goalInstruction));
            }else{
                branchCoverages.add(BranchCoverageFactory.createRootBranchTestFitness(goalInstruction));
            }

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


    private double normalize(double value) throws IllegalArgumentException {
        if (value < 0d) {
            throw new IllegalArgumentException("Values to normalize cannot be negative");
        }
        if (Double.isInfinite(value)) {
            return 1.0;
        }
        return value / (1.0 + value);
    }




}
