package eu.stamp.botsing.secondaryobjectives;

import eu.stamp.botsing.CrashProperties;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.secondaryobjectives.MinimizeLengthSecondaryObjective;

public class TestCaseSecondaryObjective {


    public static void setSecondaryObjectives() {
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
