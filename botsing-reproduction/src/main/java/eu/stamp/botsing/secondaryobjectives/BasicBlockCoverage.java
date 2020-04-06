package eu.stamp.botsing.secondaryobjectives;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.StackTrace;
import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import eu.stamp.botsing.commons.testgeneration.TestGenerationContextUtility;
import eu.stamp.botsing.secondaryobjectives.basicblock.BasicBlockUtility;
import eu.stamp.botsing.secondaryobjectives.basicblock.CoveredBasicBlock;
import org.evosuite.TestGenerationContext;
import org.evosuite.ga.SecondaryObjective;
import org.evosuite.graphs.GraphPool;
import org.evosuite.graphs.cfg.ActualControlFlowGraph;
import org.evosuite.graphs.cfg.BasicBlock;
import org.evosuite.testcase.TestChromosome;

import java.util.*;

public class BasicBlockCoverage extends SecondaryObjective<TestChromosome> {
    @Override
    public int compareChromosomes(TestChromosome chromosome1, TestChromosome chromosome2) {
        // Get target method and target class
        String targetClass = getTargetClass(chromosome1,chromosome2);
        String targetMethod = getTargetMethod(chromosome1,chromosome2);

        Collection<CoveredBasicBlock> coveredBlocks1 = collectCoveredBasicBlocks(chromosome1,targetClass,targetMethod);
        Collection<CoveredBasicBlock> coveredBlocks2 = collectCoveredBasicBlocks(chromosome2,targetClass,targetMethod);


        if(BasicBlockUtility.sameBasicBlockCoverage(coveredBlocks1,coveredBlocks2)){
            return compareCoveredLines(chromosome1,chromosome2,BasicBlockUtility.getSemiCoveredBasicBlocks(coveredBlocks1));
        }else if(coveredBlocks1.containsAll(coveredBlocks2) || coveredBlocks2.containsAll(coveredBlocks1)){
            // chromosome 2 coverage is a subset of chromosome 1 coverage
            // or
            // chromosome 1 coverage is a subset of chromosome 2 coverage
            // the returned value is >0 if chromosome1 is a subset of chromosome2 and vice versa.
            return coveredBlocks2.size() - coveredBlocks1.size();
        }else {
            return 0;
        }
    }



    private int compareCoveredLines(TestChromosome chromosome1, TestChromosome chromosome2, List<BasicBlock> semiCoveredBasicBlocks) {
        // First, we check if we have any semiCoveredBlock
        if (semiCoveredBasicBlocks.isEmpty()){
            return 0;
        }
        // then we select the target block
        BasicBlock targetBlock;
        if (semiCoveredBasicBlocks.size() == 1){
            // if we only have one semiCoveredBlock, we will select it  as our target block
            targetBlock = semiCoveredBasicBlocks.get(0);
        }else{
            // if we have more than one block, we select the closest one to the target line
            targetBlock = BasicBlockUtility.findTheClosestBlock(semiCoveredBasicBlocks, getTargetLine(chromosome1,chromosome2));
        }


        // find the covered lines in the target method by the given chromosome
        Collection<Integer> coveredLines1  = BasicBlockUtility.detectInterestingCoveredLines(chromosome1,targetBlock);
        Collection<Integer> coveredLines2  = BasicBlockUtility.detectInterestingCoveredLines(chromosome2,targetBlock);

        if (coveredLines1.equals(coveredLines2)){
            // Same coverage
            return 0;
        }

        // the returned value is >0 if the number of covered lines by chromosome2 is more than chromosome1 and vice versa
        return coveredLines2.size() - coveredLines1.size();
    }

    // toDo: Should we filter out the irrelevant basic blocks?
    private Collection<CoveredBasicBlock> collectCoveredBasicBlocks(TestChromosome chromosome, String targetClass, String targetMethod) {
        Set<CoveredBasicBlock> coveredBasicBlocks = new HashSet<>();

        // Find the control flow graph of target method
        ActualControlFlowGraph targetMethodCFG = BasicBlockUtility.getTargetMethodCFG(targetClass,targetMethod);

        // find the covered lines in the target method by the given chromosome
        Set<Integer> coveredLines  = chromosome.getLastExecutionResult().getTrace().getCoveredLines(targetClass);

        // check the basic blocks in the targetMethodCFG iteratively
        List<BasicBlock> visitedBasicBlocks = new ArrayList<>();
        List<BasicBlock> BasicBlocksToVisit = new LinkedList<>();
        // Start with the entry point node
        BasicBlock entryBasicBlock = targetMethodCFG.getEntryPoint().getBasicBlock();
        BasicBlocksToVisit.add(entryBasicBlock);

        while (BasicBlocksToVisit.size() > 0){
            // Get a basic block
            BasicBlock currentBasicBlock = BasicBlocksToVisit.remove(0);
            visitedBasicBlocks.add(currentBasicBlock);
            // check if it is covered
            if(isTouched(currentBasicBlock,coveredLines)){
                // if it is covered, first, we add it to the final list.
                coveredBasicBlocks.add(new CoveredBasicBlock(currentBasicBlock,isFullyCovered(currentBasicBlock,coveredLines)));
                // Second we check its children to check them too.
                for (BasicBlock child: targetMethodCFG.getChildren(currentBasicBlock)){
                    if(!visitedBasicBlocks.contains(child)){
                        BasicBlocksToVisit.add(child);
                    }
                }
            }
        }

        return coveredBasicBlocks;
    }

    private boolean isFullyCovered(BasicBlock currentBasicBlock, Set<Integer> coveredLines) {
        if(currentBasicBlock.isEntryBlock()){
            return true;
        }
        int lastLineNumber = currentBasicBlock.getLastLine();

        return coveredLines.contains(lastLineNumber);
    }

    private boolean isTouched(BasicBlock currentBasicBlock, Set<Integer> coveredLines) {
        if(currentBasicBlock.isEntryBlock()){
            return true;
        }
        int firstLineNumber = currentBasicBlock.getFirstLine();

        return coveredLines.contains(firstLineNumber);
    }

    private String getTargetMethod(TestChromosome chromosome1, TestChromosome chromosome2) {
        if(CrashProperties.integrationTesting){
            // ToDo: Complete this
            return null;
        }else{
            // target method is fixed
            StackTrace crash  = CrashProperties.getInstance().getStackTrace(0);

            return TestGenerationContextUtility.derivingMethodFromBytecode(CrashProperties.integrationTesting,crash.getTargetClass(), crash.getTargetLine());
//            return CrashProperties.getInstance().getStackTrace(0).getTargetMethod();
        }

    }

    private String getTargetClass(TestChromosome chromosome1, TestChromosome chromosome2) {
        if(CrashProperties.integrationTesting){
            // ToDo: Complete this
            return null;
        }else{
            // target class is fixed
            return CrashProperties.getInstance().getStackTrace(0).getTargetClass();
        }
    }

    private int getTargetLine(TestChromosome chromosome1, TestChromosome chromosome2) {
        if(CrashProperties.integrationTesting){
            // ToDo: Complete this
            return 0;
        }else{
            // target method is fixed
            return CrashProperties.getInstance().getStackTrace(0).getTargetLine();
        }

    }

    @Override
    public int compareGenerations(TestChromosome parent1, TestChromosome parent2,
                                  TestChromosome child1, TestChromosome child2) {
        return 0;
    }
}
