package eu.stamp.botsing.ga.strategy.archive;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.ga.strategy.archive.util.Grid;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.FitnessFunction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GridArchive<T extends Chromosome> {

    private Grid<T> grid;
    private int archiveSizeLimit;

    private List<T> archive;

    public GridArchive(){
        grid = new Grid<>();
        archiveSizeLimit = CrashProperties.archiveSize;
        archive = new ArrayList<>();
    }


    public void updateArchive(List<T> population){
        for (T individual: population){
            // add to archive if it is empty
            if (archive.isEmpty()){
                archive.add(individual);
                continue;
            }

            Iterator<T> iterator = archive.iterator();
            boolean isDominated = false;
            while (iterator.hasNext() && (!isDominated)) {
                T archiveIndividual = iterator.next();

                // dominance comparison
                int dominanceComparisonResult = this.dominanceComparison(individual,archiveIndividual);

                if (dominanceComparisonResult == -1){
                    // Individual in the current popluation dominates archiveIndividual.
                    // So, we will remove archiveIndividual from archive.
                    iterator.remove();
                    int hyperCubeIndex = grid.getHyperCube(archiveIndividual);
                    if (grid.getDensity(hyperCubeIndex) > 1) {//The hypercube contains
                        grid.removeIndividual(archiveIndividual);            //more than one individual
                    } else {
                        grid.updateGrid(archive);
                    }

                } else if (dominanceComparisonResult == 1){
                    isDominated = true;
                    break;
                }
            }


            if (isDominated){
                continue;
            }

            // Here, we know that the current individual from population is non-dominated compared to individuals in the archive.
            // So, we should add it to archive.

            // If archive is empty, first, we add the individual to the population.
            if (archive.size() == 0){
                archive.add(individual);
                grid.updateGrid(archive);
                continue;
            }

            // If archive is not emtpy, first, we update the grids if the fitness values of  the given individual exceed the current boundaries of the grid
            if(!grid.inRange(individual)){
                grid.updateGrid(individual,archive);
            }
            // Then, we add the solution to archive
            // Here, we check if the archive is not full
            if (this.archive.size() < this.archiveSizeLimit) {
                // If it is not full, we simply add the individual to grid and archive
                grid.add(individual);
                archive.add(individual);
                continue;
            }

            // If it is not full, we will check if this individual is in the most populated hyperCube
            if(grid.inMostPopulatedHypercube(individual)){
                // If it is is in the most populated hypercube, we simply skip it
                continue;
            }

            // else,
            // we remove an individual from the most crowded hyper cube
            boolean successfulPrune = prune();
            if(successfulPrune){
                //, and insert our new solution
                grid.add(individual);
                archive.add(individual);
            }else {
                throw new IllegalStateException("prune was not successful");
            }
        }
    }

    private boolean prune() {
        Iterator<T> iterator = archive.iterator();
        while (iterator.hasNext()){
            T individual = iterator.next();
            if(grid.inMostPopulatedHypercube(individual)){
                iterator.remove();
                grid.removeIndividual(individual);
                return true;
            }
        }
        return false;
    }


    /**
     * Compares two individuals.
     * @param individual1 first given individual.
     * @param individual2 second given individual.
     * @return -1, or 0, or 1 if individual1 dominates individual2, both are non-dominated, or individual2
     *     dominates individual1, respectively.
     */
    private int dominanceComparison(T individual1, T individual2) {
        if (individual1 == null || individual2 == null){
            throw new IllegalArgumentException("the given individuals are empty!");
        }

        Map<FitnessFunction<?>, Double> individual1FitnessValues = individual1.getFitnessValues();
        Map<FitnessFunction<?>, Double> individual2FitnessValues = individual2.getFitnessValues();


        if (individual1FitnessValues.keySet().size() != individual2FitnessValues.keySet().size()){
            throw new IllegalArgumentException("number of objectives of individual1 is not same as individual2");
        }

        boolean individual1IsBetterInOneObjective = false;
        boolean individual2IsBetterInOneObjective = false;

        for (FitnessFunction<?> individual1FF : individual1FitnessValues.keySet()){
            if(!individual2FitnessValues.containsKey(individual1FF)){
                throw new IllegalArgumentException("Objective"+individual1FF.toString()+"is not available in individual2");
            }

            double individual1FitnessValue = individual1FitnessValues.get(individual1FF).doubleValue();
            double individual2FitnessValue = individual2FitnessValues.get(individual1FF).doubleValue();

            if (individual1FitnessValue == individual2FitnessValue){
                continue;
            }

            if (individual1FitnessValue < individual2FitnessValue) {
                individual1IsBetterInOneObjective = true;
            } else {
                individual2IsBetterInOneObjective = true;
            }
        }

        if ((individual1IsBetterInOneObjective && individual2IsBetterInOneObjective) || (!individual1IsBetterInOneObjective && !individual2IsBetterInOneObjective)){
            // non-dominated
            return 0;
        }

        // individual1 dominates individual2
        if (individual1IsBetterInOneObjective && !individual2IsBetterInOneObjective){
            return -1;
        }

        // individual2 dominates individual1
        return 1;
    }

    public List<T> getSolutions(){return archive;}

    public Grid<T> getGrid() {
        return grid;
    }
}
