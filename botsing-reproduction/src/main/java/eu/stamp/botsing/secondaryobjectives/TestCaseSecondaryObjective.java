package eu.stamp.botsing.secondaryobjectives;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.ga.strategy.GuidedGeneticAlgorithm;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.secondaryobjectives.MinimizeLengthSecondaryObjective;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestCaseSecondaryObjective {
    private static final Logger LOG = LoggerFactory.getLogger(TestCaseSecondaryObjective.class);

    public static void setSecondaryObjectives() {
        LOG.info("Secondary Objectives: {}",CrashProperties.secondaryObjectives);
        for (CrashProperties.SecondaryObjective secondaryObjective : CrashProperties.secondaryObjectives) {
            switch (secondaryObjective) {
                case BasicBlockCoverage:
                    TestChromosome.addSecondaryObjective(new BasicBlockCoverage());
                    break;
                case TestLength:
                    TestChromosome.addSecondaryObjective(new MinimizeLengthSecondaryObjective());
                    break;
                default:
                    break;
            }
        }
    }
}
