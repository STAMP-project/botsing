package eu.stamp.botsing.integration.fitnessfunction;

import eu.stamp.botsing.integration.integrationtesting.IntegrationTestingGoalFactory;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.testcase.TestFitnessFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FitnessFunctions extends eu.stamp.botsing.commons.fitnessfunction.FitnessFunctions {
    private static final Logger LOG = LoggerFactory.getLogger(FitnessFunctions.class);

    public List<TestFitnessFunction> getFitnessFunctionList(){
        List<TestFitnessFunction> goalsList = new ArrayList<>();
        // Collecting goals which are related to the integration points coverage
        IntegrationTestingGoalFactory integrationTestingGoalFactory = new IntegrationTestingGoalFactory();
        goalsList.addAll(integrationTestingGoalFactory.getCoverageGoals());
        return goalsList;
    }

    @Override
    public <T extends Chromosome> boolean isCriticalGoalsAreCovered(Set<FitnessFunction<T>> uncoveredGoals) {
        if(uncoveredGoals.size() == 0){
            return true;
        }
        return false;
    }

    @Override
    public void printCriticalTargets(Map<FitnessFunction<?>, Double> front0) {
        for(FitnessFunction<?> g: front0.keySet()){
                LOG.debug(""+g+": "+front0.get(g));
        }
    }

}
