package eu.stamp.botsing.commons.ga.strategy.mosa;

import eu.stamp.botsing.commons.fitnessfunction.CrashCoverageSuiteFitness;
import eu.stamp.botsing.commons.fitnessfunction.FitnessFunctions;
import eu.stamp.botsing.commons.ga.strategy.operators.Mutation;
import org.evosuite.Properties;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.ConstructionFailedException;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.utils.Randomness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class AbstractMOSA<T extends Chromosome> extends org.evosuite.ga.metaheuristics.mosa.AbstractMOSA<T> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractMOSA.class);
    protected Mutation<T> mutation;

    /** Boolean vector to indicate whether each test goal is covered or not. **/
    protected Set<FitnessFunction<T>> uncoveredGoals = new LinkedHashSet<FitnessFunction<T>>();
    protected Set<FitnessFunction<T>> coveredGoals = new LinkedHashSet<FitnessFunction<T>>();

    /** Map used to store the covered test goals (keys of the map) and the corresponding covering test cases (values of the map) **/
    protected Map<FitnessFunction<T>, T> archive = new LinkedHashMap<FitnessFunction<T>, T>();
    FitnessFunctions fitnessCollector;

    public AbstractMOSA(ChromosomeFactory<T> factory, FitnessFunctions fitnessCollector) {
        super(factory);
        this.fitnessCollector = fitnessCollector;
        getSuiteFitnessFunctions();
    }

    @Override
    protected void setupSuiteFitness(){

    }


    protected void getSuiteFitnessFunctions(){
        for (FitnessFunction ff: this.fitnessCollector.getFitnessFunctionList()){
            this.suiteFitnessFunctions.put(new CrashCoverageSuiteFitness(fitnessCollector),ff.getClass());
        }
    }


    @Override
    /**
     * This method is used to generate new individuals (offsprings) from
     * the current population
     * @return offspring population
     */
    @SuppressWarnings("unchecked")
    protected List<T> breedNextGeneration() {
        List<T> offspringPopulation = new ArrayList<T>(Properties.POPULATION);
        // we apply only Properties.POPULATION/2 iterations since in each generation
        // we generate two offspring
        for (int i=0; i < Properties.POPULATION/2 && !isFinished(); i++){
            // select best individuals
            T parent1 = selectionFunction.select(population);
            T parent2 = selectionFunction.select(population);
            T offspring1 = (T) parent1.clone();
            T offspring2 = (T) parent2.clone();
            // apply crossover
            if (Randomness.nextDouble() <= Properties.CROSSOVER_RATE) {
                try {
                    crossoverFunction.crossOver(offspring1, offspring2);
                } catch (ConstructionFailedException e) {
                    e.printStackTrace();
                }
            }

            // Remove unused variables from the offsprings (for minimization)
            removeUnusedVariables(offspring1);
            removeUnusedVariables(offspring2);


            // Mutation
            this.mutation.mutateOffspring(offspring1);
            notifyMutation(offspring1);
            if (offspring1.isChanged()) {
                clearCachedResults(offspring1);
                offspring1.updateAge(currentIteration);
                calculateFitness(offspring1);
                offspringPopulation.add(offspring1);
            }

            this.mutation.mutateOffspring(offspring2);
            notifyMutation(offspring2);
            if (offspring2.isChanged()) {
                clearCachedResults(offspring2);
                offspring2.updateAge(currentIteration);
                calculateFitness(offspring2);
                offspringPopulation.add(offspring2);
            }


            // ToDo: Should we add size check?
        }
        // Add new randomly generate tests
        for (int i = 0; i<Properties.POPULATION * Properties.P_TEST_INSERTION; i++){
            T tch;
            if (this.getNumberOfCoveredGoals() == 0 || Randomness.nextBoolean()){
                tch = this.chromosomeFactory.getChromosome();
                tch.setChanged(true);
            } else {
                tch = (T) Randomness.choice(getArchive()).clone();
                this.mutation.mutateOffspring(tch);
                this.mutation.mutateOffspring(tch);
            }
            if (tch.isChanged()) {
                tch.updateAge(currentIteration);
                calculateFitness(tch);
                offspringPopulation.add(tch);
            }
        }
        LOG.debug("Number of offsprings = {}", offspringPopulation.size());
        return offspringPopulation;
    }


    protected List<T> getArchive() {
        Set<T> set = new LinkedHashSet<T>();
        set.addAll(archive.values());
        List<T> arch = new ArrayList<T>();
        arch.addAll(set);
        return arch;
    }

    @Override
    protected void evolve() {}

    @Override
    public void generateSolution() {}

    @Override
    protected Set<FitnessFunction<T>> getCoveredGoals() {
        return this.coveredGoals;
    }

    @Override
    protected Set<FitnessFunction<T>> getUncoveredGoals() {
        return this.uncoveredGoals;
    }

}
