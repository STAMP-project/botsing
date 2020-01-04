package eu.stamp.botsing.ga;

import eu.stamp.botsing.ga.stoppingconditions.SingleObjectiveZeroStoppingCondition;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.stoppingconditions.StoppingCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

public class GAUtil {

    private static final Logger LOG = LoggerFactory.getLogger(GAUtil.class);

    public static boolean getSinglecObjectiveZeroSC(Set<StoppingCondition> stoppingConditions){
        for (StoppingCondition condition: stoppingConditions){
            if (condition instanceof SingleObjectiveZeroStoppingCondition){
                return true;
            }
        }

        return false;
    }


    public static void reportBestFF(Set<StoppingCondition> stoppingConditions) {
        for (StoppingCondition condition: stoppingConditions){
            if (condition instanceof SingleObjectiveZeroStoppingCondition){
                SingleObjectiveZeroStoppingCondition selectedCondition = (SingleObjectiveZeroStoppingCondition) condition;
                LOG.info("The best FF for {} is {}",selectedCondition.getNameOfObjective(),selectedCondition.getCurrentDoubleValue());
                break;
            }
        }
    }

    public static void reportNonDominatedFF(List<Chromosome> paretoFront) {
        int counter = 1;
        for (Chromosome individual: paretoFront){
            LOG.info("Individual #{}:",counter);
            for (FitnessFunction<?> fitnessFunction: individual.getFitnessValues().keySet()){
                double fitnessValue = individual.getFitnessValues().get(fitnessFunction);
                LOG.info("{}: {}",fitnessFunction.getClass().getName(), fitnessValue);
            }
            counter++;
        }
    }

}


