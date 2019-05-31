package eu.stamp.botsing.integration.integrationtesting;

import eu.stamp.botsing.integration.fitnessfunction.FitnessFunctions;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testsuite.AbstractFitnessFactory;

import java.util.LinkedList;
import java.util.List;

public class IntegrationTestingGoalFactory extends AbstractFitnessFactory<TestFitnessFunction> {
    private static List<TestFitnessFunction> goals = new LinkedList<>();

    public IntegrationTestingGoalFactory(){
        // This is for branch coverage
        FitnessFunctions ff = new FitnessFunctions();
        goals.addAll(ff.getFitnessFunctionList());
        // ToDo: Make it a proper factory after defining new FFs
    }
    @Override
    public List<TestFitnessFunction> getCoverageGoals() {
        return goals;
    }
}
