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
import eu.stamp.botsing.fitnessfunction.testcase.factories.StackTraceChromosomeFactory;
import eu.stamp.botsing.ga.strategy.operators.GuidedMutation;
import eu.stamp.botsing.ga.strategy.operators.GuidedSinglePointCrossover;
import eu.stamp.botsing.secondaryobjectives.TestCaseSecondaryObjective;
import org.evosuite.Properties;
import org.evosuite.ga.*;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.ga.stoppingconditions.StoppingCondition;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.utils.Randomness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


public class GuidedGeneticAlgorithm<T extends Chromosome> extends GeneticAlgorithm<T> {

    private static final Logger LOG = LoggerFactory.getLogger(GuidedGeneticAlgorithm.class);

    protected ReplacementFunction replacementFunction = new FitnessReplacementFunction();

    private GuidedMutation<T> mutation;

    private int populationSize;

    private int eliteSize;

    public GuidedGeneticAlgorithm(ChromosomeFactory<T> factory) {
        super(factory);
        this.stoppingConditions.clear();
        this.crossoverFunction = new GuidedSinglePointCrossover();
        ((GuidedSinglePointCrossover)this.crossoverFunction).updatePublicCalls(((StackTraceChromosomeFactory) chromosomeFactory).getPublicCalls());
        this.mutation = new GuidedMutation<>();
        this.mutation.updatePublicCalls(((StackTraceChromosomeFactory) chromosomeFactory).getPublicCalls());
        try {
            this.populationSize =  CrashProperties.getInstance().getIntValue("population");
            this.eliteSize = CrashProperties.getInstance().getIntValue("elite");
        } catch (IllegalAccessException e) {
            LOG.error("Illegal access during initialization", e);
        } catch (Properties.NoSuchParameterException e) {
            LOG.error("Parameter not found during initialization", e);
        }

        // set the secondary objectives of test cases (useful when MOSA compares two test
        // cases to, for example, update the archive)
        TestCaseSecondaryObjective.setSecondaryObjectives();
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
            TestChromosome bestTest = (TestChromosome) getBestIndividual();

            LOG.debug("*  The best generated test is: "+bestTest.getTestCase().toCode());
            LOG.debug("{} thrown exception(s) are detected in the best test case: ",bestTest.getLastExecutionResult().getAllThrownExceptions().size());
            for(Throwable t: bestTest.getLastExecutionResult().getAllThrownExceptions()){
                LOG.debug(t.toString());

                for(StackTraceElement frame:t.getStackTrace()){
                    LOG.debug(frame.toString());
                }

            }

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
        if (fitnessFunctions.get(0).isMaximizationFunction()) {
            Collections.sort(population, Collections.reverseOrder());
        } else {
            Collections.sort(population);
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

    protected boolean keepOffspring(Chromosome parent1, Chromosome parent2, Chromosome offspring1,
                                    Chromosome offspring2) {
        return replacementFunction.keepOffspring(parent1, parent2, offspring1, offspring2);
    }
}
