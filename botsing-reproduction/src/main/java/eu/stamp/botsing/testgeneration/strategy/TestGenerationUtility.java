package eu.stamp.botsing.testgeneration.strategy;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.fitnessfunction.FitnessFunctions;
import eu.stamp.botsing.fitnessfunction.testcase.factories.StackTraceChromosomeFactory;
import eu.stamp.botsing.ga.strategy.GuidedGeneticAlgorithm;
import eu.stamp.botsing.ga.strategy.mosa.DynaMOSA;
import eu.stamp.botsing.commons.ga.strategy.mosa.MOSA;
import eu.stamp.botsing.ga.strategy.operators.GuidedMutation;
import eu.stamp.botsing.ga.strategy.operators.GuidedSearchUtility;
import eu.stamp.botsing.ga.strategy.operators.GuidedSinglePointCrossover;
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
                //ChromosomeFactory factory, CrossOverFunction crossOverOperator, Mutation mutationOperator, FitnessFunctions fitnessCollector
                return new MOSA(getChromosomeFactory(),new GuidedSinglePointCrossover(),new GuidedMutation(),new FitnessFunctions());
            case DynaMOSA:
                return new DynaMOSA(getChromosomeFactory(),new GuidedSinglePointCrossover(),new GuidedMutation());
            default:
                return new GuidedGeneticAlgorithm(getChromosomeFactory());
        }
    }


    private ChromosomeFactory<TestChromosome> getChromosomeFactory() {
        return new StackTraceChromosomeFactory(CrashProperties.getInstance().getStackTrace(0), new GuidedSearchUtility());
    }





}
