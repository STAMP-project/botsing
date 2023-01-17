package eu.stamp.botsing.fitnessfunction;

import eu.stamp.botsing.StackTrace;
import eu.stamp.botsing.fitnessfunction.calculator.diversity.HammingDiversity;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;


public class CallDiversity extends TestFitnessFunction {

    StackTrace targetCrash ;


    public CallDiversity(StackTrace crash){
        targetCrash=crash;
    }


    @Override
    public double getFitness(TestChromosome individual) {
        double fitness = this.getFitness(individual, null);
        this.updateIndividual(individual, fitness);
        return fitness;
    }

    @Override
    public double getFitness(TestChromosome testChromosome, ExecutionResult executionResult) {
        return HammingDiversity.getInstance(targetCrash).getSimilarityValue(testChromosome);
    }

    @Override
    public int compareTo(TestFitnessFunction other) {
        if (other == null){
            return 1;
        }

        if (other instanceof WeightedSum){
            return 0;
        }

        return compareClassName(other);
    }

    @Override
    public int hashCode() {
        final int prime = 41;
        int result = 1;
        result = prime * result + ( targetCrash.getTargetClass().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        return getClass() == obj.getClass();
    }

    @Override
    public String getTargetClass() {
        return targetCrash.getTargetClass();
    }

    @Override
    public String getTargetMethod() {
        return targetCrash.getTargetMethod();
    }
}
