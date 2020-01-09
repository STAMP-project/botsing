package eu.stamp.botsing.commons.ga.strategy.mosa;

import eu.stamp.botsing.commons.fitnessfunction.FitnessFunctions;
import eu.stamp.botsing.commons.ga.strategy.operators.Mutation;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.comparators.OnlyCrowdingComparator;
import org.evosuite.ga.operators.crossover.CrossOverFunction;
import org.evosuite.ga.operators.ranking.CrowdingDistance;
import org.evosuite.ga.stoppingconditions.StoppingCondition;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.testsuite.TestSuiteFitnessFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class MOSA<T extends Chromosome> extends AbstractMOSA<T> {
    private static final Logger LOG = LoggerFactory.getLogger(MOSA.class);


    /** Boolean vector to indicate whether each test goal is covered or not. **/
    protected Set<FitnessFunction<T>> uncoveredGoals = new LinkedHashSet<FitnessFunction<T>>();

    protected CrowdingDistance<T> distance = new CrowdingDistance<T>();

    /** Maps each test target (exception) to the minimum fitness score it achieves */
    private HashMap<TestFitnessFunction, Double> fitnessTracker = new HashMap<TestFitnessFunction, Double>();

    /** Maps each test target (exception) to the point in time it is covered */
    private HashMap<TestFitnessFunction, Long> fitnessTimeTracker = new HashMap<TestFitnessFunction, Long>();

    private long startTime;

    private List<T> offspringPopulation = new ArrayList<>();

    private TestFitnessFunction criticalObjective;

    public MOSA(ChromosomeFactory<T> factory, CrossOverFunction crossOverOperator, Mutation<T> mutationOperator, FitnessFunctions fitnessCollector) {
        super(factory, fitnessCollector);
        mutation = mutationOperator;
        this.crossoverFunction = crossOverOperator;

        for (TestFitnessFunction ff : fitnessCollector.getFitnessFunctionList()) {
            String ffClassName = ff.getClass().getName();
            if (ffClassName.equals("eu.stamp.botsing.fitnessfunction.WeightedSum") || ffClassName.equals("eu" +
                    ".stamp.botsing.fitnessfunction.IntegrationTestingFF")) {
                criticalObjective = ff;
            }
        }
    }



    @Override
    protected void evolve() {
        offspringPopulation = this.breedNextGeneration();

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
        }

        try {
            LOG.info(criticalObjective + ": " + fitnessTracker.get(criticalObjective));
        } catch (Exception e) {
            LOG.info("SubFront is empty!");
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
        fitnessFunctions.clear();
        fitnessFunctions.addAll(uncoveredGoals);

        //initialize population
        if (this.population.isEmpty()) {
            this.initializePopulation();
        }

        // Calculate dominance ranks and crowding distance
        this.rankingFunction.computeRankingAssignment(this.population, this.uncoveredGoals);
        for (int i = 0; i < this.rankingFunction.getNumberOfSubfronts(); i++) {
            this.distance.fastEpsilonDominanceAssignment(this.rankingFunction.getSubfront(i), this.getUncoveredGoals());
        }


        while (!this.isFinished() && this.getNumberOfCoveredGoals()<this.fitnessFunctions.size() && ! fitnessCollector.isCriticalGoalsAreCovered(this.uncoveredGoals)) {
            LOG.info("Number of covered goals are {}/{}",this.getNumberOfCoveredGoals(),this.fitnessFunctions.size());
            this.evolve();
            LOG.info("generation #{} is created.",this.currentIteration);
            this.notifyIteration();
        }

        if(this.isFinished()){
            for(StoppingCondition stoppingCondition : this.stoppingConditions){
                if(stoppingCondition.isFinished()){
                    LOG.info("Stopping reason: {}", stoppingCondition.toString());
                }

            }
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
            LOG.debug("New covered goal: {}",covered);
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



    protected int getNumberOfCoveredGoals() {
        int n_covered_goals = this.fitnessFunctions.size() - this.uncoveredGoals.size();
        LOG.debug("# Covered Goals = " + n_covered_goals);
        return n_covered_goals;
    }

    public List<T> getOffspringPopulation() {
        return offspringPopulation;
    }
}
