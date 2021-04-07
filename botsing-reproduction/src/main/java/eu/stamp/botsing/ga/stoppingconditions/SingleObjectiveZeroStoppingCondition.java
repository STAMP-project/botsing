package eu.stamp.botsing.ga.stoppingconditions;


import eu.stamp.botsing.commons.ga.strategy.mosa.MOSA;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.ga.stoppingconditions.StoppingConditionImpl;
import org.evosuite.testcase.TestFitnessFunction;

import java.util.List;


public class SingleObjectiveZeroStoppingCondition extends StoppingConditionImpl {


    private TestFitnessFunction mainFF;
    private double lastFitness = Double.MAX_VALUE;

    public SingleObjectiveZeroStoppingCondition(TestFitnessFunction mainFF){
        super();
        this.mainFF = mainFF;
    }

    @Override
    public void forceCurrentValue(long l) {

    }

    @Override
    public void iteration(GeneticAlgorithm<?> algorithm) {
        if (algorithm instanceof MOSA){
            for (Chromosome individual: (List<Chromosome>)((MOSA)algorithm).getOffspringPopulation()){
                lastFitness = Math.min(lastFitness, individual.getFitness(mainFF));
            }
        }

        for(Chromosome individual:  algorithm.getPopulation()){
            lastFitness = Math.min(lastFitness, individual.getFitness(mainFF));
        }
    }


    @Override
    public long getCurrentValue() {
        return (long) this.lastFitness;
    }

    public double getCurrentDoubleValue() {
        return  this.lastFitness;
    }

    @Override
    public long getLimit() {
        return 0;
    }

    @Override
    public boolean isFinished() {
        return lastFitness <= 0.0;
    }

    @Override
    public void reset() {
        this.lastFitness = Double.MAX_VALUE;
    }

    @Override
    public void setLimit(long l) {
        // do nothing
    }

    public String getNameOfObjective(){
        return this.mainFF.getClass().getSimpleName();
    }
}
