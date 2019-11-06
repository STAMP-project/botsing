package eu.stamp.botsing.commons.testgeneration.strategy;

import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.testcase.TestChromosome;


public abstract class AbstractTestGenerationUtility {


    public abstract GeneticAlgorithm getGA();


    protected abstract ChromosomeFactory<TestChromosome> getChromosomeFactory();


}
