package eu.stamp.botsing.fitnessfunction.multiobjectivization;

import eu.stamp.botsing.StackTrace;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.execution.ExecutionResult;

public class LineCoverageFF extends AbstractMultiObjectivizationFF {
    public LineCoverageFF(StackTrace crash){
        super(crash);
        this.objectiveId=1;
    }

    /**
     * Calculates distance from the target line (line number of the target frame)
     * @param testChromosome generated test chromosome.
     * @param executionResult execution result of the given testChromosome.
     * @return a double value between 1.0 and 0.0 according to the approach level and branch distance.
     * If the returned value is 0, it means that testChromosome covers the target line.
     */
    @Override
    public double getFitness(TestChromosome testChromosome, ExecutionResult executionResult) {
        double fitness = fitnessCalculator.getLineCoverageFitness(executionResult, targetCrash.getTargetLine());
        return fitness;
    }
}
