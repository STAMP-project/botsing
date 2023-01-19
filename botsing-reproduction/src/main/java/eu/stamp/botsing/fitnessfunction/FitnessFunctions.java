package eu.stamp.botsing.fitnessfunction;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.coverage.io.IOCoverageUtility;
import eu.stamp.botsing.coverage.io.input.InputCoverageFactory;
import eu.stamp.botsing.coverage.io.output.OutputCoverageFactory;
import eu.stamp.botsing.reproduction.CrashReproductionGoalFactory;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.testcase.TestChromosome;
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

        // crash coverage goals
        CrashReproductionGoalFactory goalFactory = new CrashReproductionGoalFactory();
        goalsList.addAll(goalFactory.getCoverageGoals());

        if(CrashProperties.IODiversity){
            IOCoverageUtility coverageUtility = new IOCoverageUtility();
            // input coverage goals
            InputCoverageFactory inputFactory = new InputCoverageFactory(coverageUtility);
            goalsList.addAll(inputFactory.getCoverageGoals());

            // output coverage goals
            OutputCoverageFactory outputFactory = new OutputCoverageFactory(coverageUtility);
            goalsList.addAll(outputFactory.getCoverageGoals());
        }

        return goalsList;
    }

    @Override
    public <T extends Chromosome> boolean isCriticalGoalsAreCovered(Set<TestFitnessFunction> uncoveredGoals) {
        for (TestFitnessFunction goal: uncoveredGoals){
            if(goal instanceof IntegrationTestingFF || goal instanceof WeightedSum){
                return false;
            }
        }
        return true;
    }

    @Override
    public void printCriticalTargets(Map<FitnessFunction<TestChromosome>, Double> front0) {
        for(FitnessFunction<TestChromosome> g: front0.keySet()){
            if(g instanceof IntegrationTestingFF ){
                LOG.info(""+g+": "+front0.get(g));
            }
        }
    }

}
