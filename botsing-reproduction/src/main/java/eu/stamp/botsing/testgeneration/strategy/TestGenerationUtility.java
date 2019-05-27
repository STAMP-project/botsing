package eu.stamp.botsing.testgeneration.strategy;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.fitnessfunction.testcase.factories.StackTraceChromosomeFactory;
import eu.stamp.botsing.ga.strategy.GuidedGeneticAlgorithm;
import eu.stamp.botsing.ga.strategy.mosa.DynaMOSA;
import eu.stamp.botsing.ga.strategy.mosa.MOSA;
import eu.stamp.botsing.ga.strategy.operators.GuidedSearchUtility;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.testcase.TestChromosome;


public class TestGenerationUtility {


    public GeneticAlgorithm getGA(){
        switch (CrashProperties.searchAlgorithm){
            case Single_Objective_GGA:
                if(CrashProperties.getInstance().getCrashesSize() > 1){
                    throw new IllegalArgumentException("The number of crashes should be one in single objective GGA");
                }
                return new GuidedGeneticAlgorithm(getChromosomeFactory());
            case Guided_MOSA:
                return new MOSA(getChromosomeFactory());
            case DynaMOSA:
                return new DynaMOSA(getChromosomeFactory());
            default:
                return new GuidedGeneticAlgorithm(getChromosomeFactory());
        }
    }


    private ChromosomeFactory<TestChromosome> getChromosomeFactory() {
        return new StackTraceChromosomeFactory(CrashProperties.getInstance().getStackTrace(0), new GuidedSearchUtility());
    }





}
