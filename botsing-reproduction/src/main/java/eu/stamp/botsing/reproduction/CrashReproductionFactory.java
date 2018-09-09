package eu.stamp.botsing.reproduction;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.fitnessfunction.WeightedSum;
import org.evosuite.testsuite.AbstractFitnessFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CrashReproductionFactory extends AbstractFitnessFactory<WeightedSum> {

    private static Map<String, WeightedSum> goals = new LinkedHashMap<>();

    public CrashReproductionFactory(){
        Throwable targetException = CrashProperties.getTargetException();
        WeightedSum goal = new WeightedSum(targetException);
        String key = goal.getKey();
        if (!goals.containsKey(key)) {
            goals.put(key, goal);
        }
    }

    @Override
    public List<WeightedSum> getCoverageGoals() {
        return  new ArrayList<WeightedSum>(goals.values());
    }
}
