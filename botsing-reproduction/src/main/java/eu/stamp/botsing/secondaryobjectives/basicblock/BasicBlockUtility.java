package eu.stamp.botsing.secondaryobjectives.basicblock;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import org.evosuite.TestGenerationContext;
import org.evosuite.graphs.GraphPool;
import org.evosuite.graphs.cfg.ActualControlFlowGraph;
import org.evosuite.graphs.cfg.BasicBlock;
import org.evosuite.testcase.TestChromosome;

import java.util.*;
import java.util.stream.Collectors;

public class BasicBlockUtility {

    public static boolean sameBasicBlockCoverage(Collection<CoveredBasicBlock> coveredBlocks1, Collection<CoveredBasicBlock> coveredBlocks2) {
        Collection<BasicBlock> fullyCovered1 = getFullyCoveredBasicBlocks(coveredBlocks1);
        Collection<BasicBlock> fullyCovered2 = getFullyCoveredBasicBlocks(coveredBlocks2);
        if(!fullyCovered1.equals(fullyCovered2)){
            return false;
        }

        Collection<BasicBlock> semiCovered1 = BasicBlockUtility.getSemiCoveredBasicBlocks(coveredBlocks1);
        Collection<BasicBlock> semiCovered2 = BasicBlockUtility.getSemiCoveredBasicBlocks(coveredBlocks1);
        return semiCovered1.equals(semiCovered2);
    }

    private static Set<BasicBlock> getFullyCoveredBasicBlocks(Collection<CoveredBasicBlock> coveredBlocks) {
        Set<BasicBlock> fullyCovered = new HashSet<>();
        for (CoveredBasicBlock coveredBasicBlock: coveredBlocks){
            if (coveredBasicBlock.isFullyCovered()){
                fullyCovered.add(coveredBasicBlock.getBasicBlock());
            }
        }

        return fullyCovered;
    }


    public static List<BasicBlock> getSemiCoveredBasicBlocks(Collection<CoveredBasicBlock> coveredBlocks) {
        Set<BasicBlock> semiCovered = new HashSet<>();
        for (CoveredBasicBlock coveredBasicBlock: coveredBlocks){
            if (!coveredBasicBlock.isFullyCovered()){
                semiCovered.add(coveredBasicBlock.getBasicBlock());
            }
        }
        return semiCovered.stream().collect(Collectors.toList());
    }

    public static BasicBlock findTheClosestBlock(List<BasicBlock> semiCoveredBasicBlocks, int targetLine){
        String targetClass = semiCoveredBasicBlocks.get(0).getClassName();
        String targetMethod = semiCoveredBasicBlocks.get(0).getMethodName();

        // Find the control flow graph of target method
        ActualControlFlowGraph targetMethodCFG = BasicBlockUtility.getTargetMethodCFG(targetClass,targetMethod);
        // Find a basic block which contains the target line
        BasicBlock targetBlock = findTargetBlock(targetMethodCFG,targetLine);

        BasicBlock result=null;
        int minimumDistance = Integer.MAX_VALUE;

        for (BasicBlock currentBlock: semiCoveredBasicBlocks){
            int distance = targetMethodCFG.getDistance(currentBlock,targetBlock);
            if (distance <= minimumDistance){
                result = currentBlock;
                minimumDistance = distance;
            }
        }
        if (result == null){
            throw new IllegalStateException("The selected basic block is null");
        }
        return result;
    }

    private static BasicBlock findTargetBlock(ActualControlFlowGraph targetMethodCFG, int targetLine) {
        // check the basic blocks in the targetMethodCFG iteratively
        List<BasicBlock> visitedBasicBlocks = new ArrayList<>();
        List<BasicBlock> BasicBlocksToVisit = new LinkedList<>();
        // Start with the entry point node
        BasicBlock entryBasicBlock = targetMethodCFG.getEntryPoint().getBasicBlock();
        BasicBlocksToVisit.add(entryBasicBlock);

        while (BasicBlocksToVisit.size() > 0) {
            // Get a basic block
            BasicBlock currentBasicBlock = BasicBlocksToVisit.remove(0);
            visitedBasicBlocks.add(currentBasicBlock);
            int firstLine = currentBasicBlock.getFirstLine();
            int lastLine = currentBasicBlock.getLastLine();
            if (targetLine >= firstLine && targetLine<= lastLine){
                return currentBasicBlock;
            }
        }
        throw new IllegalArgumentException("The target line is not available in the given control flow graph!");
    }


    public static ActualControlFlowGraph getTargetMethodCFG(String targetClass, String targetMethod){
        if(CrashProperties.integrationTesting){
            GraphPool graphPool = GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
            return graphPool.getActualCFG(targetClass,targetMethod);
        }else {
            GraphPool graphPool = GraphPool.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT());
            return graphPool.getActualCFG(targetClass,targetMethod);
        }
    }


    public static Set<Integer> detectInterestingCoveredLines(TestChromosome chromosome, BasicBlock targetBlock) {
        Set<Integer> result = new HashSet<>();

        Set<Integer> coveredLines  = chromosome.getLastExecutionResult().getTrace().getCoveredLines(targetBlock.getClassName());
        for (Integer line: coveredLines){
            if (line >= targetBlock.getFirstLine() && line <= targetBlock.getLastLine()){
                result.add(line);
            }
        }

        return result;
    }
}
