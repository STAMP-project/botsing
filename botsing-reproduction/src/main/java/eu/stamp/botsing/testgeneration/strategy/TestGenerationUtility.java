package eu.stamp.botsing.testgeneration.strategy;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.fitnessfunction.testcase.factories.RootMethodTestChromosomeFactory;
import eu.stamp.botsing.ga.strategy.GuidedGeneticAlgorithm;
import eu.stamp.botsing.ga.strategy.mosa.MOSA;
import eu.stamp.botsing.ga.strategy.operators.GuidedSearchUtility;
import eu.stamp.botsing.reproduction.CrashReproductionGoalFactory;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;

import java.util.List;


public class TestGenerationUtility {


    public GeneticAlgorithm getGA(){
        switch (CrashProperties.searchAlgorithm){
            case Single_Objective_GGA:
                return new GuidedGeneticAlgorithm(getChromosomeFactory());
            case Guided_MOSA:
                return new MOSA(getChromosomeFactory());
            default:
                return new GuidedGeneticAlgorithm(getChromosomeFactory());
        }
    }


    private ChromosomeFactory<TestChromosome> getChromosomeFactory() {
        return new RootMethodTestChromosomeFactory(CrashProperties.getInstance().getStackTrace(0), new GuidedSearchUtility());
    }


    public List<TestFitnessFunction> getFitnessFunctionList(){
        CrashReproductionGoalFactory goalFactory = new CrashReproductionGoalFactory();
        return goalFactory.getCoverageGoals();
    }



}
