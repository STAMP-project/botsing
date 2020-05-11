package eu.stamp.botsing.ga;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.fitnessfunction.FitnessFunctionHelper;
import eu.stamp.botsing.fitnessfunction.utils.WSEvolution;
import eu.stamp.botsing.ga.stoppingconditions.SingleObjectiveZeroStoppingCondition;
import org.evosuite.Properties;
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

    public static void reportNonDominatedFF(List<Chromosome> paretoFront,int numberOfIterations) {
        int counter = 1;
        for (Chromosome individual: paretoFront){
            LOG.info("Individual #{}:",counter);
            for (FitnessFunction<?> fitnessFunction: individual.getFitnessValues().keySet()){
                double fitnessValue = individual.getFitnessValues().get(fitnessFunction);
                LOG.info("{}: {}",fitnessFunction.getClass().getName(), fitnessValue);
            }

        }
    }


    public static void informWSEvolution(Chromosome individual){
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


