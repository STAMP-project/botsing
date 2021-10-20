package eu.stamp.botsing.secondaryobjectives;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.StackTrace;
import eu.stamp.botsing.commons.testgeneration.TestGenerationContextUtility;
import eu.stamp.botsing.fitnessfunction.utils.CrashDistanceEvolution;
import eu.stamp.botsing.secondaryobjectives.basicblock.BasicBlockUtility;
import org.evosuite.TestGenerationContext;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.SecondaryObjective;
import org.evosuite.graphs.cfg.ActualControlFlowGraph;
import org.evosuite.graphs.cfg.BasicBlock;
import org.evosuite.graphs.cfg.BytecodeInstructionPool;
import org.evosuite.testcase.TestChromosome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class BasicBlockCoverage extends SecondaryObjective<TestChromosome> {
    private static final Logger LOG = LoggerFactory.getLogger(BasicBlockCoverage.class);
    BasicBlockUtility basicBlockUtility;
    public BasicBlockCoverage(){
        super();
        basicBlockUtility = new BasicBlockUtility();
    }

    @Override
    public int compareChromosomes(TestChromosome chromosome1, TestChromosome chromosome2) {
        double bestFF = CrashDistanceEvolution.getInstance().getBestFitnessValue();
        Map.Entry<FitnessFunction<?>, Double> entry = chromosome1.getFitnessValues().entrySet().iterator().next();
        if (bestFF < entry.getValue()){
            return 0;
        }

        int finalValue;


        // Get target method and target class
        String targetClass = getTargetClass(chromosome1,chromosome2);
        String targetMethod = getTargetMethod(chromosome1,chromosome2);
        int targetLine = getTargetLine(chromosome1,chromosome2);
        ActualControlFlowGraph targetMethodCFG = BytecodeInstructionPool.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT()).getFirstInstructionAtLineNumber(targetClass,targetMethod,targetLine).getActualCFG();
        BasicBlock targetBlock = BytecodeInstructionPool.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT()).getFirstInstructionAtLineNumber(targetClass,targetMethod,targetLine).getBasicBlock();
        // Collect lines in target method which are covered by each of the given chromosomes.
        Set<Integer> coveredLines1 = basicBlockUtility.getCoveredLines(chromosome1, targetMethodCFG);
        Set<Integer> coveredLines2 = basicBlockUtility.getCoveredLines(chromosome2, targetMethodCFG);

        // Chromosome 1 and 2 covered the target line and the only remaining thing is covering the right branch. In this case, BBC cannot help.
        if(coveredLines1.contains(targetLine) && coveredLines2.contains(targetLine)){
            return 0;
        }

        // If covered lines are identical, we dont need to check the coverage.
        if(coveredLines1.equals(coveredLines2)){
            return 0;
        }

        // Detect last covered control dependent node in the given chromosomes. This node is the same as we have same approach level and branch distance.
        BasicBlock closestCoveredControlDependency = basicBlockUtility.getClosestCoveredControlDependency(targetBlock, coveredLines1);

        // Calcualte the covered blocks for both chromosomes
        List<Set<BasicBlock>> coveredBasicBlocks1 = basicBlockUtility.collectCoveredBasicBlocks(targetMethodCFG,targetLine,targetBlock,coveredLines1,closestCoveredControlDependency);
        List<Set<BasicBlock>> coveredBasicBlocks2 = basicBlockUtility.collectCoveredBasicBlocks(targetMethodCFG,targetLine,targetBlock,coveredLines2,closestCoveredControlDependency);


        // Collect FCB1 and SCB1
        Set<BasicBlock> FCB1 = coveredBasicBlocks1.get(0);
        Set<BasicBlock> semiCovered1 = coveredBasicBlocks1.get(1);
        // SCB is the closest semi covered block to the target block.
        BasicBlock SCB1 = basicBlockUtility.findTheClosestBlock(semiCovered1,targetMethodCFG,targetBlock);

        // Collect FCB2 and SCB2
        Set<BasicBlock> FCB2 = coveredBasicBlocks2.get(0);
        Set<BasicBlock> semiCovered2 = coveredBasicBlocks2.get(1);
        BasicBlock SCB2 = basicBlockUtility.findTheClosestBlock(semiCovered2,targetMethodCFG,targetBlock);

        // Check if both chromosomes get stuck in a same basic block
        if(basicBlockUtility.goDeeper(FCB1,SCB1,FCB2,SCB2)){
            finalValue=basicBlockUtility.compareCoveredLines(chromosome1,chromosome2,SCB1,targetLine);
            // logging
            if (finalValue != 0){
                LOG.info("*C1*");
            }
            // Otherwise, check heck if one of the chromosomes has more coverage in the effective basic blocks
        }else if (basicBlockUtility.oneChromosomeHasMoreCoveredBlocks(FCB1,SCB1,FCB2,SCB2)){

            // chromosome 2 coverage is a subset of chromosome 1 coverage
            // or
            // chromosome 1 coverage is a subset of chromosome 2 coverage
            // the returned value is >0 if chromosome1 is a subset of chromosome2 and vice versa.

            finalValue = basicBlockUtility.getCoverageSize(FCB2) - basicBlockUtility.getCoverageSize(FCB1);
            // logging
            if (finalValue != 0){
                LOG.info("*C2*");
            }
        }else{
            // Here, we cannot say which test is better. So, we set the final value to zero.
            finalValue= 0;
        }

        return finalValue;
    }

    @Override
    public int compareGenerations(TestChromosome parent1, TestChromosome parent2,
                                  TestChromosome child1, TestChromosome child2) {
        return 0;
    }



    private String getTargetMethod(TestChromosome chromosome1, TestChromosome chromosome2) {
        StackTrace crash  = CrashProperties.getInstance().getStackTrace(0);
        if(CrashProperties.integrationTesting){
            // Find the first uncovered frame by the given chromosomes. We always choose the deepest first uncovered frame.
            StackTraceElement uncoveredFrame = basicBlockUtility.findFirstUncoveredFrame(crash,chromosome1,chromosome2);
            // Return the method name in the detected frame level
            return TestGenerationContextUtility.derivingMethodFromBytecode(true,uncoveredFrame.getClassName(),uncoveredFrame.getLineNumber());
        }else{
            // target method is fixed
            return TestGenerationContextUtility.derivingMethodFromBytecode(false,crash.getTargetClass(), crash.getTargetLine());
        }

    }

    private String getTargetClass(TestChromosome chromosome1, TestChromosome chromosome2) {
        StackTrace crash  = CrashProperties.getInstance().getStackTrace(0);
        if(CrashProperties.integrationTesting){
            // Find the first uncovered frame by the given chromosomes. We always choose the deepest first uncovered frame.
            StackTraceElement uncoveredFrame = basicBlockUtility.findFirstUncoveredFrame(crash,chromosome1,chromosome2);
            // Return the className in the detected frame
            return uncoveredFrame.getClassName();
        }else{
            // target class is fixed
            return crash.getTargetClass();
        }
    }

    private int getTargetLine(TestChromosome chromosome1, TestChromosome chromosome2) {
        StackTrace crash  = CrashProperties.getInstance().getStackTrace(0);
        if(CrashProperties.integrationTesting){
            // Find the first uncovered frame by the given chromosomes. We always choose the deepest first uncovered frame.
            StackTraceElement uncoveredFrame = basicBlockUtility.findFirstUncoveredFrame(crash,chromosome1,chromosome2);
            // Return the line number indicated in the detected frame
            return uncoveredFrame.getLineNumber();
        }else{
            // target line is fixed
            return crash.getTargetLine();
        }

    }
}
