package eu.stamp.botsing.fitnessfunction.multiobjectivization;


import eu.stamp.botsing.StackTrace;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.execution.ExecutionResult;

public class StackTraceSimilarityFF extends AbstractMultiObjectivizationFF {

    public StackTraceSimilarityFF(StackTrace crash) {
        super(crash);
        this.objectiveId=3;
    }

    @Override
    public double getFitness(TestChromosome testChromosome, ExecutionResult executionResult) {
        double frameSimilarity = 1.0;
        for (Integer ExceptionLocator : executionResult.getPositionsWhereExceptionsWereThrown()) {
            double tempFitness = fitnessCalculator.calculateFrameSimilarity( executionResult.getExceptionThrownAtPosition(ExceptionLocator).getStackTrace());
            if (tempFitness == 0.0){
                frameSimilarity = 0.0;
                break;
            }else if (tempFitness<frameSimilarity){
                frameSimilarity = tempFitness;
            }
        }

        return frameSimilarity;
    }
}
