package eu.stamp.botsing.ga.strategy;

/*-
 * #%L
 * botsing-reproduction
 * %%
 * Copyright (C) 2017 - 2018 eu.stamp-project
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.ga.strategy.operators.GuidedMutation;
import eu.stamp.botsing.ga.strategy.operators.GuidedSinglePointCrossover;
import org.evosuite.Properties;
import org.evosuite.ga.*;
import org.evosuite.ga.comparators.CrowdingComparator;
import org.evosuite.ga.comparators.DominanceComparator;
import org.evosuite.ga.comparators.SortByFitness;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.ga.stoppingconditions.StoppingCondition;
import org.evosuite.utils.Randomness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


public class GuidedNSGAII<T extends Chromosome> extends GeneticAlgorithm<T> {

    private static final Logger LOG = LoggerFactory.getLogger(GuidedNSGAII.class);

    private GuidedMutation<T> mutation;

    private int populationSize;

    private int eliteSize;

    private DominanceComparator dc;

    public GuidedNSGAII(ChromosomeFactory<T> factory) {
        super(factory);
        this.crossoverFunction = new GuidedSinglePointCrossover();
        this.mutation = new GuidedMutation<>();
        try {
            this.populationSize =  CrashProperties.getInstance().getIntValue("population");
            this.eliteSize = CrashProperties.getInstance().getIntValue("elite");
        } catch (IllegalAccessException e) {
            LOG.error("Illegal access during initialization", e);
        } catch (Properties.NoSuchParameterException e) {
            LOG.error("Parameter not found during initialization", e);
        }
        this.dc = new DominanceComparator();
    }

    @Override
    public void generateSolution() {
        currentIteration = 0;

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

        int starvationCounter = 0;
        double bestFitness = getBestFitness();
        double lastBestFitness = bestFitness;
        LOG.info("Best fitness in the initial population is: {}", bestFitness);
        long finalPT = getPassingTime();
        reportNewBestFF(lastBestFitness,finalPT);
        this.notifyIteration();
        LOG.info("Starting evolution");
        int generationCounter = 1;
        while (!isFinished()){
            // Create next generation
        	Boolean newGen = false;
            while (!newGen){
                try{
                    evolve();
                    sortPopulation();
                    newGen=true;
                }catch (Error | Exception e){
                    LOG.warn("Botsing was unsuccessful in generating new generation. cause: {}",e.getMessage());
                }
            }

            generationCounter++;
            bestFitness = getBestFitness();
            LOG.info("Best fitness in the current population: {} | {}", bestFitness,Properties.POPULATION *generationCounter);

            // Check for starvation
            if (Double.compare(bestFitness, lastBestFitness) == 0) {
                starvationCounter++;
            } else {
                LOG.debug("Reset starvationCounter after {} iterations", starvationCounter);
                starvationCounter = 0;
                lastBestFitness = bestFitness;
                finalPT = getPassingTime();
                reportNewBestFF(lastBestFitness,finalPT);
            }
            updateSecondaryCriterion(starvationCounter);

            LOG.debug("Current iteration: {}", currentIteration);
            this.notifyIteration();
        }
        LOG.info("The search process is finished.");
        reportNewBestFF(lastBestFitness,finalPT);
    }

    private void reportNewBestFF(double lastBestFitness, long finalPT) {
        if(Properties.STOPPING_CONDITION == Properties.StoppingCondition.MAXTIME){
            LOG.info("Best fitness in the final population is: {}. PT: {} seconds", lastBestFitness,finalPT);
        }else{
            LOG.info("Best fitness in the final population is: {}. FE: {} ", lastBestFitness,finalPT);
        }

    }

    @Override
    protected void evolve() {
        // Elitism
        LOG.debug("Selection");
        List<T> newGeneration = new ArrayList<T>(elitism());

        while (newGeneration.size() < this.populationSize && !isFinished()) {
            LOG.debug("Generating offspring");
            T parent1 = selectionFunction.select(population);
            T parent2 = selectionFunction.select(population);
            T offspring1 = (T) parent1.clone();
            T offspring2 = (T) parent2.clone();
            // Crossover
            if (Randomness.nextDouble() <= Properties.CROSSOVER_RATE) {
                ((GuidedSinglePointCrossover) crossoverFunction).crossOver(offspring1, offspring2);
            }

            // Mutation
            this.mutation.mutateOffspring(offspring1);
            notifyMutation(offspring1);
            this.mutation.mutateOffspring(offspring2);
            notifyMutation(offspring2);

            //calculate fitness
            calculateFitness(offspring1);
            calculateFitness(offspring2);

            // If and only if one of the offsprings is not worse than the best parent, we replace parents by offsprings.
            if (keepOffspring(parent1, parent2, offspring1, offspring2)) {
                LOG.debug("Replace parents");

                // Reject offspring straight away if it's too long
                int rejected = 0;
                if (isTooLong(offspring1) || offspring1.size() == 0) {
                    rejected++;
                } else {
                    newGeneration.add(offspring1);
                }

                if (isTooLong(offspring2) || offspring2.size() == 0) {
                    rejected++;
                } else {
                    newGeneration.add(offspring2);
                }

                if (rejected == 1) {
                    newGeneration.add(Randomness.choice(parent1, parent2));
                }else if (rejected == 2) {
                    newGeneration.add(parent1);
                    newGeneration.add(parent2);
                }
            } else {
                LOG.debug("Keep parents");
                newGeneration.add(parent1);
                newGeneration.add(parent2);
            }
        }

        population = newGeneration;
        // archive
        updateFitnessFunctionsAndValues();

        currentIteration++;
    }

    protected List<T>  elitism() {
        List<T> elite = new ArrayList<T>();
        LOG.debug("Cloning the best individuals to next generation");
        for (int i = 0; i < eliteSize; i++) {
            elite.add(population.get(i));
        }
        return elite;
    }

    @Override
    public void initializePopulation() {
        if (!population.isEmpty()) {
            return;
        }

        // Generate Initial Population
        generatePopulation(this.populationSize);

        LOG.debug("Initializing the population.");
        // Calculate fitness functions
        calculateFitness();
        // Sort individuals
        sortPopulation();
        assert!population.isEmpty() : "Could not create any test";
    }

    protected void sortPopulation() {
        LOG.debug("Sort current population.");
        List<T> unsorted = new ArrayList<T>(population);
        population.clear();
        // Ranking the unsorted population
        List<List<T>> ranking = fastNonDominatedSort(unsorted);
		// Iterate over fronts
		List<T> front = null;
		for (int i = 0; i < ranking.size(); i++) {
			// Obtain the next front
			front = ranking.get(i);
			// Assign crowding distance to individuals
			crowingDistanceAssignment(front);
			// Sort front
			Collections.sort(front, new CrowdingComparator(true));
			// Add the sorted individuals of this front to population
			for (int j = 0; j < front.size(); j++) {
				population.add(front.get(j));
			}
		}
    }

	private List<List<T>> fastNonDominatedSort(List<T> population) {
        // dominateMe[i] contains the number of individuals dominating i
        int[] dominateMe = new int[population.size()];

        // iDominate[k] contains the list of individuals dominated by k
        List<Integer>[] iDominate = new List[population.size()];

        // front[i] contains the list of individuals belonging to the front i
        List<Integer>[] front = new List[population.size() + 1];

        // flagDominate is an auxiliar variable
        int flagDominate;

        // Initialize the fronts
        for (int i = 0; i < front.length; i++) {
            front[i] = new LinkedList<Integer>();
		}

        // Fast non dominated sorting algorithm
        for (int p = 0; p < population.size(); p++) {
            // Initialize the list of individuals that i dominate and the number
            // of individuals that dominate me
            iDominate[p] = new LinkedList<Integer>();
            dominateMe[p] = 0;
        }

        for (int p = 0; p < (population.size() - 1); p++) {
            // for all q individuals, calculate if p dominates q or vice versa
            for (int q = p + 1; q < population.size(); q++) {
                //flagDominate = dominanceComparator(population.get(p), population.get(q));
                flagDominate = dc.compare(population.get(p), population.get(q));
                if (flagDominate == -1){
                    iDominate[p].add(q);
                    dominateMe[q]++;
                }else if (flagDominate == 1){
                    iDominate[q].add(p);
                    dominateMe[p]++;
                }
            }
            // if nobody dominates p, p belongs to the first front
        }

        for (int p = 0; p < population.size(); p++) {
            if (dominateMe[p] == 0){
                front[0].add(p);
                population.get(p).setRank(0);
            }
        }

        // obtain the rest of fronts
        int i = 0;
        Iterator<Integer> it1, it2;
        while (front[i].size() != 0) {
            i++;
            it1 = front[i - 1].iterator();
            while (it1.hasNext()) {
                it2 = iDominate[it1.next()].iterator();
                while (it2.hasNext()) {
                    int index = it2.next();
                    dominateMe[index]--;
                    if (dominateMe[index] == 0){
                        front[i].add(index);
                        population.get(index).setRank(i);
                    }
                }
            }
        }

        List<List<T>> ranking = new ArrayList<List<T>>(i);
        // 0,1,2,....,i-1 are front, then i fronts
        for (int j = 0; j < i; j++) {
            List<T> f = new ArrayList<T>(front[j].size());
            it1 = front[j].iterator();
            while (it1.hasNext()) {
                f.add(population.get(it1.next()));
			}
            ranking.add(f);
        }

        return ranking;
    }

    private void crowingDistanceAssignment(List<T> f) {
        int size = f.size();

        if (size == 0){
            return;
        }
        if (size == 1){
            f.get(0).setDistance(Double.POSITIVE_INFINITY);
            return;
        }
        if (size == 2){
            f.get(0).setDistance(Double.POSITIVE_INFINITY);
            f.get(1).setDistance(Double.POSITIVE_INFINITY);
            return;
        }

        // use a new Population List to avoid altering the original Population
        List<T> front = new ArrayList<T>(size);
        front.addAll(f);

        for (int i = 0; i < size; i++) {
            front.get(i).setDistance(0.0);
        }

        double objetiveMaxn;
        double objetiveMinn;
        double distance;

        for (final FitnessFunction<?> ff : this.getFitnessFunctions()) {
            // Sort the population by Fit n
            Collections.sort(front, new SortByFitness(ff, true));

            objetiveMinn = front.get(0).getFitness(ff);
            objetiveMaxn = front.get(front.size() - 1).getFitness(ff);

            // set crowding distance
            front.get(0).setDistance(Double.POSITIVE_INFINITY);
            front.get(size - 1).setDistance(Double.POSITIVE_INFINITY);

            for (int j = 1; j < size - 1; j++) {
                distance = front.get(j + 1).getFitness(ff) - front.get(j - 1).getFitness(ff);
                distance = distance / (objetiveMaxn - objetiveMinn);
                distance += front.get(j).getDistance();
                front.get(j).setDistance(distance);
            }
        }
    }

    protected void calculateFitness() {
        LOG.debug("Calculating fitness for " + population.size() + " individuals");
        Iterator<T> iterator = population.iterator();
        while (iterator.hasNext()) {
            T c = iterator.next();
            if (isFinished()) {
                if (c.isChanged()){
                    iterator.remove();
                }
            } else {
                calculateFitness(c);
            }
        }
    }

    protected void calculateFitness(T chromosome){
        for (FitnessFunction<T> fitnessFunction : fitnessFunctions) {
            notifyEvaluation(chromosome);
            double value = fitnessFunction.getFitness(chromosome);
        }
    }

    private void generatePopulation(int populationSize) {
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

    public long getPassingTime(){
        for (StoppingCondition c : stoppingConditions) {

            if(c.getClass().getName().contains("MaxFitnessEvaluationsStoppingCondition") || c.getClass().getName().contains("MaxTimeStoppingCondition")){
                return c.getCurrentValue();
            }
        }
        return 0;
    }

    public boolean isFinished() {
        for (StoppingCondition c : stoppingConditions) {
            LOG.debug("Current value of stopping condition "+ c.getClass().toString()+": "+c.getCurrentValue());

            // logger.error(c + " "+ c.getCurrentValue());
            if (c.isFinished()){
                LOG.info(c.toString());
                return true;
            }
        }
        return false;
    }

    private double getBestFitness() {
        T bestIndividual = getBestIndividual();
        for (FitnessFunction<T> ff : fitnessFunctions) {
            ff.getFitness(bestIndividual);
        }
        return bestIndividual.getFitness();
    }

    public T getBestIndividual() {
        if (population.isEmpty()) {
            return this.chromosomeFactory.getChromosome();
        }

        // Assume population is sorted
        return population.get(0);
    }

    protected boolean keepOffspring(T parent1, T parent2, T offspring1, T offspring2) {
    	List<T> unsorted = new ArrayList<T>(4);
        unsorted.add(parent1);
        unsorted.add(parent2);
        unsorted.add(offspring1);
        unsorted.add(offspring2);
        List<List<T>> ranking = fastNonDominatedSort(unsorted);
		if (ranking.get(0).contains(offspring1) || ranking.get(0).contains(offspring2)) {
			return true;
		} else {
			return false;
		}
    }

}
