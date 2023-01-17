package eu.stamp.botsing.commons.ga.strategy.mosa;

import eu.stamp.botsing.commons.fitnessfunction.FitnessFunctions;
import eu.stamp.botsing.commons.ga.strategy.operators.Mutation;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.FitnessFunction;

import java.util.*;

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

public class MOSA extends AbstractMOSA {
    private static final Logger LOG = LoggerFactory.getLogger(MOSA.class);




    protected CrowdingDistance<TestChromosome> distance = new CrowdingDistance<TestChromosome>();

    /** Maps each test target (exception) to the minimum fitness score it achieves */
    private HashMap<TestFitnessFunction, Double> fitnessTracker = new HashMap<TestFitnessFunction, Double>();

    /** Maps each test target (exception) to the point in time it is covered */
    private HashMap<TestFitnessFunction, Long> fitnessTimeTracker = new HashMap<TestFitnessFunction, Long>();

    private long startTime;

    private List<TestChromosome> offspringPopulation;


    public MOSA(ChromosomeFactory factory, CrossOverFunction crossOverOperator, Mutation mutationOperator, FitnessFunctions fitnessCollector) {
        super(factory, fitnessCollector);
        mutation = mutationOperator;
        this.crossoverFunction = crossOverOperator;
    }



    @Override
    protected void evolve() {
        offspringPopulation = this.breedNextGeneration();

        // Create the union of parents and offSpring
        List<TestChromosome> union = new ArrayList<TestChromosome>();
        union.addAll(this.population);
        union.addAll(offspringPopulation);


        Set<TestFitnessFunction> uncoveredGoals = this.getUncoveredGoals();

        // Ranking the union
        LOG.debug("Union Size =" + union.size());
        // Ranking the union using the best rank algorithm (modified version of the non dominated sorting algorithm)
        this.rankingFunction.computeRankingAssignment(union, uncoveredGoals);

        int remain = this.population.size();
        int index = 0;
        List<TestChromosome> front;
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

        try{



            Map<FitnessFunction<TestChromosome>, Double> front0= this.rankingFunction.getSubfront(0).get(0).getFitnessValues();
            this.fitnessCollector.printCriticalTargets(front0);

        }catch (Exception e){
            LOG.info("SubFront is empty!");

        }



        this.currentIteration++;
    }

    @Override
    public void generateSolution() {
        LOG.info("Generating solution in MOSA");

        this.startTime = System.nanoTime();
        // keep track of covered goals
        for (FitnessFunction<TestChromosome> goal : fitnessFunctions) {

            uncoveredGoals.add((TestFitnessFunction)goal);
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
            this.distance.fastEpsilonDominanceAssignment(this.rankingFunction.getSubfront(i), this.uncoveredGoals);
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
                    LOG.info("Number of covered goals are: {}", this.coveredGoals.size());
                }

            }
        }
    }





    @Override
    @SuppressWarnings("unchecked")
    protected void calculateFitness(TestChromosome c) {
        for (FitnessFunction<TestChromosome> fitnessFunction : this.fitnessFunctions) {
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


    private void updateArchive(TestChromosome solution, FitnessFunction<TestChromosome> covered) {
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
            this.coveredGoals.add((TestFitnessFunction) covered);
            LOG.debug("New covered goal: {}",covered);
        }
    }

//    @Override
//    @SuppressWarnings("unchecked")
//    public TestSuiteChromosome getBestIndividual() {
//        List<TestChromosome> archiveContent = this.getArchive();
//        if (archiveContent.isEmpty()) {
//            TestSuiteChromosome suite = new TestSuiteChromosome();
//            for (TestSuiteFitnessFunction suiteFitness : suiteFitnessFunctions.keySet()) {
//                suite.setFitness(suiteFitness, Double.MAX_VALUE);
//            }
//            return suite;
//        }
//
//        TestSuiteChromosome best = new TestSuiteChromosome();
//        for (TestChromosome test : archiveContent) {
//            best.addTest((TestChromosome) test);
//        }
//        // compute overall fitness and coverage
//        double coverage = ((double) this.getNumberOfCoveredGoals()) / ((double) this.fitnessFunctions.size());
//        for (TestSuiteFitnessFunction suiteFitness : suiteFitnessFunctions.keySet()){
//            best.setCoverage(suiteFitness, coverage);
//            best.setFitness(suiteFitness,  this.uncoveredGoals.size());
//        }
//        return best;
//    }


    @SuppressWarnings("unchecked")
    @Override
    public List<TestChromosome> getBestIndividuals() {
        //get final test suite (i.e., non dominated solutions in Archive)
        List<TestChromosome> finalTestSuite = this.getFinalTestSuite();
        if (finalTestSuite.isEmpty()) {
            return Arrays.asList(new TestChromosome());
        }

        TestSuiteChromosome bestTestCases = new TestSuiteChromosome();
        for (TestChromosome test : finalTestSuite) {
            bestTestCases.addTest(test);
        }
        for (FitnessFunction<TestChromosome> f : this.getCoveredGoals()){
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
        List<TestChromosome> bests = new ArrayList<>(1);
        bests.addAll(bestTestCases.getTestChromosomes());
        return bests;
    }

    protected List<TestChromosome> getFinalTestSuite() {
        // trivial case where there are no branches to cover or the archive is empty
        if (this.getNumberOfCoveredGoals()==0) {
            return getArchive();
        }
        if (archive.size() == 0){
            if (population.size() > 0) {
                ArrayList<TestChromosome> list = new ArrayList<TestChromosome>();
                list.add(population.get(population.size() - 1));
                return list;
            } else{
                return getArchive();
            }
        }

        List<TestChromosome> final_tests = getArchive();
        List<TestChromosome> tests = this.getNonDominatedSolutions(final_tests);
        return tests;
    }



    protected int getNumberOfCoveredGoals() {
        int n_covered_goals = this.fitnessFunctions.size() - this.uncoveredGoals.size();
        LOG.debug("# Covered Goals = " + n_covered_goals);
        return n_covered_goals;
    }


    public List<TestChromosome> getOffspringPopulation() {
        return offspringPopulation;
    }

    @Override
    public List<TestChromosome> getNonDominatedSolutions(List<TestChromosome> solutions) {
        return super.getNonDominatedSolutions(solutions);
    }



}
