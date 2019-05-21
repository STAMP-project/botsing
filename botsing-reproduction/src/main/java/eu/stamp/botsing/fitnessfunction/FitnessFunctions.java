package eu.stamp.botsing.fitnessfunction;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.coverage.io.input.InputCoverageFactory;
import eu.stamp.botsing.coverage.io.output.OutputCoverageFactory;
import eu.stamp.botsing.reproduction.CrashReproductionGoalFactory;
import org.evosuite.testcase.TestFitnessFunction;

import java.util.ArrayList;
import java.util.List;

public class FitnessFunctions {


    public static List<TestFitnessFunction> getFitnessFunctionList(){
        List<TestFitnessFunction> goalsList = new ArrayList<>();

        // crash coverage goals
        CrashReproductionGoalFactory goalFactory = new CrashReproductionGoalFactory();
        goalsList.addAll(goalFactory.getCoverageGoals());

        if(CrashProperties.IODiversity){
            // input coverage goals
            InputCoverageFactory inputFactory = new InputCoverageFactory();
            goalsList.addAll(inputFactory.getCoverageGoals());

            // output coverage goals
            OutputCoverageFactory outputFactory = new OutputCoverageFactory();
            goalsList.addAll(outputFactory.getCoverageGoals());
        }

        return goalsList;
    }

}
