package eu.stamp.botsing.commons.ga.strategy.mosa;

import eu.stamp.botsing.commons.fitnessfunction.CrashCoverageSuiteFitness;
import eu.stamp.botsing.commons.fitnessfunction.FitnessFunctions;
import eu.stamp.botsing.commons.ga.strategy.operators.Mutation;
import org.evosuite.Properties;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.ConstructionFailedException;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.utils.Randomness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class AbstractMOSA extends org.evosuite.ga.metaheuristics.mosa.AbstractMOSA {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractMOSA.class);
    protected Mutation<TestChromosome> mutation;

    /** Boolean vector to indicate whether each test goal is covered or not. **/
    protected Set<TestFitnessFunction> uncoveredGoals = new LinkedHashSet<>();
    protected Set<TestFitnessFunction> coveredGoals = new LinkedHashSet<>();

    /** Map used to store the covered test goals (keys of the map) and the corresponding covering test cases (values of the map) **/
    protected Map<FitnessFunction<TestChromosome>, TestChromosome> archive = new LinkedHashMap<>();
    FitnessFunctions fitnessCollector;

    public AbstractMOSA(ChromosomeFactory<TestChromosome> factory, FitnessFunctions fitnessCollector) {
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
    protected List<TestChromosome> breedNextGeneration() {
        List<TestChromosome> offspringPopulation = new ArrayList<>(Properties.POPULATION);
        // we apply only Properties.POPULATION/2 iterations since in each generation
        // we generate two offspring
        for (int i=0; i < Properties.POPULATION/2 && !isFinished(); i++){
            // select best individuals
            TestChromosome parent1 = selectionFunction.select(population);
            TestChromosome parent2 = selectionFunction.select(population);
            TestChromosome offspring1 = parent1.clone();
            TestChromosome offspring2 = parent2.clone();
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
            TestChromosome tch;
            if (this.getNumberOfCoveredGoals() == 0 || Randomness.nextBoolean()){
                tch = this.chromosomeFactory.getChromosome();
                tch.setChanged(true);
            } else {
                tch = (TestChromosome) Randomness.choice(getArchive()).clone();
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


    protected List<TestChromosome> getArchive() {
        Set<TestChromosome> set = new LinkedHashSet<>();
        set.addAll(archive.values());
        List<TestChromosome> arch = new ArrayList<>();
        arch.addAll(set);
        return arch;
    }

    @Override
    protected void evolve() {}

    @Override
    public void generateSolution() {}

//    @Override
//    protected Set<TestFitnessFunction> getCoveredGoals() {
//        return this.coveredGoals;
//    }

//    @Override
//    protected Set<TestFitnessFunction> getUncoveredGoals() {
//        return this.getUncoveredGoals();
//    }

}
