package eu.stamp.botsing.secondaryobjectives;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.StackTrace;
import eu.stamp.botsing.commons.testgeneration.TestGenerationContextUtility;
import eu.stamp.botsing.fitnessfunction.utils.CrashDistanceEvolution;
import eu.stamp.botsing.fitnessfunction.utils.WSEvolution;
import eu.stamp.botsing.secondaryobjectives.basicblock.BasicBlockUtility;
import eu.stamp.botsing.secondaryobjectives.basicblock.CoveredBasicBlock;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.SecondaryObjective;
import org.evosuite.testcase.TestChromosome;

import java.util.*;

public class BasicBlockCoverage extends SecondaryObjective<TestChromosome> {

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

        basicBlockUtility.collectCoveredBasicBlocks(chromosome1,targetClass,targetMethod,targetLine);
        basicBlockUtility.collectCoveredBasicBlocks(chromosome2,targetClass,targetMethod,targetLine);

        if(basicBlockUtility.sameBasicBlockCoverage(chromosome1,chromosome2)){
            finalValue=basicBlockUtility.compareCoveredLines(chromosome1,chromosome2,basicBlockUtility.getSemiCoveredBasicBlocks(chromosome1),targetLine);
        }else if(basicBlockUtility.isSubset(chromosome2,chromosome1) || basicBlockUtility.isSubset(chromosome1,chromosome2)){
            // chromosome 2 coverage is a subset of chromosome 1 coverage
            // or
            // chromosome 1 coverage is a subset of chromosome 2 coverage
            // the returned value is >0 if chromosome1 is a subset of chromosome2 and vice versa.
            finalValue = basicBlockUtility.getCoverageSize(chromosome2) - basicBlockUtility.getCoverageSize(chromosome1);
        }else {
            finalValue= 0;
        }
        basicBlockUtility.clear();
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
