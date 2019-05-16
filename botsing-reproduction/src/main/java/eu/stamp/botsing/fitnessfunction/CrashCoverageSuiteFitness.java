package eu.stamp.botsing.fitnessfunction;

import eu.stamp.botsing.testgeneration.strategy.TestGenerationUtility;
import org.evosuite.testcase.ExecutableChromosome;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testsuite.AbstractTestSuiteChromosome;
import org.evosuite.testsuite.TestSuiteFitnessFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CrashCoverageSuiteFitness extends TestSuiteFitnessFunction {
    public static double totalFitnessValue;
    private static final Logger LOG = LoggerFactory.getLogger(CrashCoverageSuiteFitness.class);

    private static TestGenerationUtility utility = new TestGenerationUtility();

    @Override
    public double getFitness(AbstractTestSuiteChromosome<? extends ExecutableChromosome> abstractTestSuiteChromosome) {
        List<ExecutionResult> results = runTestSuite(abstractTestSuiteChromosome);
        double goalcoverage;

        if (results == null) {
            throw new IllegalArgumentException();
        } else {
            goalcoverage = calculateFitness(abstractTestSuiteChromosome);
            LOG.info("Total fitness value: " + totalFitnessValue );
            LOG.info("Total goal coverage ratio: " + goalcoverage);
            abstractTestSuiteChromosome.setFitness(this, totalFitnessValue);
            abstractTestSuiteChromosome.setCoverage(this, goalcoverage);
        }

        updateIndividual(this, abstractTestSuiteChromosome, totalFitnessValue);

        return totalFitnessValue;
    }


    private static int calculateFitness (AbstractTestSuiteChromosome<? extends ExecutableChromosome>  suite){
        int coveredGoals = 0;
        double fitnessValue = 0.0;
        List<TestFitnessFunction> fitnessFunctions = FitnessFunctions.getFitnessFunctionList();
        int totalGoals = fitnessFunctions.size();
        for (TestFitnessFunction crashGoal: fitnessFunctions){
            double minFitnessValue = Double.MAX_VALUE;

            for (int i=0 ; i<suite.size(); i++) {
                minFitnessValue = Math.min(minFitnessValue , crashGoal.getFitness((TestChromosome) suite.getTestChromosome(i)));
            }

            LOG.info("Goal is: " + crashGoal.getClass().getName());
            LOG.info("Fitness for this goal is: " + minFitnessValue);

            if (minFitnessValue == 0){
                coveredGoals++;
            }
            fitnessValue += minFitnessValue;
        }

        totalFitnessValue = fitnessValue;
        return (coveredGoals/totalGoals);
    }
}