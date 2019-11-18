package eu.stamp.botsing.ga.strategy.metaheuristics;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.commons.ga.strategy.operators.Mutation;
import eu.stamp.botsing.ga.stoppingconditions.SingleObjectiveZeroStoppingCondition;
import org.evosuite.Properties;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.comparators.RankAndCrowdingDistanceComparator;
import org.evosuite.ga.operators.crossover.CrossOverFunction;
import org.evosuite.ga.operators.ranking.CrowdingDistance;
import org.evosuite.ga.stoppingconditions.StoppingCondition;
import org.evosuite.ga.stoppingconditions.ZeroFitnessStoppingCondition;
import org.evosuite.utils.Randomness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

public class NSGAII<T extends Chromosome> extends org.evosuite.ga.metaheuristics.NSGAII<T> {

    private static final Logger LOG = LoggerFactory.getLogger(NSGAII.class);

    Mutation mutation;

    private int populationSize;

    private final CrowdingDistance<T> crowdingDistance;

    public NSGAII(ChromosomeFactory factory, CrossOverFunction crossOverOperator, Mutation mutationOperator) {
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
        this.crowdingDistance = new CrowdingDistance<T>();
    }

    @Override
    protected void evolve() {
        List<T> offspringPopulation = new ArrayList<T>(population.size());

        // create a offspring population
        for (int i = 0; i < (population.size() / 2); i++) {
            // Selection
            T parent1 = selectionFunction.select(population);
            T parent2 = selectionFunction.select(population);

            // crossOver
            T offspring1 = (T) parent1.clone();
            T offspring2 = (T) parent2.clone();

            try {
                if (Randomness.nextDouble() <= Properties.CROSSOVER_RATE){
                    crossoverFunction.crossOver(offspring1, offspring2);
                }
            } catch (Exception e) {
                LOG.info("CrossOver failed");
            }

            // Mutation
            if (Randomness.nextDouble() <= Properties.MUTATION_RATE) {
                notifyMutation(offspring1);
                mutation.mutateOffspring(offspring1);
                notifyMutation(offspring2);
                mutation.mutateOffspring(offspring2);
            }

            //calculate fitness
            calculateFitness(offspring1);
            calculateFitness(offspring2);

            // Add to offspring population
            offspringPopulation.add(offspring1);
            offspringPopulation.add(offspring2);
        }

        // Create the population union of Population and offSpring
        List<T> union = union(population, offspringPopulation);

        // Ranking the union
        this.rankingFunction.computeRankingAssignment(union, new LinkedHashSet(this.getFitnessFunctions()));


        int remain = population.size();
        int index = 0;
        List<T> front;
        population.clear();

        // Obtain the next front
        front = this.rankingFunction.getSubfront(index);

        while ((remain > 0) && (remain >= front.size())) {
            // Assign crowding distance to individuals
            this.crowdingDistance.crowdingDistanceAssignment(front, this.getFitnessFunctions());
            // Add the individuals of this front
            for (int k = 0; k < front.size(); k++){
                population.add(front.get(k));
            }

            // Decrement remain
            remain = remain - front.size();

            // Obtain the next front
            index++;
            if (remain > 0){
                front = this.rankingFunction.getSubfront(index);
            }
        }

        // Remain is less than front(index).size, insert only the best one
        if (remain > 0) {
            // front contains individuals to insert
            this.crowdingDistance.crowdingDistanceAssignment(front, this.getFitnessFunctions());

            Collections.sort(front, new RankAndCrowdingDistanceComparator<T>(true));

            for (int k = 0; k < remain; k++){
                population.add(front.get(k));
            }

//            remain = 0;
        }

        currentIteration++;
    }

    @Override
    public void initializePopulation() {
        notifySearchStarted();

        if (!population.isEmpty()) {
            return;
        }

// Generate Initial Population
        LOG.debug("Initializing the population.");
        generatePopulation(this.populationSize);

        calculateFitness();

        this.notifyIteration();

    }

    protected void generatePopulation(int populationSize) {
        LOG.debug("Creating random population");
        for (int i = 0; i < populationSize; i++) {
            T individual;
            individual = chromosomeFactory.getChromosome();
            for (FitnessFunction<?> fitnessFunction : this.fitnessFunctions) {
                individual.addFitness(fitnessFunction);
            }

            population.add(individual);

            if (isFinished()){
                break;
            }
        }
    }

    @Override
    public void generateSolution() {
        // Check if only zero value of only one objective  is important for us
        boolean containsSinglecObjectiveZeroSC = false;
        for (StoppingCondition condition: stoppingConditions){
            if (condition instanceof ZeroFitnessStoppingCondition){
                containsSinglecObjectiveZeroSC = true;
                break;
            }
        }

        // generate initial population
        LOG.info("Initializing the first population with size of {} individuals",this.populationSize);
        Boolean initilized = false;
        while (!initilized){
            try {
                initializePopulation();
                initilized=true;
            }catch (Exception |Error e){
                LOG.warn("Botsing was unsuccessful in generating the initial population. cause: {}",e.getMessage());
            }
        }

        while (!isFinished()) {
            LOG.info("Number of generations: {}",currentIteration+1);

            if(containsSinglecObjectiveZeroSC){
                reportBestFF();
            }

            evolve();
            this.notifyIteration();
            LOG.info("Value of fitness functions");
            this.writeIndividuals(this.population);
        }
    }

    private void reportBestFF() {

        for (StoppingCondition condition: stoppingConditions){
            if (condition instanceof ZeroFitnessStoppingCondition){
                SingleObjectiveZeroStoppingCondition selectedCondition = (SingleObjectiveZeroStoppingCondition) condition;
                LOG.info("The best FF for {} is {}",selectedCondition.getNameOfObjective(),selectedCondition.getCurrentValue());
                break;
            }
        }
    }
}
