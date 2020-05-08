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
import eu.stamp.botsing.commons.testgeneration.TestGenerationContextUtility;
import org.evosuite.coverage.ControlFlowDistance;
import org.evosuite.coverage.branch.BranchCoverageFactory;
import org.evosuite.coverage.branch.BranchCoverageTestFitness;
import org.evosuite.coverage.exception.ExceptionCoverageHelper;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.BytecodeInstructionPool;
import org.evosuite.graphs.cfg.ControlDependency;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testcase.execution.MethodCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CrashCoverageFitnessCalculator {

    private static final Logger LOG = LoggerFactory.getLogger(CrashCoverageFitnessCalculator.class);

    private StackTrace targetCrash;
    LinkedList<LinkedList<Integer>> tracking = new LinkedList();
    int irrelevantFrameCounter=0;

    public CrashCoverageFitnessCalculator(StackTrace crash){
        targetCrash = crash;
    }

    public double getLineCoverageForFrame( ExecutionResult result, int frameLevel){
        // If the frame is pointong to one of the dependencies of SUT, we just assume that the distance is zero
        if(targetCrash.isIrrelevantFrame(frameLevel)){
            irrelevantFrameCounter++;
            return 0.0;
        }
        // Check if we have the  coverage in the right depth or not
        int callDepth = targetCrash.getPublicTargetFrameLevel() - frameLevel + 1 - irrelevantFrameCounter;
        StackTraceElement targetFrame = targetCrash.getFrame(frameLevel);
        String methodName = TestGenerationContextUtility.derivingMethodFromBytecode(CrashProperties.integrationTesting, targetFrame.getClassName(), targetFrame.getLineNumber());
        boolean found = findMethodCallsInDepth(result,methodName,targetFrame.getLineNumber(),callDepth);
        // If we find it, we can return zero value for the fitness
        if(found){
            return 0.0;
        }
        // Otherwise, we need to calculate the distance

        // Clear tracking variables
        tracking.clear();
        irrelevantFrameCounter=0;

        // Calculate the distance
        return calculateBranchDistanceAndApproachLevel(result);
    }


//    private double calculateDistance(ExecutionResult result){
//        if(true){
//            // lets calculate the distance using the classical approach level + branch distance
//            return calculateBranchDistanceAndApproachLevel(result);
//        }else{
//            // lets use the the basic block distance
//            return computeBasicBlockDistance(result);
//        }
//    }

    protected boolean findMethodCallsInDepth(ExecutionResult result, String methodName,int lineNumber, int callDepth) {
        boolean found = false;
        List<MethodCall> callChains = result.getTrace().getMethodCalls();
        callChains.addAll(result.getTrace().getUnfinishedCalls());
        for(MethodCall call: callChains){
            if(call.callDepth == callDepth && call.methodName.equals(methodName) && call.lineTrace.contains(lineNumber)){
                // Check the caller method id

                // If we are in the first level the level of the caller is always 0
                if(callDepth == 1 && call.callerId == 0 ){
                    found = true;
                    LinkedList<Integer> newChain = new LinkedList();
                    newChain.push(call.methodId);
                    tracking.push(newChain);
                // For next levels:
                }else if (callDepth > 1){
                    int index = getParentsTrackIndex(call);
                    if(index != -1){
                        found = true;
                        tracking.get(index).push(call.methodId);
                    }
                }
            }
        }

        return found;
    }

    private int getParentsTrackIndex(MethodCall call) {
        for(int index=0;index<tracking.size();index++){
            if(tracking.get(index).peek() == call.callerId){
                return index;
            }
        }
        return -1;
    }

    public double getLineCoverageFitness(ExecutionResult result, int lineNumber) {
        // If line is covered, we the line distance is zero
        if (result.getTrace().getCoveredLines().contains(lineNumber)) {
            return  0.0;
        }

        return calculateBranchDistanceAndApproachLevel(result);
    }


    private double calculateBranchDistanceAndApproachLevel(ExecutionResult result) {
        // Get the target frame
        StackTraceElement targetFrame = this.targetCrash.getFrame(targetCrash.getNumberOfFrames());
        // Get the target method name
        String methodName = TestGenerationContextUtility.derivingMethodFromBytecode(CrashProperties.integrationTesting,targetFrame.getClassName(), targetFrame.getLineNumber());

        // get control dependent branches
        List<BranchCoverageTestFitness> branchFitnesses = setupDependencies(targetFrame.getClassName(), methodName, targetFrame.getLineNumber());

        double lineCoverageFitness = Double.MAX_VALUE;
        // Indicate minimum distance
        for (BranchCoverageTestFitness branchFitness : branchFitnesses) {
            // let's calculate the branch distance
            double distance = computeBranchDistance(branchFitness, result);
            lineCoverageFitness = Math.min(lineCoverageFitness, distance);
        }

        return lineCoverageFitness;
    }

    private double computeBasicBlockDistance(ExecutionResult result) {
        // Get the target frame
        StackTraceElement targetFrame = this.targetCrash.getFrame(targetCrash.getNumberOfFrames());
        // Get the target method name
        String methodName = TestGenerationContextUtility.derivingMethodFromBytecode(CrashProperties.integrationTesting,targetFrame.getClassName(), targetFrame.getLineNumber());

        // get control dependent branches
        List<BranchCoverageTestFitness> branchFitnesses = setupDependencies(targetFrame.getClassName(), methodName, targetFrame.getLineNumber());
        

        return Double.MAX_VALUE;
    }

    protected double computeBranchDistance(BranchCoverageTestFitness branchFitness, ExecutionResult result){
        ControlFlowDistance distance = branchFitness.getBranchGoal().getDistance(result);
        double value = distance.getResultingBranchFitness();

        if (value == 0.0) {
            // If the control dependency was covered, then likely
            // an exception happened before the line was reached
            value = normalize(1.0);
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
        BytecodeInstruction goalInstruction = BytecodeInstructionPool.getInstance(TestGenerationContextUtility.getTestGenerationContextClassLoader(CrashProperties.integrationTesting)).getFirstInstructionAtLineNumber(className, methodName, lineNumber);
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


    public void setTargetCrash(StackTrace targetCrash) {
        this.targetCrash = targetCrash;
    }

    public boolean sameException(ExecutionResult executionResult) {
        for (Integer ExceptionLocator : executionResult.getPositionsWhereExceptionsWereThrown()) {
            String thrownException = ExceptionCoverageHelper.getExceptionClass(executionResult, ExceptionLocator).getName();
            if (thrownException.equals(targetCrash.getExceptionType())){
                double tempFitness = calculateFrameSimilarity( executionResult.getExceptionThrownAtPosition(ExceptionLocator).getStackTrace());
                if (tempFitness == 0.0){
                    return true;
                }
            }
        }

        return false;
    }
}
