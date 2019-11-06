package eu.stamp.cling.fitnessfunction;

import eu.stamp.cling.integrationtesting.IntegrationTestingGoalFactory;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.testcase.TestFitnessFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class FitnessFunctions extends eu.stamp.botsing.commons.fitnessfunction.FitnessFunctions {
    private static final Logger LOG = LoggerFactory.getLogger(FitnessFunctions.class);

    public List<TestFitnessFunction> getFitnessFunctionList(){
        List<TestFitnessFunction> goalsList = new ArrayList<>();
        // Collecting goals which are related to the integration points coverage
        IntegrationTestingGoalFactory integrationTestingGoalFactory = new IntegrationTestingGoalFactory();
        Set<TestFitnessFunction> goalsSet = new HashSet<>(integrationTestingGoalFactory.getCoverageGoals());
        goalsList.addAll(goalsSet);
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
