package eu.stamp.botsing.ga.strategy.mosa;

import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ChromosomeFactory;

public class MOSA<T extends Chromosome> extends AbstractMOSA<T> {
    public MOSA(ChromosomeFactory factory) {
        super(factory);
    }
}
