package eu.stamp.botsing.fitnessfunction;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.coverage.io.input.InputCoverageFactory;
import eu.stamp.botsing.coverage.io.output.OutputCoverageFactory;
import eu.stamp.botsing.coverage.variable.BranchingVariableCoverageFactory;
import eu.stamp.botsing.reproduction.CrashReproductionGoalFactory;
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

    public List<TestFitnessFunction> getFitnessFunctionList() {
        // crash coverage goals
        CrashReproductionGoalFactory goalFactory = new CrashReproductionGoalFactory();
        List<TestFitnessFunction> goalsList = new ArrayList<>(goalFactory.getCoverageGoals());

        if (CrashProperties.IODiversity) {
            // input coverage goals
            InputCoverageFactory inputFactory = new InputCoverageFactory();
            goalsList.addAll(inputFactory.getCoverageGoals());

            // output coverage goals
            OutputCoverageFactory outputFactory = new OutputCoverageFactory();
            goalsList.addAll(outputFactory.getCoverageGoals());
        }

        // branching variable diversity goals
        if (CrashProperties.branchingVariableDiversity) {
            BranchingVariableCoverageFactory branchingVariableFactory = new BranchingVariableCoverageFactory();
            goalsList.addAll(branchingVariableFactory.getCoverageGoals());
        }

        return goalsList;
    }

    @Override
    public <T extends Chromosome> boolean isCriticalGoalsAreCovered(Set<FitnessFunction<T>> uncoveredGoals) {
        for (FitnessFunction<T> goal : uncoveredGoals) {
            if (goal instanceof IntegrationTestingFF || goal instanceof WeightedSum) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void printCriticalTargets(Map<FitnessFunction<?>, Double> front0) {
        for (FitnessFunction<?> g : front0.keySet()) {
            if (g instanceof IntegrationTestingFF) {
                LOG.info("" + g + ": " + front0.get(g));
            }
        }
    }
}
