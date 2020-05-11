package eu.stamp.botsing.fitnessfunction.multiobjectivization;

import eu.stamp.botsing.StackTrace;
import eu.stamp.botsing.fitnessfunction.WeightedSum;
import eu.stamp.botsing.fitnessfunction.calculator.CrashCoverageFitnessCalculator;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;

import javax.annotation.Resource;

public class AbstractMultiObjectivizationFF extends TestFitnessFunction {

    @Resource
    protected CrashCoverageFitnessCalculator fitnessCalculator;
    protected StackTrace targetCrash;

    protected int objectiveId = 0;


    public AbstractMultiObjectivizationFF(StackTrace crash){
        fitnessCalculator = new CrashCoverageFitnessCalculator(crash);
        targetCrash = crash;
    }

    @Override
    public double getFitness(TestChromosome testChromosome, ExecutionResult executionResult) {
        return 0;
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
        final int prime = 31;
        int result = 1;
        result = prime * result + (targetCrash.hashCode())+objectiveId;
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
