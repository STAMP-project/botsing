package eu.stamp.botsing.secondaryobjectives;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.StackTrace;
import eu.stamp.botsing.commons.testgeneration.TestGenerationContextUtility;
import eu.stamp.botsing.secondaryobjectives.basicblock.BasicBlockUtility;
import eu.stamp.botsing.secondaryobjectives.basicblock.CoveredBasicBlock;
import org.evosuite.ga.SecondaryObjective;
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
        int targetLine = getTargetLine(chromosome1,chromosome2);

        Collection<CoveredBasicBlock> coveredBlocks1 = BasicBlockUtility.collectCoveredBasicBlocks(chromosome1,targetClass,targetMethod,targetLine);
        Collection<CoveredBasicBlock> coveredBlocks2 = BasicBlockUtility.collectCoveredBasicBlocks(chromosome2,targetClass,targetMethod,targetLine);

        if(BasicBlockUtility.sameBasicBlockCoverage(coveredBlocks1,coveredBlocks2)){
            return BasicBlockUtility.compareCoveredLines(chromosome1,chromosome2,BasicBlockUtility.getSemiCoveredBasicBlocks(coveredBlocks1),targetLine);
        }else if(BasicBlockUtility.isSubset(coveredBlocks2,coveredBlocks1) || BasicBlockUtility.isSubset(coveredBlocks1,coveredBlocks2)){
            // chromosome 2 coverage is a subset of chromosome 1 coverage
            // or
            // chromosome 1 coverage is a subset of chromosome 2 coverage
            // the returned value is >0 if chromosome1 is a subset of chromosome2 and vice versa.
            return BasicBlockUtility.getCoverageSize(coveredBlocks2) - BasicBlockUtility.getCoverageSize(coveredBlocks1);
        }else {
            return 0;
        }
    }

    @Override
    public int compareGenerations(TestChromosome parent1, TestChromosome parent2,
                                  TestChromosome child1, TestChromosome child2) {
        return 0;
    }



    private String getTargetMethod(TestChromosome chromosome1, TestChromosome chromosome2) {
        if(CrashProperties.integrationTesting){
            // ToDo: Complete this
            return null;
        }else{
            // target method is fixed
            StackTrace crash  = CrashProperties.getInstance().getStackTrace(0);
            return TestGenerationContextUtility.derivingMethodFromBytecode(CrashProperties.integrationTesting,crash.getTargetClass(), crash.getTargetLine());
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
            // target line is fixed
            return CrashProperties.getInstance().getStackTrace(0).getTargetLine();
        }

    }
}
