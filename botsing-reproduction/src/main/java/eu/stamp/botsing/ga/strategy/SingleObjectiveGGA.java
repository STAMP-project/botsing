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
import eu.stamp.botsing.fitnessfunction.WeightedSum;
import org.evosuite.Properties;
import org.evosuite.ga.*;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.ga.stoppingconditions.StoppingCondition;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.statements.ConstructorStatement;
import org.evosuite.testcase.statements.MethodStatement;
import org.evosuite.testcase.statements.Statement;
import org.evosuite.utils.Randomness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


public class SingleObjectiveGGA  <T extends Chromosome> extends GeneticAlgorithm<T> {
    private static final Logger LOG = LoggerFactory.getLogger(SingleObjectiveGGA.class);

    public SingleObjectiveGGA(ChromosomeFactory factory) {
        super(factory);
    }
    protected ReplacementFunction replacementFunction = new FitnessReplacementFunction();
    public void setFitnessFunction(){

    }


    @Override
    public void generateSolution() {
        // generate solution
        if (population.isEmpty()) {
            initializePopulation();
            assert!population.isEmpty() : "Could not create any test";
        }

        LOG.debug("Starting evolution");
        int starvationCounter = 0;
        double bestFitness = Double.MAX_VALUE;
        double lastBestFitness = Double.MAX_VALUE;

        double bestFFinInitialization = getBestFitness();
        LOG.debug("Best FF in the initial population is: "+bestFFinInitialization);
        while (!isFinished()){
            double bestFitnessBeforeEvolution = getBestFitness();
            evolve();
            sortPopulation();
            double newFitness = getBestFitness();
            LOG.info("New fitness Function is: "+newFitness);

            if (Double.compare(bestFitness, lastBestFitness) == 0) {
                starvationCounter++;
            } else {
                LOG.info("reset starvationCounter after " + starvationCounter + " iterations");
                starvationCounter = 0;
                lastBestFitness = bestFitness;

            }

            updateSecondaryCriterion(starvationCounter);

            LOG.info("Current iteration: " + currentIteration);
            this.notifyIteration();

        }
    }

    @Override
    protected void evolve() {
        List<T> newGeneration = new ArrayList<T>();
        // Elitism
        LOG.debug("Selection");

            newGeneration.addAll(elitism());

        while (!isPopulationFull(newGeneration) && !isFinished()) {
            LOG.debug("Generating offspring");
            T parent1 = selectionFunction.select(population);
            T parent2 = selectionFunction.select(population);
            T offspring1 = (T) parent1.clone();
            T offspring2 = (T) parent2.clone();
            // Crossover
            if (Randomness.nextDouble() <= Properties.CROSSOVER_RATE) {
                try{
                    crossoverFunction.crossOver(offspring1, offspring2);
                }catch (ConstructionFailedException e){
					LOG.error("construction failed when doing crossover!");
                    continue;
                }catch (Exception e) {
					LOG.error("Exception during the crossover!");
                }

            }
            // Check if offsprings contain the target method call
            if (!includesPublicCall(offspring1)) {
                offspring1 = (T) parent1.clone();
            } else if(!includesPublicCall(offspring2)) {
                offspring2 = (T) parent2.clone();
            }

            // Mutation
            try {
                mutateOffspring(offspring1);
                mutateOffspring(offspring2);
            }catch (Exception e) {
                LOG.error("Mutation is unsuccessful");
                e.printStackTrace();
            }
            // If and only if one of the offsprings is not worse than the best parent, we replace parents by offsprings.
            FitnessFunction fitnessFunction = fitnessFunctions.get(0);
            fitnessFunction.getFitness(offspring1);
            notifyEvaluation(offspring1);
            fitnessFunction.getFitness(offspring2);
            notifyEvaluation(offspring2);

            if (keepOffspring(parent1, parent2, offspring1, offspring2)) {
                LOG.debug("Replace parents");

                // Reject offspring straight away if it's too long
                int rejected = 0;
                if (isTooLong(offspring1) || offspring1.size() == 0) {
                    rejected++;
                } else {
                    // if(Properties.ADAPTIVE_LOCAL_SEARCH ==
                    // AdaptiveLocalSearchTarget.ALL)
                    // applyAdaptiveLocalSearch(offspring1);
                    newGeneration.add(offspring1);
                }

                if (isTooLong(offspring2) || offspring2.size() == 0) {
                    rejected++;
                } else {
                    // if(Properties.ADAPTIVE_LOCAL_SEARCH ==
                    // AdaptiveLocalSearchTarget.ALL)
                    // applyAdaptiveLocalSearch(offspring2);
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

    private boolean isPopulationFull(List<T> newGeneration) {
        try {
            if(newGeneration.size() >= CrashProperties.getIntValue("population")) {
                return true;
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (Properties.NoSuchParameterException e) {
            e.printStackTrace();
        }
        return false;
    }

    protected List<T>  elitism() {
        List<T> elite = new ArrayList<T>();
        LOG.debug("Cloning the best individuals to next generation");
        try {
            for (int i = 0; i < CrashProperties.getIntValue("elite"); i++) {
                elite.add(population.get(i));
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return elite;
    }

    @Override
    public void initializePopulation() {
        currentIteration = 0;
        // Generate Initial Population
        try {
            generatePopulation(CrashProperties.getIntValue("population"));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (Properties.NoSuchParameterException e) {
            e.printStackTrace();
        }

        LOG.debug("Initializing the population.");
        // Calculate fitness functions
        calculateFitnessOfPopulation();
        // Sort individuals
        sortPopulation();
    }

    protected void sortPopulation() {
        LOG.debug("Sort current population.");


        if (fitnessFunctions.get(0).isMaximizationFunction()) {
            Collections.sort(population, Collections.reverseOrder());
        } else {
            Collections.sort(population);
        }
    }

    private void calculateFitnessOfPopulation() {
        LOG.debug("Calculating fitness for " + population.size() + " individuals");
        Iterator<T> iterator = population.iterator();
        while (iterator.hasNext()) {
            T c = iterator.next();
            if (isFinished()) {
                if (c.isChanged()){
                    iterator.remove();
                }
            } else {
                for (FitnessFunction<T> fitnessFunction : fitnessFunctions) {
                    notifyEvaluation(c);
                    fitnessFunction.getFitness(c);
                }
            }
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

    private boolean includesPublicCall (T individual) {

        Iterator <String> publicCallsIterator = WeightedSum.publicCalls.iterator();
        TestChromosome candidateChrom = (TestChromosome) individual;
        TestCase candidate = candidateChrom.getTestCase();
        if (candidate.size() == 0){
            return false;
        }
        while (publicCallsIterator.hasNext()){
            String callName = publicCallsIterator.next();
            for ( int index= 0 ; index < candidate.size() ;index++) {
                Statement currentStatement = candidate.getStatement(index);
                if (!callName.contains(".") && currentStatement instanceof MethodStatement) {
                    MethodStatement candidateMethod = (MethodStatement) candidate.getStatement(index);
                    if (candidateMethod.getMethodName().equalsIgnoreCase(callName)) {
                        return true;
                    }
                } else if (callName.contains(".") && currentStatement instanceof ConstructorStatement){
                    if (callName.equals(((ConstructorStatement) currentStatement).getDeclaringClassName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    private void mutateOffspring (T offspring) {
        boolean permission = false;
        while (!permission) {
            // Mutation
            try{
                offspring.mutate();
            }catch(Exception | AssertionError e){
                LOG.error("Mutation failed!");
            }
            if (offspring.isChanged()) {
                offspring.updateAge(currentIteration);
            }
            try {
                permission = includesPublicCall(offspring);
            }catch(Exception e){
                LOG.error("Something went wrong when checking the target call after mutation! \n ");
            }
        }
    }

    protected boolean keepOffspring(Chromosome parent1, Chromosome parent2, Chromosome offspring1,
                                    Chromosome offspring2) {
        return replacementFunction.keepOffspring(parent1, parent2, offspring1, offspring2);
    }
}
