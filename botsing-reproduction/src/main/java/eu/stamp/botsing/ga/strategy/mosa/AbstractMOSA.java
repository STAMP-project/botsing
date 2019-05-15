package eu.stamp.botsing.ga.strategy.mosa;

import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ChromosomeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;

public class AbstractMOSA<T extends Chromosome> extends org.evosuite.ga.metaheuristics.mosa.AbstractMOSA<T> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractMOSA.class);



    public AbstractMOSA(ChromosomeFactory<T> factory) {
        super(new ArrayList<ChromosomeFactory<T>>(Arrays.asList(factory)));
    }


    @Override
    protected void evolve() {

    }

    @Override
    public void generateSolution() {

    }




}
