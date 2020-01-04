package eu.stamp.botsing.ga.strategy.archive.util;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.fitnessfunction.FitnessFunctionHelper;
import org.evosuite.ga.Chromosome;
import org.evosuite.utils.Randomness;

import java.util.*;

public class Grid<T extends Chromosome> {

    private int[] hypercubes;

    int numberOfObjectives;


    // <fitness function, [min,max]>
    private Map<CrashProperties.FitnessFunction,Double[]>  objectivesBoundaries = new HashMap<>();

    private int mostPopulatedHypercube;

    List<Integer> nonEmptyHyperCubes;

    public Grid(){
        numberOfObjectives = CrashProperties.fitnessFunctions.length;
        initializeBoundaries();
        initializeHyperCubes();
    }

    private void initializeHyperCubes() {
        hypercubes = new int[(int) Math.pow(2.0, CrashProperties.archiveBisections * CrashProperties.fitnessFunctions.length)];
        Arrays.fill(hypercubes, 0);

        nonEmptyHyperCubes = new ArrayList<>();
    }

    private void initializeBoundaries() {
        objectivesBoundaries.clear();
        for (CrashProperties.FitnessFunction ff: CrashProperties.fitnessFunctions){
            // two doubles: min and max
            objectivesBoundaries.put(ff,new Double[]{Double.MAX_VALUE,Double.MIN_VALUE});
        }
    }


    private void resetBoundaries() {
        objectivesBoundaries.clear();
        for (CrashProperties.FitnessFunction ff: CrashProperties.fitnessFunctions){
            // two doubles: min and max
            objectivesBoundaries.put(ff,new Double[]{Double.MAX_VALUE,Double.MIN_VALUE});
        }
    }


    private void updateBoundaries(List<T> archive){
        resetBoundaries();
        //Find and set the min and max limits of objectives in archive
        for (T individualInArchive: archive){
            for (CrashProperties.FitnessFunction ff: CrashProperties.fitnessFunctions){
                double individualsFitnessValue = FitnessFunctionHelper.getFitnessValue(individualInArchive,ff);
                double min = objectivesBoundaries.get(ff)[0];
                double max = objectivesBoundaries.get(ff)[1];
                Double[] newMinMax = {min,max};

                if (individualsFitnessValue < min){
                    newMinMax[0] = individualsFitnessValue;
                }

                if (individualsFitnessValue > max){
                    newMinMax[1] = individualsFitnessValue;
                }

                objectivesBoundaries.put(ff,newMinMax);
            }
        }
    }

    public int getHyperCube(T individual) {
        //Calculate the position for each objective
        int[] position = new int[numberOfObjectives];
        for(int ffIndex=0; ffIndex< numberOfObjectives;ffIndex++){
            CrashProperties.FitnessFunction ff =  CrashProperties.fitnessFunctions[ffIndex];
            double fitnessValue = FitnessFunctionHelper.getFitnessValue(individual,ff);

            if (!inBoundary(ff,fitnessValue)){
                throw new IllegalArgumentException("Fitness value of the the given individual is not in the detected range!");
            }

            if (objectivesBoundaries.get(ff)[0] == fitnessValue){
                position[ffIndex] = 0;
            }else if (objectivesBoundaries.get(ff)[1] == fitnessValue){
                position[ffIndex] = ((int) Math.pow(2.0, CrashProperties.archiveBisections)) - 1;
            }else {
                double size = getRangeSize(ff);
                double min = objectivesBoundaries.get(ff)[0];
                int cubeRange = (int) Math.pow(2.0, CrashProperties.archiveBisections);
                for (int bisection=0; bisection < CrashProperties.archiveBisections;bisection++){
                    size /= 2.0;
                    cubeRange /= 2;
                    if (fitnessValue > (min + size)) {
                        position[ffIndex] += cubeRange;
                        min += size;
                    }
                }
            }

        }

        //Calculate hypercube id
        int hyperCubeId = 0;
        for (int obj = 0; obj < numberOfObjectives; obj++) {
            hyperCubeId += position[obj] * Math.pow(2.0, obj * CrashProperties.archiveBisections);
        }
        return hyperCubeId;
    }

    private double getRangeSize(CrashProperties.FitnessFunction ff) {
        Double min = objectivesBoundaries.get(ff)[0];
        Double max = objectivesBoundaries.get(ff)[1];
        return max - min;
    }

    private boolean inBoundary(CrashProperties.FitnessFunction ff, double fitnessValue) {
        Double min = objectivesBoundaries.get(ff)[0];
        Double max = objectivesBoundaries.get(ff)[1];
        return (fitnessValue >= min) && (fitnessValue <= max);
    }

    public void updateGrid(List<T> archive) {
        // update min, max boundaries of objective values according to the given archive.
        updateBoundaries(archive);
        // Reset hyperCubes density
        Arrays.fill(hypercubes, 0);
        // Calculate all of the hypercubes
        calculateHyperCubes(archive);
    }

    private void calculateHyperCubes(List<T> archive) {
        mostPopulatedHypercube = 0;

        for (T individual: archive){
            int individualsHyperCube = this.getHyperCube(individual);
            hypercubes[individualsHyperCube]++;
            if (hypercubes[individualsHyperCube] > hypercubes[mostPopulatedHypercube]) {
                mostPopulatedHypercube = individualsHyperCube;
            }
        }

        updateNonEmptyHyperCubes();
    }

    private void updateNonEmptyHyperCubes() {
        nonEmptyHyperCubes.clear();
        for (int i = 0; i < hypercubes.length; i++) {
            if (hypercubes[i] > 0) {
                nonEmptyHyperCubes.add(new Integer(i));
            }
        }
    }

    public boolean inRange(T individual) {
        for (CrashProperties.FitnessFunction ff: CrashProperties.fitnessFunctions){

            double currentFitnessValue = FitnessFunctionHelper.getFitnessValue(individual,ff);
            double min = objectivesBoundaries.get(ff)[0];
            double max = objectivesBoundaries.get(ff)[1];

            if (currentFitnessValue < min || currentFitnessValue > max){
                return false;
            }
        }
        return true;
    }

    public void updateGrid(T individual, List<T> archive) {
        // Update boundaries according to the new individual
        updateBoundaries(individual,archive);
        // Reset hypercubes
        Arrays.fill(hypercubes, 0);
        // Calculate all of the hypercubes
        calculateHyperCubes(archive);
    }

    private void updateBoundaries(T individual, List<T> archive) {
        updateBoundaries(archive);

        //Actualize the lower and upper limits accroding to the new individual
        for (CrashProperties.FitnessFunction ff: CrashProperties.fitnessFunctions){
            double individualsFitnessValue = FitnessFunctionHelper.getFitnessValue(individual,ff);
            double min = objectivesBoundaries.get(ff)[0];
            double max = objectivesBoundaries.get(ff)[1];
            Double[] newMinMax = {min,max};

            if (individualsFitnessValue < min){
                newMinMax[0] = individualsFitnessValue;
            }

            if (individualsFitnessValue > max){
                newMinMax[1] = individualsFitnessValue;
            }

            objectivesBoundaries.put(ff,newMinMax);
        }

    }

    public void add(T individual) {
        int individualsHyperCube = this.getHyperCube(individual);
        //Increase density of individualsHyperCube
        hypercubes[individualsHyperCube]++;

        //Update the most populated hypercube
        if (hypercubes[individualsHyperCube] > hypercubes[mostPopulatedHypercube]) {
            mostPopulatedHypercube = individualsHyperCube;
        }

        // update non empty hyper cubes if this individual is added to an empty hyper cube
        if (hypercubes[individualsHyperCube] == 1) {
            this.updateNonEmptyHyperCubes();
        }

    }

    public boolean inMostPopulatedHypercube(T individual) {
        int individualsHyperCube = this.getHyperCube(individual);
        if (individualsHyperCube == mostPopulatedHypercube){
            return true;
        }
        return false;
    }

    public int getMostPopulatedHypercube(){return this.mostPopulatedHypercube;}

    public void removeIndividual(T individual) {
        int individualsHyperCube = this.getHyperCube(individual);
        // decrease the population in individualsHyperCube
        hypercubes[individualsHyperCube]--;

        // Update the mostPopulatedHyperCube, if it is needed
        if (individualsHyperCube == mostPopulatedHypercube){
            for (int i = 0; i < hypercubes.length; i++) {
                if (hypercubes[i] > hypercubes[mostPopulatedHypercube]) {
                    mostPopulatedHypercube = i;
                }
            }
        }

        // Update the nonEmptyHyperCubes, if it is needed
        if (hypercubes[individualsHyperCube] == 0){
            this.updateNonEmptyHyperCubes();
        }
    }

    public int getRandomHyperCube(boolean countEmpty){
        if (countEmpty){
            int randomHyperCube = Randomness.nextInt(0,hypercubes.length);
            return randomHyperCube;
        }else {
            if(nonEmptyHyperCubes.size() == 1){
                return nonEmptyHyperCubes.get(0);
            }
            int randomNonEmptyHyperCubeIndex = Randomness.nextInt(0,nonEmptyHyperCubes.size());
            return nonEmptyHyperCubes.get(randomNonEmptyHyperCubeIndex);
        }
    }

    public int getDensity(int hyperCubeIndex) {
        if (hyperCubeIndex >= hypercubes.length){
            throw new IllegalArgumentException("The given hyperCubeIndex ("+hyperCubeIndex+") is not available in hyper cubes");
        }
        return this.hypercubes[hyperCubeIndex];
    }
}
