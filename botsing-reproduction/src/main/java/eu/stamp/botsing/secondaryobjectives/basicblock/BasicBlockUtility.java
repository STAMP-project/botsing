package eu.stamp.botsing.secondaryobjectives.basicblock;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.StackTrace;
import eu.stamp.botsing.commons.testgeneration.TestGenerationContextUtility;
import eu.stamp.botsing.fitnessfunction.calculator.CrashCoverageFitnessCalculator;
import org.evosuite.TestGenerationContext;
import org.evosuite.graphs.GraphPool;
import org.evosuite.graphs.cfg.ActualControlFlowGraph;
import org.evosuite.graphs.cfg.BasicBlock;
import org.evosuite.graphs.cfg.ControlDependency;
import org.evosuite.testcase.TestChromosome;

import java.util.*;

public class BasicBlockUtility {



    /*
    Check if the given chromosomes got stuck in the same SCB
    */
    public boolean goDeeper(Set<BasicBlock> FCB1, BasicBlock SCB1, Set<BasicBlock> FCB2, BasicBlock SCB2) {

        boolean FCB2_isSubsetEqual_FCB1 = FCB1.containsAll(FCB2); // FCB2 ⊆ FCB1
        boolean FCB1_isSubsetEqual_FCB2 = FCB2.containsAll(FCB1); // FCB1 ⊆ FCB2

        boolean SCBs_equal; // SCB1 == SCB2
        if(SCB1 == null || SCB2 == null){
            // if SCB is null, it means that chromosomes did not stuck in any block.
            SCBs_equal = false;
        }else{
            SCBs_equal = (SCB1.equals(SCB2));
        }

        return ((FCB1_isSubsetEqual_FCB2 || FCB2_isSubsetEqual_FCB1) && SCBs_equal);
    }

    /*
    Check if one chromosome has more coverage in the effective blocks.
    */
    public boolean oneChromosomeHasMoreCoveredBlocks(Set<BasicBlock> FCB1, BasicBlock SCB1, Set<BasicBlock> FCB2, BasicBlock SCB2) {
        boolean FCB2_isSubsetEqual_FCB1 = FCB1.containsAll(FCB2); // FCB2 ⊆ FCB1
        boolean FCB1_isSubsetEqual_FCB2 = FCB2.containsAll(FCB1); // FCB1 ⊆ FCB2

        boolean SCB1_in_FCB2 = FCB2.contains(SCB1) || SCB1 == null; // SCB1 ∈ FCB2
        boolean SCB2_in_FCB1 = FCB1.contains(SCB2) || SCB2 == null; // SCB2 ∈ FCB1

        // In this case, the given chromosomes has the identical coverage in the effective blocks.
        if(SCB1 == null && SCB2 == null && FCB1.equals(FCB2)){
            return false;
        }

        return ((FCB1_isSubsetEqual_FCB2 && SCB1_in_FCB2) || (FCB2_isSubsetEqual_FCB1 && SCB2_in_FCB1));
    }

    /*
    Search the given actual control flow graph to find the closest basic block to the given target block.
    */
    public BasicBlock findTheClosestBlock(Set<BasicBlock> semiCoveredBasicBlocks, ActualControlFlowGraph targetMethodCFG, BasicBlock targetBlock){

        if (targetBlock == null){
            return null;
        }

        if(semiCoveredBasicBlocks.size() == 0){
            return null;
        }
        // There is only one semi covered block. So, no need to find the closest one.
        if(semiCoveredBasicBlocks.size() == 1){
            BasicBlock result = semiCoveredBasicBlocks.iterator().next();

            if (result.getFirstLine() == result.getLastLine()){
                throw new IllegalStateException("A semi-covered basic block cannot have only one line");
            }

            return result;
        }

        // find the semi covered block with the minimum distance to the target block.
        BasicBlock result=null;
        int minimumDistance = Integer.MAX_VALUE;

        for (BasicBlock currentBlock: semiCoveredBasicBlocks){
            if (currentBlock.getFirstLine() == currentBlock.getLastLine()){
                throw new IllegalStateException("A semi-covered basic block cannot have only one line");
            }

            int distance = getDistance(currentBlock,targetMethodCFG,targetBlock);

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
        Returns the actual control flow graph of the requested method
     */
    private  ActualControlFlowGraph getTargetMethodCFG(String targetClass, String targetMethod){
        GraphPool graphPool = GraphPool.getInstance(TestGenerationContextUtility.getTestGenerationContextClassLoader(false));
        return graphPool.getActualCFG(targetClass,targetMethod);
    }

    /*
        Returns the lines that their coverage is important for us in the current comparison
     */
    private  Set<Integer> detectInterestingCoveredLines(TestChromosome chromosome, BasicBlock targetBlock, int targetLine) {
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
    get the block coverage size of a test cases.
    */
    public int getCoverageSize(Set<BasicBlock> FCB) {
        // number of fully-covered blocks. We did not consider SCB here as each chromosome has only one closest semi-covered block.
        return FCB.size();
    }

    /*
        Checks if the given chromosome has covered all of the given basic block
     */
    private  boolean isFullyCovered(BasicBlock currentBasicBlock, Set<Integer> coveredLines, int targetLine) {
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
    private  boolean isTouched(BasicBlock currentBasicBlock, Set<Integer> coveredLines) {
        if(currentBasicBlock.isEntryBlock()){
            return true;
        }
        int firstLineNumber = currentBasicBlock.getFirstLine();

        return coveredLines.contains(firstLineNumber);
    }


    public Set<Integer> getCoveredLines(TestChromosome chromosome, ActualControlFlowGraph targetMethodCFG){
        Set<Integer> coveredLines;
        try {
            coveredLines = chromosome.getLastExecutionResult().getTrace().getCoveredLines(targetMethodCFG.getClassName());
        }catch (NullPointerException e){
            return new HashSet<>();
        }
        return coveredLines;
    }


    /*
    Returns the closest control dependent node to the target node, which is covered.
    */
    public BasicBlock getClosestCoveredControlDependency( BasicBlock targetBlock, Set<Integer> coveredLines) {

        BasicBlock result = null;
        String targetClass = targetBlock.getClassName();
        String targetMethod = targetBlock.getMethodName();

        Set<ControlDependency> CDBs = GraphPool.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT()).getCDG(targetClass, targetMethod).getControlDependentBranches(targetBlock);

        List<Integer> visitedCDLines = new ArrayList<>();
        List<ControlDependency> controlDependenciesToVisit = new LinkedList<>();
        controlDependenciesToVisit.addAll(CDBs);


        while(controlDependenciesToVisit.size() > 0){
            ControlDependency cb = controlDependenciesToVisit.remove(0);
            visitedCDLines.add(cb.getBranch().getInstruction().getLineNumber());

            if (coveredLines.contains(cb.getBranch().getInstruction().getLineNumber())){
                result = cb.getBranch().getInstruction().getBasicBlock();
                break;
            }
            Set<ControlDependency> temp = GraphPool.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT()).getCDG(targetClass, targetMethod).getControlDependentBranches(cb.getBranch().getInstruction().getBasicBlock());

            for(ControlDependency tempCB : temp){
                if(!visitedCDLines.contains(tempCB.getBranch().getInstruction().getLineNumber())){
                    controlDependenciesToVisit.add(tempCB);
                }
            }

        }
        return result;
    }

    /*
    Returns the distance (i.e., Dijkstra Shortest Path) from the givenBlock (src) to targetBlock (dest).
    Returns Integer.MAX_VALUE if there is no path from src to dest.
    */
    protected int getDistance(BasicBlock givenBlock, ActualControlFlowGraph targetMethodCFG, BasicBlock targetBlock){
        // To avoid extra calculations, we use a distancePool which stores all of the distances that are already calculated
        Integer distance = DistancePool.getInstance().getDistance(givenBlock,targetBlock);
        // if distance pool is null, the distance between the given block and the target block is not calculated.
        if(distance == null){
            // Hence, we measure it using Dijkstra Shortest Path
            distance = targetMethodCFG.getDistance(givenBlock,targetBlock);
            // if distance is -1, it means that there is no path from the given block to the target block.
            if (distance == -1){
                distance = Integer.MAX_VALUE;
            }
            // store the measured distance for the next iterations.
            DistancePool.getInstance().addDistance(givenBlock,targetBlock,distance);
        }

        return distance;
    }
    /*
     Returns all of the basic blocks in the target method, which are covered (either fully or semi) by the given chromosome.
  */
    public  List<Set<BasicBlock>> collectCoveredBasicBlocks(ActualControlFlowGraph targetMethodCFG, int targetLine, BasicBlock targetBlock, Set<Integer> coveredLines, BasicBlock closestCoveredControlDependency) {
        Set<BasicBlock> fullyCoveredBlocks= new HashSet<>();
        Set<BasicBlock> semiCoveredBlocks= new HashSet<>();

        List<Set<BasicBlock>> coveredBasicBlocks = new ArrayList<>();

        if (targetMethodCFG == null){
            return coveredBasicBlocks;
        }

        // check the basic blocks in the targetMethodCFG iteratively
        List<BasicBlock> visitedBasicBlocks = new ArrayList<>();
        List<BasicBlock> BasicBlocksToVisit = new LinkedList<>();

        // Start with closest covered control dependent node. If none, start with target CFG's entry point.
        if(closestCoveredControlDependency == null){
            BasicBlock entryBasicBlock = targetMethodCFG.getEntryPoint().getBasicBlock();
            BasicBlocksToVisit.add(entryBasicBlock);
        }else{
            BasicBlocksToVisit.add(closestCoveredControlDependency);
        }

        while (BasicBlocksToVisit.size() > 0){
            // Get a basic block
            BasicBlock currentBasicBlock = BasicBlocksToVisit.remove(0);
            visitedBasicBlocks.add(currentBasicBlock);
            // check if it is touched (i.e., the first line of the block is covered)
            if(isTouched(currentBasicBlock,coveredLines)){
                // if it is touched, we check if it is fully covered.
                if(isFullyCovered(currentBasicBlock,coveredLines,targetLine)){
                    // add to fully covered blocks
                    fullyCoveredBlocks.add(currentBasicBlock);

                    // Second we add its children to our visiting list.
                    for (BasicBlock child: targetMethodCFG.getChildren(currentBasicBlock)){
                        // If the child is added to review if both of the following conditions are fulfilled:
                        // 1- The child is not already reviewed
                        // 2- The child is either target block or the target block is reachable from it.
                        if(!visitedBasicBlocks.contains(child) &&
                                (getDistance(child,  targetMethodCFG,targetBlock) < Integer.MAX_VALUE || child.equals(targetBlock))
                        ){
                            BasicBlocksToVisit.add(child);
                        }
                    }
                }else{
                    // Add semi covered blocks
                    semiCoveredBlocks.add(currentBasicBlock);
                }

            }
        }
        // Add final results to the final output collection
        coveredBasicBlocks.add(0,fullyCoveredBlocks);
        coveredBasicBlocks.add(1,semiCoveredBlocks);
        return coveredBasicBlocks;
    }
    /*
      Check the line coverage in the semi covered blocks. More line coverage is better
       */
    public int compareCoveredLines(TestChromosome chromosome1, TestChromosome chromosome2, BasicBlock semiCoveredBasicBlock,int targetLine) {


        // find the covered lines in the target method by the given chromosome
        Collection<Integer> coveredLines1  = detectInterestingCoveredLines(chromosome1,semiCoveredBasicBlock,targetLine);
        Collection<Integer> coveredLines2  = detectInterestingCoveredLines(chromosome2,semiCoveredBasicBlock,targetLine);

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
    public  StackTraceElement findFirstUncoveredFrame(StackTrace crash, TestChromosome chromosome1, TestChromosome chromosome2) {
        int UncoveredFrameLevel = Integer.min(findFirstUncoveredFrame(chromosome1),findFirstUncoveredFrame(chromosome2));
        StackTraceElement UncoveredFrame = crash.getFrame(UncoveredFrameLevel);

        return UncoveredFrame;
    }

    /*
            Returns the first frame which is not covered by the given chromosome
    */
    private  int findFirstUncoveredFrame(TestChromosome chromosome) {
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
    private  boolean isFrameCovered(CrashCoverageFitnessCalculator fitnessCalculator, int frameLevel, TestChromosome chromosome) {
        if(fitnessCalculator.getLineCoverageForFrame(chromosome.getLastExecutionResult(),frameLevel) == 0){
            return true;
        }

        return false;
    }

}
