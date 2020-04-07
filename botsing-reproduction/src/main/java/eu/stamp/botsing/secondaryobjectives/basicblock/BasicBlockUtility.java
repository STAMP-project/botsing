package eu.stamp.botsing.secondaryobjectives.basicblock;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.StackTrace;
import eu.stamp.botsing.commons.testgeneration.TestGenerationContextUtility;
import eu.stamp.botsing.fitnessfunction.calculator.CrashCoverageFitnessCalculator;
import org.evosuite.graphs.GraphPool;
import org.evosuite.graphs.cfg.ActualControlFlowGraph;
import org.evosuite.graphs.cfg.BasicBlock;
import org.evosuite.testcase.TestChromosome;

import java.util.*;
import java.util.stream.Collectors;

public class BasicBlockUtility {

    /*
     Checks if the given CoveredBasicBlocks has the same coverage (returns true) or not.
    */
    public static boolean sameBasicBlockCoverage(Collection<CoveredBasicBlock> coveredBlocks1, Collection<CoveredBasicBlock> coveredBlocks2) {
        Collection<BasicBlock> fullyCovered1 = getFullyCoveredBasicBlocks(coveredBlocks1);
        Collection<BasicBlock> fullyCovered2 = getFullyCoveredBasicBlocks(coveredBlocks2);
        if(!fullyCovered1.equals(fullyCovered2)){
            return false;
        }

        Collection<BasicBlock> semiCovered1 = getSemiCoveredBasicBlocks(coveredBlocks1);
        Collection<BasicBlock> semiCovered2 = getSemiCoveredBasicBlocks(coveredBlocks2);
        return semiCovered1.equals(semiCovered2);
    }
    /*
         Returns the fully-covered basic blocks
     */
    private static Set<BasicBlock> getFullyCoveredBasicBlocks(Collection<CoveredBasicBlock> coveredBlocks) {
        Set<BasicBlock> fullyCovered = new HashSet<>();
        for (CoveredBasicBlock coveredBasicBlock: coveredBlocks){
            if (coveredBasicBlock.isFullyCovered()){
                fullyCovered.add(coveredBasicBlock.getBasicBlock());
            }
        }

        return fullyCovered;
    }

    /*
        Returns the semi-covered basic blocks
    */
    public static List<BasicBlock> getSemiCoveredBasicBlocks(Collection<CoveredBasicBlock> coveredBlocks) {
        Set<BasicBlock> semiCovered = new HashSet<>();
        for (CoveredBasicBlock coveredBasicBlock: coveredBlocks){
            if (!coveredBasicBlock.isFullyCovered()){
                semiCovered.add(coveredBasicBlock.getBasicBlock());
            }
        }
        return semiCovered.stream().collect(Collectors.toList());
    }

    /*
    Search the given actual control flow graph to find the closest basic block to the given line number.
    */
    private static BasicBlock findTheClosestBlock(List<BasicBlock> semiCoveredBasicBlocks, int targetLine){
        String targetClass = semiCoveredBasicBlocks.get(0).getClassName();
        String targetMethod = semiCoveredBasicBlocks.get(0).getMethodName();

        // Find the control flow graph of target method
        ActualControlFlowGraph targetMethodCFG = getTargetMethodCFG(targetClass,targetMethod);
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

    /*
        Search the given actual control flow graph to find a basic block, which contains the given line number.
     */
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
            for (BasicBlock child: targetMethodCFG.getChildren(currentBasicBlock)){
                if (!visitedBasicBlocks.contains(child)){
                    BasicBlocksToVisit.add(child);
                }
            }
        }
        throw new IllegalArgumentException("The target line is not available in the given control flow graph!");
    }


    /*
        Returns the actual control flow graph of the requested method
     */
    private static ActualControlFlowGraph getTargetMethodCFG(String targetClass, String targetMethod){
        GraphPool graphPool = GraphPool.getInstance(TestGenerationContextUtility.getTestGenerationContextClassLoader(false));
        return graphPool.getActualCFG(targetClass,targetMethod);
    }

    /*
        Returns the lines that their coverage is important for us in the current comparison
     */
    private static Set<Integer> detectInterestingCoveredLines(TestChromosome chromosome, BasicBlock targetBlock, int targetLine) {
        Set<Integer> result = new HashSet<>();
        int lastLine = Integer.min(targetLine,targetBlock.getLastLine());

        Set<Integer> coveredLines  = chromosome.getLastExecutionResult().getTrace().getCoveredLines(targetBlock.getClassName());

        for (Integer line: coveredLines){
            if (line >= targetBlock.getFirstLine() && line <= lastLine){
                result.add(line);
            }
        }

        return result;
    }

    /*
    Checks if the fully covered blocks in the first parameter (coveredBlocks1) is a subset of fully covered blocks in the second parameter (coveredBlocks2)
     */
    public static boolean isSubset(Collection<CoveredBasicBlock> coveredBlocks1, Collection<CoveredBasicBlock> coveredBlocks2) {

        Collection<BasicBlock> fullyCovered1 = getFullyCoveredBasicBlocks(coveredBlocks1);
        Collection<BasicBlock> fullyCovered2 = getFullyCoveredBasicBlocks(coveredBlocks2);
        if(!fullyCovered2.containsAll(fullyCovered1)){
            return false;
        }

        Collection<BasicBlock> semiCovered1 = getSemiCoveredBasicBlocks(coveredBlocks1);
        Collection<BasicBlock> semiCovered2 = getSemiCoveredBasicBlocks(coveredBlocks1);
        return semiCovered2.containsAll(semiCovered1);
    }

    /*
    Returns the coverage size. The fully covered blocks are counted two times.
     */
    public static int getCoverageSize(Collection<CoveredBasicBlock> coveredBlocks) {
        Collection<BasicBlock> fullyCovered = getFullyCoveredBasicBlocks(coveredBlocks);
        Collection<BasicBlock> semiCovered = getSemiCoveredBasicBlocks(coveredBlocks);

        return fullyCovered.size()*2+ semiCovered.size();
    }

    /*
        Checks if the given chromosome has covered all of the given basic block
     */
    private static boolean isFullyCovered(BasicBlock currentBasicBlock, Set<Integer> coveredLines, int targetLine) {
        if(currentBasicBlock.isEntryBlock()){
            return true;
        }
        int lastLineNumber = currentBasicBlock.getLastLine();
        if (targetLine < lastLineNumber){
            lastLineNumber = targetLine;
        }

        return coveredLines.contains(lastLineNumber);
    }

    /*
        Checks if the given chromosome has reached to the given basic block
     */
    private static boolean isTouched(BasicBlock currentBasicBlock, Set<Integer> coveredLines) {
        if(currentBasicBlock.isEntryBlock()){
            return true;
        }
        int firstLineNumber = currentBasicBlock.getFirstLine();

        return coveredLines.contains(firstLineNumber);
    }

    /*
        Returns all of the basic blocks in the target method, which are covered (either fully or semi) by the given chromosome.
     */
    // toDo: Should we filter out the irrelevant basic blocks?
    public static Collection<CoveredBasicBlock> collectCoveredBasicBlocks(TestChromosome chromosome, String targetClass, String targetMethod, int targetLine) {
        Set<CoveredBasicBlock> coveredBasicBlocks = new HashSet<>();

        // Find the control flow graph of target method
        ActualControlFlowGraph targetMethodCFG = getTargetMethodCFG(targetClass,targetMethod);

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
                coveredBasicBlocks.add(new CoveredBasicBlock(currentBasicBlock,isFullyCovered(currentBasicBlock,coveredLines,targetLine)));
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


    /*
    Check the line coverage in the semi covered blocks. More line coverage is better
     */

    public static int compareCoveredLines(TestChromosome chromosome1, TestChromosome chromosome2, List<BasicBlock> semiCoveredBasicBlocks, int targetLine) {

        // First, we check if we have any semiCoveredBlock
        if (semiCoveredBasicBlocks.isEmpty()){
            return 0;
        }

        // Then, we remove blocks, in which the target block is not accessible
        String targetClass = semiCoveredBasicBlocks.get(0).getClassName();
        String targetMethod = semiCoveredBasicBlocks.get(0).getMethodName();
        ActualControlFlowGraph targetMethodCFG = getTargetMethodCFG(targetClass,targetMethod);
        BasicBlock targetBlock = findTargetBlock(targetMethodCFG,targetLine);
        Iterator<BasicBlock> iter = semiCoveredBasicBlocks.iterator();
        while (iter.hasNext()){
            BasicBlock scBlock = iter.next();
            if (targetMethodCFG.getDistance(scBlock,targetBlock)<0){
                iter.remove();
            }
        }

        // We continue if we still have any candidate for line comparison
        if (semiCoveredBasicBlocks.isEmpty()){
            return 0;
        }

        // Next, we select the target block
        BasicBlock targetSemiCoveredBlock;
        if (semiCoveredBasicBlocks.size() == 1){
            // if we only have one semiCoveredBlock, we will select it  as our target block
            targetSemiCoveredBlock = semiCoveredBasicBlocks.get(0);
        }else{
            // if we have more than one block, we select the closest one to the target line
            targetSemiCoveredBlock = findTheClosestBlock(semiCoveredBasicBlocks, targetLine);
        }



        // find the covered lines in the target method by the given chromosome
        Collection<Integer> coveredLines1  = detectInterestingCoveredLines(chromosome1,targetSemiCoveredBlock,targetLine);
        Collection<Integer> coveredLines2  = detectInterestingCoveredLines(chromosome2,targetSemiCoveredBlock,targetLine);

        if (coveredLines1.equals(coveredLines2)){
            // Same coverage
            return 0;
        }

        // the returned value is >0 if the number of covered lines by chromosome2 is more than chromosome1 and vice versa
        return coveredLines2.size() - coveredLines1.size();
    }

    /*
   Returns the uncovered frame in the minimum level
     */
    public static StackTraceElement findFirstUncoveredFrame(StackTrace crash, TestChromosome chromosome1, TestChromosome chromosome2) {
        int UncoveredFrameLevel = Integer.min(findFirstUncoveredFrame(chromosome1),findFirstUncoveredFrame(chromosome2));
        StackTraceElement UncoveredFrame = crash.getFrame(UncoveredFrameLevel);

        return UncoveredFrame;
    }

    /*
            Returns the first frame which is not covered by the given chromosome
    */
    private static int findFirstUncoveredFrame(TestChromosome chromosome) {
        StackTrace crash  = CrashProperties.getInstance().getStackTrace(0);
        CrashCoverageFitnessCalculator fitnessCalculator = new CrashCoverageFitnessCalculator(crash);
        int frameLevel = crash.getPublicTargetFrameLevel();

        while (frameLevel>0){
            if (isFrameCovered(fitnessCalculator, frameLevel,chromosome)){
                frameLevel--;
            }else {
                return frameLevel;
            }
        }

        // Covered all of the frames. So, we return the deepest frame.
        return 1;
    }

    /*
        Checks if the line indicated by the given frameLevel is covered by the given chromosome
    */
    private static boolean isFrameCovered(CrashCoverageFitnessCalculator fitnessCalculator, int frameLevel, TestChromosome chromosome) {
        if(fitnessCalculator.getLineCoverageForFrame(chromosome.getLastExecutionResult(),frameLevel) == 0){
            return true;
        }

        return false;
    }
}
