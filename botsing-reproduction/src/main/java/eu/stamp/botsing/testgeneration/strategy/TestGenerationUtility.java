package eu.stamp.botsing.testgeneration.strategy;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.commons.testgeneration.strategy.AbstractTestGenerationUtility;
import eu.stamp.botsing.fitnessfunction.FitnessFunctions;
import eu.stamp.botsing.fitnessfunction.testcase.factories.StackTraceChromosomeFactory;
import eu.stamp.botsing.ga.strategy.metaheuristics.GuidedSingleObjectiveGA;
import eu.stamp.botsing.ga.strategy.mosa.DynaMOSA;
import eu.stamp.botsing.commons.ga.strategy.mosa.MOSA;
import eu.stamp.botsing.ga.strategy.operators.GuidedMutation;
import eu.stamp.botsing.ga.strategy.operators.GuidedSearchUtility;
import eu.stamp.botsing.ga.strategy.operators.GuidedSinglePointCrossover;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.testcase.TestChromosome;


public class TestGenerationUtility extends AbstractTestGenerationUtility {


    public GeneticAlgorithm getGA(){
        switch (CrashProperties.searchAlgorithm){
            case Single_Objective_GGA:
                if(CrashProperties.getInstance().getCrashesSize() > 1){
                    throw new IllegalArgumentException("The number of crashes should be one in single objective GGA");
                }
                return new GuidedSingleObjectiveGA(getChromosomeFactory());
            case Guided_MOSA:
                // Create chromosome factory
                ChromosomeFactory<TestChromosome> chromosomeFactory = getChromosomeFactory();
                // Create guided crossover operator
                GuidedSinglePointCrossover guidedSinglePointCrossover = new GuidedSinglePointCrossover();
                guidedSinglePointCrossover.updatePublicCalls(((StackTraceChromosomeFactory) chromosomeFactory).getPublicCalls());
                // Create guided mutation operator
                GuidedMutation guidedMutation = new GuidedMutation();
                guidedMutation.updatePublicCalls(((StackTraceChromosomeFactory) chromosomeFactory).getPublicCalls());
                return new MOSA(chromosomeFactory,guidedSinglePointCrossover,guidedMutation,new FitnessFunctions());
            case DynaMOSA:
                return new DynaMOSA(getChromosomeFactory(),new GuidedSinglePointCrossover(),new GuidedMutation());
            default:
                return new GuidedSingleObjectiveGA(getChromosomeFactory());
        }
    }


    protected ChromosomeFactory<TestChromosome> getChromosomeFactory() {
        return new StackTraceChromosomeFactory(CrashProperties.getInstance().getStackTrace(0), new GuidedSearchUtility());
    }





}
