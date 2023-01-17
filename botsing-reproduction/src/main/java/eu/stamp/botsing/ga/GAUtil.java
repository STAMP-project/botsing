package eu.stamp.botsing.ga;


import eu.stamp.botsing.fitnessfunction.utils.WSEvolution;
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

    public static <T extends Chromosome<T>> boolean getSinglecObjectiveZeroSC(Set<StoppingCondition<T>> stoppingConditions){
        for (StoppingCondition condition: stoppingConditions){
            if (condition instanceof SingleObjectiveZeroStoppingCondition){
                return true;
            }
        }

        return false;
    }


    public static <T extends Chromosome<T>> void reportBestFF(Set<StoppingCondition<T>> stoppingConditions) {
        for (StoppingCondition condition: stoppingConditions){
            if (condition instanceof SingleObjectiveZeroStoppingCondition){
                SingleObjectiveZeroStoppingCondition selectedCondition = (SingleObjectiveZeroStoppingCondition) condition;
                LOG.info("The best FF for {} is {}",selectedCondition.getNameOfObjective(),selectedCondition.getCurrentDoubleValue());
                break;
            }
        }
    }

    public static <T extends Chromosome<T>> void reportNonDominatedFF(List<Chromosome<T>> paretoFront,int numberOfIterations) {
        int counter = 1;
        for (Chromosome<T> individual: paretoFront){
            LOG.info("Individual #{}:",counter);
            for (FitnessFunction<?> fitnessFunction: individual.getFitnessValues().keySet()){
                double fitnessValue = individual.getFitnessValues().get(fitnessFunction);
                LOG.info("{}: {}",fitnessFunction.getClass().getName(), fitnessValue);
            }

        }
    }


    public static <T extends Chromosome<T>> void informWSEvolution(Chromosome<T> individual){
        double LineCoverageFitness = 1;
        double exceptionCoverage = 1;
        double frameSimilarity = 1;
        for (FitnessFunction<?> fitnessFunction: individual.getFitnessValues().keySet()){
            double fitnessValue = individual.getFitnessValues().get(fitnessFunction);
            String objectiveClassname = fitnessFunction.getClass().getName();
            if(objectiveClassname.endsWith("LineCoverageFF")){
                LineCoverageFitness = fitnessValue;
            }else if (objectiveClassname.endsWith("ExceptionTypeFF")){
                exceptionCoverage = fitnessValue;
            }else if(objectiveClassname.endsWith("StackTraceSimilarityFF")){
                frameSimilarity = fitnessValue;
            }else {
                LOG.warn("4th objective is found in the multi-objectivization search");
            }
        }
        double finalFitnessValue;
        if(LineCoverageFitness > 0){
            finalFitnessValue = 3 * LineCoverageFitness + 3;
        }else if (exceptionCoverage > 0){
            finalFitnessValue = 2 * exceptionCoverage + 1;
        }else {
            finalFitnessValue = frameSimilarity;
        }
        WSEvolution.getInstance().inform(finalFitnessValue);
    }

}


