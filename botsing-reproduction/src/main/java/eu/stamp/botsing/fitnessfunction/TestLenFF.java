package eu.stamp.botsing.fitnessfunction;

import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;

public class TestLenFF extends TestFitnessFunction {
    @Override
    public double getFitness(TestChromosome testChromosome, ExecutionResult executionResult) {
        return testChromosome.getTestCase().size();
    }

    @Override
    public int compareTo(TestFitnessFunction other) {
        if (other == null){
            return 1;
        }

        if (other instanceof TestLenFF){
            return 0;
        }

        return compareClassName(other);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        return prime * result + (this.getClass().hashCode());
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
        return null;
    }

    @Override
    public String getTargetMethod() {
        return null;
    }
}
