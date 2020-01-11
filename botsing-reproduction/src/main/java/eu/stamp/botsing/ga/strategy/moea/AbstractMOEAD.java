package eu.stamp.botsing.ga.strategy.moea;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.StackTrace;
import eu.stamp.botsing.commons.ga.strategy.operators.Mutation;
import eu.stamp.botsing.fitnessfunction.FitnessFunctionHelper;
import eu.stamp.botsing.fitnessfunction.calculator.diversity.CallDiversityFitnessCalculator;
import eu.stamp.botsing.fitnessfunction.calculator.diversity.HammingDiversity;
import eu.stamp.botsing.fitnessfunction.testcase.factories.StackTraceChromosomeFactory;
import eu.stamp.botsing.ga.strategy.moea.point.IdealPoint;
import org.evosuite.Properties;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.ga.operators.crossover.CrossOverFunction;
import org.evosuite.utils.Randomness;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractMOEAD<T extends Chromosome> extends GeneticAlgorithm<T> {

    /** Z* vector */
    protected IdealPoint idealPoint;


    /** Lambda vectors (weights) */
    protected double[][] lambda;

    /** T */
    protected int neighborSize;
    protected int[][] neighborhood;

    /** Delta for neighborhood definition */
    protected double neighborhoodSelectionProbability;
    /** nr */
    protected int maximumNumberOfReplacedSolutions;



    // EA-related variables
    Mutation mutation;
    protected int populationSize;

    protected int numberOfSelectedParents = 2;

    protected CallDiversityFitnessCalculator<T> diversityCalculator;

    public AbstractMOEAD(ChromosomeFactory<T> factory, CrossOverFunction crossOverOperator, Mutation mutationOperator) {
        super(factory);
        mutation = mutationOperator;
        this.crossoverFunction = crossOverOperator;
        try {
            this.populationSize = CrashProperties.getInstance().getIntValue("population");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (Properties.NoSuchParameterException e) {
            e.printStackTrace();
        }


        this.maximumNumberOfReplacedSolutions = CrashProperties.maximumNumberOfReplacedSolutions ;


        int numberOfObjectives = CrashProperties.fitnessFunctions.length;

        idealPoint = new IdealPoint(numberOfObjectives);

        lambda = new double[populationSize][numberOfObjectives];

        neighborhoodSelectionProbability = CrashProperties.neighborhoodSelectionProbability;
        // For now, since we have only two objectives, we set T to 2
        neighborSize = 2;

        neighborhood = new int[populationSize][neighborSize];

        if (FitnessFunctionHelper.containsFitness(CrashProperties.FitnessFunction.CallDiversity)) {
            // initialize diversity calculator if it is needed
            StackTrace targetTrace = ((StackTraceChromosomeFactory) this.chromosomeFactory).getTargetTrace();
            diversityCalculator = HammingDiversity.getInstance(targetTrace);
        }
    }


    protected void initializeUniformWeight() {
        // Since we have two or three objectives here, and population size is lower than 300, our lambdas are generated automatically

        if(CrashProperties.fitnessFunctions.length == 2){
            for (int n = 0; n < populationSize; n++) {
                double a = 1.0 * n / (populationSize - 1);
                lambda[n][0] = a;
                lambda[n][1] = 1 - (a);
            }
        }else {
            for (int n = 0; n < populationSize; n++) {
                double a = 1.0 * n / (populationSize - 1);
                lambda[n][0] = a;
                lambda[n][1] = 1 - (a/2);
                lambda[n][2] = 1 - (a/2);
            }
        }

    }



    protected void updateSubProblemNeighborhood(T individual, int subProblemId, boolean selectFromNeighbor) {
        // set size of selection options
        int size;
        if (selectFromNeighbor) {
            size = neighborhood[subProblemId].length;
        } else {
            size = population.size();
        }

        int[] permutation = new int[size];
        MOEAUtils.randomPermutation(permutation, size);

        int numberOfReplaceSolutions=0;
        for (int i = 0; i < size; i++) {
            int k;
            if (selectFromNeighbor){
                k = neighborhood[subProblemId][permutation[i]];
            } else {
                k = permutation[i];
            }
            double f1, f2;

            f1 = calculateDistanceFromIdealPoint(population.get(k), lambda[k]);
            f2 = calculateDistanceFromIdealPoint(individual, lambda[k]);

            // If the distance is lower than the selected individual in the population, we will replace it with the new better one
            if (f2 < f1) {
                population.set(k, (T)individual.clone());
                numberOfReplaceSolutions++;
            }

            if (numberOfReplaceSolutions >= maximumNumberOfReplacedSolutions) {
                return;
            }
        }

    }

    private double calculateDistanceFromIdealPoint(T individual, double[] lambdas) {
        double distance;
        switch (CrashProperties.distanceCalculator){
            case WS:
                distance = 0.0;
                // distance = Sigma(lambda_i * f_i(x))
                for (int objectiveIndex=0; objectiveIndex < CrashProperties.fitnessFunctions.length;objectiveIndex++){
                    CrashProperties.FitnessFunction objective = CrashProperties.fitnessFunctions[objectiveIndex];
                    double fitnessValue = FitnessFunctionHelper.getFitnessValue(individual,objective);
                    distance += (lambdas[objectiveIndex] * fitnessValue);
                }
                return distance;
            case TE:
                distance = 0.0;
                // distance = max(lambda_i * |f_i(x)-Z_i|)
                for (int objectiveIndex=0; objectiveIndex < CrashProperties.fitnessFunctions.length;objectiveIndex++){
                    CrashProperties.FitnessFunction objective = CrashProperties.fitnessFunctions[objectiveIndex];
                    double fitnessValue = FitnessFunctionHelper.getFitnessValue(individual,objective);
                    double diff = Math.abs(fitnessValue - idealPoint.getValue(objectiveIndex)-CrashProperties.idealPointShift);
                    double currentValue;
                    if (lambdas[objectiveIndex] == 0) {
                        currentValue = 0.0001 * diff;
                    } else {
                        currentValue = lambdas[objectiveIndex] * diff;
                    }

                    if (currentValue > distance) {
                        distance = currentValue;
                    }
                }
                return distance;
            case PBI:
                double d1, d2, nl;
                double theta = 5.0;

                d1 = d2 = nl = 0.0;

                for (int objectiveIndex=0; objectiveIndex < CrashProperties.fitnessFunctions.length;objectiveIndex++){
                    CrashProperties.FitnessFunction objective = CrashProperties.fitnessFunctions[objectiveIndex];
                    double fitnessValue = FitnessFunctionHelper.getFitnessValue(individual,objective);
                    d1 += (fitnessValue - idealPoint.getValue(objectiveIndex)) * lambdas[objectiveIndex];
                    nl += Math.pow(lambdas[objectiveIndex], 2.0);
                }

                nl = Math.sqrt(nl);
                d1 = Math.abs(d1) / nl;

                for (int objectiveIndex=0; objectiveIndex < CrashProperties.fitnessFunctions.length;objectiveIndex++){
                    CrashProperties.FitnessFunction objective = CrashProperties.fitnessFunctions[objectiveIndex];
                    double fitnessValue = FitnessFunctionHelper.getFitnessValue(individual,objective);
                    d2 += Math.pow((fitnessValue - idealPoint.getValue(objectiveIndex)) - d1 * (lambdas[objectiveIndex] / nl), 2.0);
                }
                d2 = Math.sqrt(d2);

                distance = (d1 + theta * d2);
                return distance;
            default:
                throw new IllegalArgumentException("Distance calculator is unknown!");
        }
    }

    /**
     * Initialize neighborhoods of lambdas
     */
    protected void initializeSubProblemsNeighborhood() {
        double[] x = new double[populationSize];
        int[] idx = new int[populationSize];


        for (int i = 0; i < populationSize; i++) {
            // calculate the distances based on weight vectors (P=2)
            for (int j = 0; j < populationSize; j++) {
                x[j] = MOEAUtils.distVector(lambda[i], lambda[j]);
                idx[j] = j;
            }

            // find nearest neighboring subproblems
            MOEAUtils.minFastSort(x, idx, populationSize, neighborSize);

            System.arraycopy(idx, 0, neighborhood[i], 0, neighborSize);
        }
    }

    protected boolean chooseNeighbor() {
        double r = Randomness.nextDouble();

        if (r < neighborhoodSelectionProbability) {
            return true;
        }

        return false;
    }

    protected List<T> parentSelection(int subProblemId, boolean selectFromNeighbor) {

        int neighbourSize = neighborhood[subProblemId].length;


        Set<Integer> parentsIndex =  new HashSet<>();
        while (parentsIndex.size() < numberOfSelectedParents) {
            if (selectFromNeighbor){
                int random = Randomness.nextInt(0, neighbourSize);
                int selectedNeighborIndex = neighborhood[subProblemId][random];
                parentsIndex.add(selectedNeighborIndex);
            }else{
                int randomSelectedIndex = Randomness.nextInt(0, population.size());
                parentsIndex.add(randomSelectedIndex);
            }
        }
        List<Integer> parentsIndexList = new ArrayList<>();
        parentsIndexList.addAll(parentsIndex);

        List<T> parents = new ArrayList<>(numberOfSelectedParents);
        parents.add(population.get(parentsIndexList.get(0)));
        parents.add(population.get(parentsIndexList.get(1)));

        return parents;
    }

    @Override
    public T getBestIndividual() {
        if(this.population.isEmpty()){
            return this.chromosomeFactory.getChromosome();
        }

        // for one main FF
        CrashProperties.FitnessFunction mainObjective;
        if(CrashProperties.fitnessFunctions.length > 1 &
                (FitnessFunctionHelper.containsFitness(CrashProperties.FitnessFunction.WeightedSum) ||
                        FitnessFunctionHelper.containsFitness(CrashProperties.FitnessFunction.IntegrationSingleObjective))){
            if (CrashProperties.fitnessFunctions[0] == CrashProperties.FitnessFunction.TestLen){
                mainObjective = CrashProperties.fitnessFunctions[1];
            }else {
                mainObjective = CrashProperties.fitnessFunctions[0];
            }
        }else {
            return this.population.get(0);
        }

        for(T individual: this.population){
            double currentFitness = FitnessFunctionHelper.getFitnessValue(individual,mainObjective);
            if (currentFitness == 0){
                return individual;
            }
        }
        return this.population.get(0);
    }

}
