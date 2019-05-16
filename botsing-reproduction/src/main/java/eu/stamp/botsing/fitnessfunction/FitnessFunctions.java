package eu.stamp.botsing.fitnessfunction;

import eu.stamp.botsing.reproduction.CrashReproductionGoalFactory;
import org.evosuite.testcase.TestFitnessFunction;

import java.util.List;

public class FitnessFunctions {


    public static List<TestFitnessFunction> getFitnessFunctionList(){
        CrashReproductionGoalFactory goalFactory = new CrashReproductionGoalFactory();
        return goalFactory.getCoverageGoals();
    }

}
