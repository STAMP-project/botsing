package eu.stamp.botsing.commons.fitnessfunction;

import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.testsuite.TestSuiteFitnessFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CrashCoverageSuiteFitness extends TestSuiteFitnessFunction {
    public static double totalFitnessValue;
    private static final Logger LOG = LoggerFactory.getLogger(CrashCoverageSuiteFitness.class);
    FitnessFunctions fitnessCollector;
    public CrashCoverageSuiteFitness(FitnessFunctions fitnessCollector){
        this.fitnessCollector = fitnessCollector;
    }
    @Override
    public double getFitness(TestSuiteChromosome abstractTestSuiteChromosome) {
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

        updateIndividual(abstractTestSuiteChromosome, totalFitnessValue);

        return totalFitnessValue;
    }


    private int calculateFitness (TestSuiteChromosome  suite){
        int coveredGoals = 0;
        double fitnessValue = 0.0;
        List<TestFitnessFunction> fitnessFunctions = this.fitnessCollector.getFitnessFunctionList();
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

    @Override
    public List<ExecutionResult> runTestSuite(TestSuiteChromosome suite){
        return super.runTestSuite(suite);
    }

}