package eu.stamp.botsing.ga.strategy.mosa;

import eu.stamp.botsing.commons.ga.strategy.mosa.AbstractMOSA;
import eu.stamp.botsing.commons.ga.strategy.operators.Mutation;
import eu.stamp.botsing.fitnessfunction.FitnessFunctions;
import eu.stamp.botsing.ga.strategy.mosa.structural.BotsingMultiCriteriatManager;
import org.evosuite.Properties;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.comparators.OnlyCrowdingComparator;
import org.evosuite.ga.metaheuristics.mosa.structural.StructuralGoalManager;
import org.evosuite.ga.operators.crossover.CrossOverFunction;
import org.evosuite.ga.operators.ranking.CrowdingDistance;
import org.evosuite.ga.stoppingconditions.StoppingCondition;
import org.evosuite.testcase.TestChromosome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DynaMOSA extends AbstractMOSA {
    private static final Logger LOG = LoggerFactory.getLogger(DynaMOSA.class);

    protected StructuralGoalManager goalsManager = null;
    protected CrowdingDistance<TestChromosome> distance = new CrowdingDistance<TestChromosome>();

    public DynaMOSA(ChromosomeFactory<TestChromosome> factory, CrossOverFunction crossOverOperator, Mutation mutationOperator) {
        super(factory, new FitnessFunctions());
        mutation = mutationOperator;
        this.crossoverFunction = crossOverOperator;
    }


    @Override
    protected void evolve() {
        LOG.info("DynaMOSA evolve");
        List<TestChromosome> offspringPopulation = this.breedNextGeneration();

        // Create the union of parents and offSpring
        List<TestChromosome> union = new ArrayList<TestChromosome>(this.population.size() + offspringPopulation.size());
        union.addAll(this.population);
        union.addAll(offspringPopulation);

        // Ranking the union
        LOG.debug("Union Size = {}", union.size());

        // Ranking the union using the best rank algorithm (modified version of the non dominated sorting algorithm
        this.rankingFunction.computeRankingAssignment(union, this.goalsManager.getCurrentGoals());

        // let's form the next population using "preference sorting and non-dominated sorting" on the
        // updated set of goals
        int remain = Math.max(Properties.POPULATION, this.rankingFunction.getSubfront(0).size());
        int index = 0;
        List<TestChromosome> front = null;
        this.population.clear();

        // Obtain the next front
        front = this.rankingFunction.getSubfront(index);

        while ((remain > 0) && (remain >= front.size()) && !front.isEmpty()) {
            // Assign crowding distance to individuals
            this.distance.fastEpsilonDominanceAssignment(front, this.goalsManager.getCurrentGoals());

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
            this.distance.fastEpsilonDominanceAssignment(front, this.goalsManager.getCurrentGoals());
            Collections.sort(front, new OnlyCrowdingComparator());
            for (int k = 0; k < remain; k++) {
                this.population.add(front.get(k));
            }

            remain = 0;
        }

        this.currentIteration++;

        LOG.debug("Covered goals = {}", goalsManager.getCoveredGoals().size());
        LOG.debug("Current goals = {}", goalsManager.getCurrentGoals().size());
        LOG.debug("Uncovered goals = {}", goalsManager.getUncoveredGoals().size());
    }


    @Override
    public void generateSolution() {
        LOG.info("Generating solution in DynaMOSA");

        this.goalsManager = new BotsingMultiCriteriatManager(this.fitnessFunctions);

        LOG.info("* Initialsss Number of Goals in DynMOSA = " +
                this.goalsManager.getCurrentGoals().size() +" / "+ this.getUncoveredGoals().size());

        LOG.debug("Initial Number of Goals = " + this.goalsManager.getCurrentGoals().size());

        //initialize population
        if (this.population.isEmpty()) {
            this.initializePopulation();
        }

        // update current goals
        this.calculateFitness();
        printEvaluations();
        // Calculate dominance ranks and crowding distance
        this.rankingFunction.computeRankingAssignment(this.population, this.goalsManager.getCurrentGoals());

        for (int i = 0; i < this.rankingFunction.getNumberOfSubfronts(); i++){
            this.distance.fastEpsilonDominanceAssignment(this.rankingFunction.getSubfront(i), this.goalsManager.getCurrentGoals());
        }

        // next generations
        while (!isFinished() && this.goalsManager.getUncoveredGoals().size() > 0) {
            this.evolve();
            printEvaluations();
            this.notifyIteration();
        }

        this.notifySearchFinished();
    }

    @Override
    protected void calculateFitness(TestChromosome c) {
        this.goalsManager.calculateFitness(c, this);
        this.notifyEvaluation(c);
    }


    public void printEvaluations(){
        LOG.info("* naaame: ");
        for (StoppingCondition stoppingCondition: stoppingConditions){
            LOG.info("* naaame: "+stoppingCondition.getClass().getName());
            if (stoppingCondition.getClass().getName().contains("MaxFitnessEvaluations")){
                LOG.info("Current fitness evaluations: "+stoppingCondition.getCurrentValue());
            }
        }

    }
}
