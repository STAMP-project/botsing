package eu.stamp.botsing.ga.strategy.mosa;

import eu.stamp.botsing.fitnessfunction.CrashCoverageSuiteFitness;
import eu.stamp.botsing.fitnessfunction.FitnessFunctions;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.FitnessFunction;


public class AbstractMOSA<T extends Chromosome> extends org.evosuite.ga.metaheuristics.mosa.AbstractMOSA<T> {



    public AbstractMOSA(ChromosomeFactory<T> factory) {
        super(factory);
    }

    @Override
    protected void setupSuiteFitness(){
        getSuiteFitnessFunctions();
        }


    protected void getSuiteFitnessFunctions(){
        for (FitnessFunction ff: FitnessFunctions.getFitnessFunctionList()){

            this.suiteFitnessFunctions.put(new CrashCoverageSuiteFitness(),ff.getClass());
        }
    }

    @Override
    protected void evolve() {

    }

    @Override
    public void generateSolution() {

    }




}
