package eu.stamp.botsing.fitnessfunction.utils;

import eu.stamp.botsing.ga.stoppingconditions.MaxTimeStoppingCondition;
import org.evosuite.ga.metaheuristics.SearchListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class CrashDistanceEvolution {
    private static final Logger LOG = LoggerFactory.getLogger(CrashDistanceEvolution.class);

    private double bestFitnessValue = Double.MAX_VALUE;

    private int fitnessFunctionEvaluations = 0;

    private long startTime;

    // Time

    private static CrashDistanceEvolution instance;

    private CrashDistanceEvolution(){}


    public static CrashDistanceEvolution getInstance(){
        if (instance == null){
            instance = new CrashDistanceEvolution();
        }

        return instance;
    }


    public void setBestFitnessValue(double bestFitnessValue) {
        LOG.info("New Value for Weighted Sum after {} fitness evolutions and {} second: {}", fitnessFunctionEvaluations, getPassedTime(), bestFitnessValue);
        this.bestFitnessValue = bestFitnessValue;
    }

    public void inform(double fitnessValue) {
        fitnessFunctionEvaluations++;
        if (fitnessValue < bestFitnessValue){
            setBestFitnessValue(fitnessValue);
        }
    }


    public void setStartTime(Set<SearchListener> listeners) {
        for (SearchListener listener : listeners){
            if (listener instanceof MaxTimeStoppingCondition){
                startTime =((MaxTimeStoppingCondition) listener).getStartingTime();
                return;
        }
        }
    }

    private long getPassedTime(){
        long currentTime = System.currentTimeMillis();
        return (currentTime - this.startTime) / 1000L;
    }
}
