package eu.stamp.botsing.ga.strategy.moea;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.commons.ga.strategy.operators.Mutation;
import eu.stamp.botsing.fitnessfunction.FitnessFunctionHelper;
import eu.stamp.botsing.fitnessfunction.utils.WSEvolution;
import eu.stamp.botsing.ga.GAUtil;
import org.evosuite.Properties;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.ConstructionFailedException;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.operators.crossover.CrossOverFunction;
import org.evosuite.utils.Randomness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class MOEAD <T extends Chromosome<T>> extends AbstractMOEAD<T> {
    private static final Logger LOG = LoggerFactory.getLogger(MOEAD.class);

    public MOEAD(ChromosomeFactory<T> factory, CrossOverFunction crossOverOperator, Mutation mutationOperator) {
        super(factory,crossOverOperator,mutationOperator);
    }

    @Override
    protected void evolve() {
        int[] permutation = new int[populationSize];
        MOEAUtils.randomPermutation(permutation, populationSize);

        for (int i = 0; i < populationSize; i++) {
            int subProblemId = permutation[i];

            boolean selectFromNeighbor = chooseNeighbor();

            List<T> parents = parentSelection(subProblemId, selectFromNeighbor) ;

            // Crossover
            T offspring1 = (T) parents.get(0).clone();
            T offspring2 = (T) parents.get(1).clone();

            if (Randomness.nextDouble() <= Properties.CROSSOVER_RATE) {
                try {
                    this.crossoverFunction.crossOver(offspring1, offspring2);
                } catch (ConstructionFailedException e) {
                    LOG.error("Crossover failed: " + e.getMessage());
                }
            }

            // Mutation
            if (Randomness.nextDouble() <= Properties.MUTATION_RATE) {
                this.notifyMutation(offspring1);
                mutation.mutateOffspring(offspring1);
                this.notifyMutation(offspring2);
                mutation.mutateOffspring(offspring2);
            }


            // Fitness Function Evaluation
            if (FitnessFunctionHelper.containsFitness(CrashProperties.FitnessFunction.CallDiversity)) {
                this.diversityCalculator.updateIndividuals(this.population,true);
            }

            for (final FitnessFunction<T> ff : this.getFitnessFunctions()) {
                ff.getFitness(offspring1);
                ff.getFitness(offspring2);
                notifyEvaluation(offspring1);
            }

            idealPoint.update(MOEAUtils.getPoints(offspring1));
            idealPoint.update(MOEAUtils.getPoints(offspring2));
            updateSubProblemNeighborhood(offspring1, subProblemId, selectFromNeighbor);
            updateSubProblemNeighborhood(offspring2, subProblemId, selectFromNeighbor);
        }

        for (int index=0;index< this.population.size();index++){
            LOG.info("{} for lambda {}",population.get(index).getFitnessValues().toString(),
                    Arrays.toString(this.lambda[index]));
        }
    }



    @Override
    public void initializePopulation() {
        if (!population.isEmpty()) {
            return;
        }

        // Generate Initial Population
        LOG.debug("Initializing the population.");
        generatePopulation(this.populationSize);

        // Calculate diversity values for initial population
        if (FitnessFunctionHelper.containsFitness(CrashProperties.FitnessFunction.CallDiversity)) {
            diversityCalculator.updateIndividuals(this.population,true);
        }
        // Calculate fitness functions
        calculateFitness();

        assert!population.isEmpty() : "Could not create any test";
    }

    private void generatePopulation(int populationSize) {
        LOG.debug("Creating random population");
        for (int i = 0; i < populationSize; i++) {
            T individual;
            individual = chromosomeFactory.getChromosome();
            for (FitnessFunction<T> fitnessFunction : this.fitnessFunctions) {
                individual.addFitness(fitnessFunction);
            }

            population.add(individual);

            if (isFinished()){
                break;
            }
        }
    }


    @Override
    protected void calculateFitness(T chromosome){
        for (FitnessFunction<T> fitnessFunction : fitnessFunctions) {
            notifyEvaluation(chromosome);
            fitnessFunction.getFitness(chromosome);
        }
    }

    @Override
    public void generateSolution() {
        // generate initial population
        LOG.info("Initializing the first population with size of {} individuals",this.populationSize);
        this.notifySearchStarted();
        WSEvolution.getInstance().setStartTime(this.listeners);
        Boolean initialized = false;
        while (!initialized){
            try {
                initializePopulation();
                initialized=true;
            }catch (Exception |Error e){
                LOG.warn("Botsing was unsuccessful in generating the initial population. cause: {}",e.getMessage());
            }

            if (isFinished()){
                break;
            }
        }

        // Calculate lambdas (sub-problems) according to the given search objectives
        initializeUniformWeight();
        // Calculate neighborhoods for determined lambdas
        initializeSubProblemsNeighborhood();
        // Update Z* according to the initial population
        MOEAUtils.updateIdealPoint(idealPoint,population);

        // Check if only zero value of only one objective  is important for us
        boolean containsSinglecObjectiveZeroSC = GAUtil.getSinglecObjectiveZeroSC(stoppingConditions);

        // The main iteration
        while (!isFinished()) {
            LOG.info("Number of generations: {}",currentIteration+1);

            // report ff value of the main (crash reporduction) objective
            if(containsSinglecObjectiveZeroSC){
                GAUtil.reportBestFF(stoppingConditions);
            }

            evolve();
            this.notifyIteration();
            this.currentIteration++;

        }
    }
}
