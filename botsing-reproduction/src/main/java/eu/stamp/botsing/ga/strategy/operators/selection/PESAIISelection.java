package eu.stamp.botsing.ga.strategy.operators.selection;

import eu.stamp.botsing.ga.strategy.archive.GridArchive;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.operators.selection.SelectionFunction;
import org.evosuite.utils.Randomness;

import java.util.List;

public class PESAIISelection <T extends Chromosome<T>> extends SelectionFunction<T> {

    public T select(GridArchive<T> archive) {
        int selectedHyperCube = selectRandomHyperCube(archive);

        int base = Randomness.nextInt(0,archive.getSolutions().size());
        int counter=0;

        while (counter < archive.getSolutions().size()){
            T individual = (T) archive.getSolutions().get((base + counter)% archive.getSolutions().size());

            if (archive.getGrid().getHyperCube(individual) != selectedHyperCube){
                counter++;
            } else {
                return (T) individual;
            }
        }

        return  (T) archive.getSolutions().get((base + counter) % archive.getSolutions().size());
    }

    private int selectRandomHyperCube(GridArchive<T> archive){
        // Get two random hyperCubes
        int hyperCube1 = archive.getGrid().getRandomHyperCube(false);
        int hyperCube2 = archive.getGrid().getRandomHyperCube(false);

        // There is no difference if they are the same
        if (hyperCube1 == hyperCube2){
            return hyperCube1;
        }


        int density1 = archive.getGrid().getDensity(hyperCube1);
        int density2 = archive.getGrid().getDensity(hyperCube2);
        // we return the hypercube with the lowest density
        if(density1 < density2){
            return hyperCube1;
        }

        if (density1 > density2){
            return hyperCube2;
        }
        // if their densities are the same, we will choose one of them randomly.
        double randomDouble = Randomness.nextDouble();
        if (randomDouble < 0.5) {
            return hyperCube2;
        }

        return hyperCube1;
    }

    @Override
    public int getIndex(List<T> list) {
        return 0;
    }


}
