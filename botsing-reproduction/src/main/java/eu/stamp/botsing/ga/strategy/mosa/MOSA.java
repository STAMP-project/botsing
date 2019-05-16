package eu.stamp.botsing.ga.strategy.mosa;

import eu.stamp.botsing.fitnessfunction.testcase.factories.StackTraceChromosomeFactory;
import eu.stamp.botsing.ga.strategy.operators.GuidedMutation;
import eu.stamp.botsing.ga.strategy.operators.GuidedSinglePointCrossover;
import org.evosuite.Properties;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.FitnessFunction;

import java.util.*;

import org.evosuite.ga.comparators.OnlyCrowdingComparator;
import org.evosuite.ga.operators.ranking.CrowdingDistance;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.testsuite.TestSuiteFitnessFunction;
import org.evosuite.utils.Randomness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MOSA<T extends Chromosome> extends AbstractMOSA<T> {
    private static final Logger LOG = LoggerFactory.getLogger(MOSA.class);
    private GuidedMutation<T> mutation;

    /** Boolean vector to indicate whether each test goal is covered or not. **/
    protected Set<FitnessFunction<T>> uncoveredGoals = new LinkedHashSet<FitnessFunction<T>>();

    protected CrowdingDistance<T> distance = new CrowdingDistance<T>();

    /** Map used to store the covered test goals (keys of the map) and the corresponding covering test cases (values of the map) **/
    protected Map<FitnessFunction<T>, T> archive = new LinkedHashMap<FitnessFunction<T>, T>();

    /** Maps each test target (exception) to the minimum fitness score it achieves */
    private HashMap<TestFitnessFunction, Double> fitnessTracker = new HashMap<TestFitnessFunction, Double>();

    /** Maps each test target (exception) to the point in time it is covered */
    private HashMap<TestFitnessFunction, Long> fitnessTimeTracker = new HashMap<TestFitnessFunction, Long>();

    private long startTime;


    public MOSA(ChromosomeFactory factory) {
        super(factory);
        mutation = new GuidedMutation<>();
        this.crossoverFunction = new GuidedSinglePointCrossover();
    }


    @Override
    /**
     * This method is used to generate new individuals (offsprings) from
     * the current population
     * @return offspring population
     */
    @SuppressWarnings("unchecked")
    protected List<T> breedNextGeneration() {
        StackTraceChromosomeFactory rootFactory = (StackTraceChromosomeFactory) chromosomeFactory;
//        boolean callsAreInjected = rootFactory.getRootMethodsFlag();
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
                ((GuidedSinglePointCrossover) crossoverFunction).crossOver(offspring1, offspring2);
            }

            // Remove unused variables from the offsprings (for minimization)
            removeUnusedVariables(offspring1);
            removeUnusedVariables(offspring2);
//
//            // From GUIDEDGA [Rmoved because this is already done in botsing's guided crossover]
//            // Check the inclusion of a target call.
//            if (callsAreInjected && !includesPublicCall(offspring1)) {
//                offspring1 = (T) parent1.clone();
//            } else if(callsAreInjected && !includesPublicCall(offspring2)) {
//                offspring2 = (T) parent2.clone();
//            }


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
    @Override
    protected void evolve() {
        List<T> offspringPopulation = this.breedNextGeneration();

        // Create the union of parents and offSpring
        List<T> union = new ArrayList<T>();
        union.addAll(this.population);
        union.addAll(offspringPopulation);


        Set<FitnessFunction<T>> uncoveredGoals = this.uncoveredGoals;

        // Ranking the union
        LOG.debug("Union Size =" + union.size());
        // Ranking the union using the best rank algorithm (modified version of the non dominated sorting algorithm)
        this.rankingFunction.computeRankingAssignment(union, uncoveredGoals);

        int remain = this.population.size();
        int index = 0;
        List<T> front;
        this.population.clear();

        // Obtain the next front
        front = this.rankingFunction.getSubfront(index);

        while ((remain > 0) && (remain >= front.size()) && !front.isEmpty()) {
            // Assign crowding distance to individuals
            this.distance.fastEpsilonDominanceAssignment(front, uncoveredGoals);
            // Add the individuals of this front
            this.population.addAll(front);

            // Decrement remain
            remain = remain - front.size();

            // Obtain the next front
            index++;
            if (remain > 0) {
                front = this.rankingFunction.getSubfront(index);
            }
        }

        // Remain is less than front(index).size, insert only the best one
        if (remain > 0 && !front.isEmpty()) { // front contains individuals to insert
            this.distance.fastEpsilonDominanceAssignment(front, uncoveredGoals);
            Collections.sort(front, new OnlyCrowdingComparator());
            for (int k = 0; k < remain; k++) {
                this.population.add(front.get(k));
            }

            remain = 0;
        }


        this.currentIteration++;
    }

    @Override
    public void generateSolution() {
        LOG.info("Generating solution in MOSA");

        this.startTime = System.nanoTime();
        // keep track of covered goals
        for (FitnessFunction<T> goal : fitnessFunctions) {
            uncoveredGoals.add(goal);
        }

        //initialize population
        if (this.population.isEmpty()) {
            this.initializePopulation();
        }

        // Calculate dominance ranks and crowding distance
        this.rankingFunction.computeRankingAssignment(this.population, this.uncoveredGoals);
        for (int i = 0; i < this.rankingFunction.getNumberOfSubfronts(); i++) {
            this.distance.fastEpsilonDominanceAssignment(this.rankingFunction.getSubfront(i), this.getUncoveredGoals());
        }


        while (!this.isFinished() && this.getNumberOfCoveredGoals()<this.fitnessFunctions.size()) {
            this.evolve();
            LOG.info("generation #{} is created.",this.currentIteration);
            LOG.info("Number of covered goals are {}/{}",this.getNumberOfCoveredGoals(),this.fitnessFunctions.size());
            this.notifyIteration();
        }
    }


    @Override
    @SuppressWarnings("unchecked")
    protected void calculateFitness(T c) {
        for (FitnessFunction<T> fitnessFunction : this.fitnessFunctions) {
            double value = fitnessFunction.getFitness(c);
            if (value == 0.0) {
                ((TestChromosome)c).getTestCase().addCoveredGoal((TestFitnessFunction) fitnessFunction);
                updateArchive(c, fitnessFunction);
            }

//            if (fitnessFunction instanceof CrashCoverageTestFitness) {
                if (!fitnessTracker.containsKey(fitnessFunction)) {
                    fitnessTracker.put((TestFitnessFunction) fitnessFunction, value);
                } else if (fitnessTracker.get(fitnessFunction) > value) {
                    fitnessTracker.put((TestFitnessFunction) fitnessFunction, value);
                }

                // storing the search time
                long endTime = System.nanoTime();
                long searchTime = (endTime - this.startTime) / 1000000000;

                // if time tracker does not include the target, we add it with the current search time
                if (!fitnessTimeTracker.containsKey(fitnessFunction)) {
                    fitnessTimeTracker.put((TestFitnessFunction) fitnessFunction, searchTime);
                } else if (fitnessTimeTracker.containsKey(fitnessFunction) && fitnessTracker.get(fitnessFunction) > value) {
                    // since the goal is included already, if the fitness score improved, we track the time
                    fitnessTimeTracker.put((TestFitnessFunction) fitnessFunction, searchTime);
                }
//            }
        }
        notifyEvaluation(c);
    }


    private void updateArchive(T solution, FitnessFunction<T> covered) {
        // the next two lines are needed since that coverage information are used
        // during EvoSuite post-processing
        TestChromosome tch = (TestChromosome) solution;
        tch.getTestCase().getCoveredGoals().add((TestFitnessFunction) covered);

        // store the test cases that are optimal for the test goal in the
        // archive
        if (archive.containsKey(covered)){
            int bestSize = this.archive.get(covered).size();
            int size = solution.size();
            if (size < bestSize){
                this.archive.put(covered, solution);
            }
        } else {
            archive.put(covered, solution);
            this.uncoveredGoals.remove(covered);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getBestIndividual() {
        List<T> archiveContent = this.getArchive();
        if (archiveContent.isEmpty()) {
            TestSuiteChromosome suite = new TestSuiteChromosome();
            for (TestSuiteFitnessFunction suiteFitness : suiteFitnessFunctions.keySet()) {
                suite.setFitness(suiteFitness, Double.MAX_VALUE);
            }
            return (T) suite;
        }

        TestSuiteChromosome best = new TestSuiteChromosome();
        for (T test : archiveContent) {
            best.addTest((TestChromosome) test);
        }
        // compute overall fitness and coverage
        double coverage = ((double) this.getNumberOfCoveredGoals()) / ((double) this.fitnessFunctions.size());
        for (TestSuiteFitnessFunction suiteFitness : suiteFitnessFunctions.keySet()){
            best.setCoverage(suiteFitness, coverage);
            best.setFitness(suiteFitness,  this.uncoveredGoals.size());
        }
        return (T) best;
    }


    @SuppressWarnings("unchecked")
    @Override
    public List<T> getBestIndividuals() {
        //get final test suite (i.e., non dominated solutions in Archive)
        List<T> finalTestSuite = this.getFinalTestSuite();
        if (finalTestSuite.isEmpty()) {
            return Arrays.asList((T) new TestSuiteChromosome());
        }

        TestSuiteChromosome bestTestCases = new TestSuiteChromosome();
        for (T test : finalTestSuite) {
            bestTestCases.addTest((TestChromosome) test);
        }
        for (FitnessFunction<T> f : this.getCoveredGoals()){
            bestTestCases.getCoveredGoals().add((TestFitnessFunction) f);
        }
        // compute overall fitness and coverage
        double fitness = this.fitnessFunctions.size() - getNumberOfCoveredGoals();
        double coverage = ((double) getNumberOfCoveredGoals()) / ((double) this.fitnessFunctions.size());
        for (TestSuiteFitnessFunction suiteFitness : this.suiteFitnessFunctions.keySet()){
            bestTestCases.setFitness(suiteFitness, fitness);
            bestTestCases.setCoverage(suiteFitness, coverage);
            bestTestCases.setNumOfCoveredGoals(suiteFitness, (int) getNumberOfCoveredGoals());
            bestTestCases.setNumOfNotCoveredGoals(suiteFitness, (int) (this.fitnessFunctions.size()-getNumberOfCoveredGoals()));
        }
        List<T> bests = new ArrayList<T>(1);
        bests.add((T) bestTestCases);
        return bests;
    }

    protected List<T> getFinalTestSuite() {
        // trivial case where there are no branches to cover or the archive is empty
        if (this.getNumberOfCoveredGoals()==0) {
            return getArchive();
        }
        if (archive.size() == 0){
            if (population.size() > 0) {
                ArrayList<T> list = new ArrayList<T>();
                list.add(population.get(population.size() - 1));
                return list;
            } else{
                return getArchive();
            }
        }

        List<T> final_tests = getArchive();
        List<T> tests = this.getNonDominatedSolutions(final_tests);
        return tests;
    }

    protected List<T> getArchive() {
        Set<T> set = new LinkedHashSet<T>();
        set.addAll(archive.values());
        List<T> arch = new ArrayList<T>();
        arch.addAll(set);
        return arch;
    }

    protected int getNumberOfCoveredGoals() {
        int n_covered_goals = this.fitnessFunctions.size() - this.uncoveredGoals.size();
        LOG.debug("# Covered Goals = " + n_covered_goals);
        return n_covered_goals;
    }
}
