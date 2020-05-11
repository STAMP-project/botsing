package eu.stamp.botsing.fitnessfunction.multiobjectivization;

import eu.stamp.botsing.StackTrace;
import org.evosuite.coverage.exception.ExceptionCoverageHelper;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.execution.ExecutionResult;

public class ExceptionTypeFF extends AbstractMultiObjectivizationFF {

    public ExceptionTypeFF(StackTrace crash) {
        super(crash);
        this.objectiveId=2;
    }

    /**
     * Compares the types of the thrown exceptions with type of the given exception.
     * @param testChromosome generated test chromosome.
     * @param executionResult execution result of the given testChromosome.
     * @return 0.0 or 1.0 if the type of the exception "is" or "is not" the same, respectively.
     */
    @Override
    public double getFitness(TestChromosome testChromosome, ExecutionResult executionResult) {
        for (Integer ExceptionLocator : executionResult.getPositionsWhereExceptionsWereThrown()) {
            String thrownException = ExceptionCoverageHelper.getExceptionClass(executionResult, ExceptionLocator).getName();
            if (thrownException.equals(targetCrash.getExceptionType())){
                return 0.0;
            }
        }
        return 1.0;
    }
}
