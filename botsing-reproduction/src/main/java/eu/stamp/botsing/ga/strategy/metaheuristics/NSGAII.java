package eu.stamp.botsing.ga.strategy.metaheuristics;

import eu.stamp.botsing.CrashProperties;

import eu.stamp.botsing.StackTrace;
import eu.stamp.botsing.commons.ga.strategy.operators.Mutation;
import eu.stamp.botsing.fitnessfunction.CallDiversity;
import eu.stamp.botsing.fitnessfunction.FitnessFunctionHelper;
import eu.stamp.botsing.fitnessfunction.calculator.diversity.CallDiversityFitnessCalculator;
import eu.stamp.botsing.fitnessfunction.calculator.diversity.HammingDiversity;
import eu.stamp.botsing.fitnessfunction.testcase.factories.StackTraceChromosomeFactory;
import eu.stamp.botsing.fitnessfunction.utils.WSEvolution;
import eu.stamp.botsing.ga.GAUtil;
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

    private CallDiversityFitnessCalculator<T> diversityCalculator;

    public NSGAII(ChromosomeFactory factory, CrossOverFunction crossOverOperator, Mutation mutationOperator) {
        super(factory);
        this.stoppingConditions.clear();
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

        // initialize diversity calculator if it is needed
        if (FitnessFunctionHelper.containsFitness(CrashProperties.FitnessFunction.CallDiversity)){
            StackTrace targetTrace = ((StackTraceChromosomeFactory) this.chromosomeFactory).getTargetTrace();
            diversityCalculator = HammingDiversity.getInstance(targetTrace);
        }

    }

    @Override
    protected void evolve() {
        List<T> offspringPopulation = new ArrayList<T>(population.size());

        // create an offspring population

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

            calculateFitness(offspring1,false);
            calculateFitness(offspring2,false);


            // Add to offspring population
            offspringPopulation.add(offspring1);
            offspringPopulation.add(offspring2);
        }

        // *** Merge
        // Create the population union of Population and offSpring
        List<T> union = union(population, offspringPopulation);
        if (FitnessFunctionHelper.containsFitness(CrashProperties.FitnessFunction.CallDiversity)){
            diversityCalculator.updateIndividuals(union,true);
            // calculate diversity for individuals in Union
            calculateDiversity(union);
        }

        // *** Sort
        // Ranking the union according to non-dominance
        this.rankingFunction.computeRankingAssignment(union, new LinkedHashSet(this.getFitnessFunctions()));

        // *** Truncate
        // Empty next population
        List<T> nextPopulation = new ArrayList<>();
        //Starting from the first front F0
        int index = 0;
        List<T> front;
        List<T> f0 = this.rankingFunction.getSubfront(index);
        LOG.info("* Front0 Size: {}",f0.size());
        for (T individual : f0){
            LOG.info("{}",individual.getFitnessValues().toString());
        }

        while (nextPopulation.size() < Properties.POPULATION){
            // obtaining the next front
            front=this.rankingFunction.getSubfront(index);
            // the remaining capacity of the next population
            int capacity = Properties.POPULATION - nextPopulation.size();
            // Assign crowding distance to individuals
            this.crowdingDistance.crowdingDistanceAssignment(front, this.getFitnessFunctions());
            // Check if the next poplation has the capacity to add all of the individuals of the current front
            if(capacity >= front.size()){
                // Add all of the individuals in the current front to the next population
                nextPopulation.addAll(front);
            }else{
                // Add the best ones according to the crowding distance
                // Sort the current front according to the crowding distance
                Collections.sort(front, new RankAndCrowdingDistanceComparator<T>(true));
                // add the first capacity individuals and add them to the next population
                for (int i = 0; i < capacity; i++){
                    nextPopulation.add(front.get(i));
                }
            }
            // increase the index for obtaining the next front
            index++;
        }

        // Next population is ready. We can proceed to the next iteration.
        population.clear();
        population.addAll(nextPopulation);

        currentIteration++;
    }

    private void calculateDiversity(List<T> union) {
        for (T chromosome : union){
            calculateDiversity(chromosome);
        }
    }

    private void calculateDiversity(T chromosome) {
        for (FitnessFunction<T> fitnessFunction :fitnessFunctions){
            if (fitnessFunction instanceof CallDiversity){
                fitnessFunction.getFitness(chromosome);
                this.notifyEvaluation(chromosome);
                return;
            }
        }
        // It should not be the case that a chromosome does not have a diversity fitness function
        throw new IllegalStateException("The GA algorithm does not have call diversity fitness function.");
    }

    private void calculateFitness(T offspring, boolean calculateDiversity) {
        if(calculateDiversity){
            calculateFitness(offspring);
            return;
        }

        for (FitnessFunction<T> fitnessFunction :fitnessFunctions){
            if (!(fitnessFunction instanceof CallDiversity)){
                fitnessFunction.getFitness(offspring);
                this.notifyEvaluation(offspring);
            }
        }

        // Update WSEvolution if we are running a multi-objectivization search
        if (FitnessFunctionHelper.containsFitness(CrashProperties.FitnessFunction.LineCoverage) &&
                FitnessFunctionHelper.containsFitness(CrashProperties.FitnessFunction.ExceptionType) &&
                FitnessFunctionHelper.containsFitness(CrashProperties.FitnessFunction.StackTraceSimilarity)){
            GAUtil.informWSEvolution(offspring);
        }
    }

    @Override
    public void initializePopulation() {


        if (!population.isEmpty()) {
            return;
        }

        // Generate Initial Population
        LOG.debug("Initializing the population.");
        generatePopulation(this.populationSize);

        calculateFitness(false);



        this.notifyIteration();

    }

    private void calculateFitness(boolean calculateDiversity) {
        for (T chromosome : this.population){
            if(this.isFinished()){
                break;
            }

            calculateFitness(chromosome,calculateDiversity);
        }
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
        boolean containsSinglecObjectiveZeroSC = GAUtil.getSinglecObjectiveZeroSC(stoppingConditions);

        // generate initial population
        LOG.info("Initializing the first population with size of {} individuals",this.populationSize);
        Boolean initialized = false;
        notifySearchStarted();
        WSEvolution.getInstance().setStartTime(this.listeners);
        while (!initialized){
            try {
                initializePopulation();
                initialized=true;
            }catch (Exception |Error e){
                LOG.warn("Botsing was unsuccessful in generating the initial population. cause: {}",e.getMessage());
            }

            if (isFinished()){
                break;
            }

        }

        while (!isFinished()) {
            LOG.info("Number of generations: {}",currentIteration+1);

            if(containsSinglecObjectiveZeroSC){
                GAUtil.reportBestFF(stoppingConditions);
            }else{
                GAUtil.reportNonDominatedFF((List<Chromosome>) this.rankingFunction.getSubfront(0),this.currentIteration+2);
            }

            evolve();
            this.notifyIteration();

            this.writeIndividuals(this.population);
        }
    }

    @Override
    public T getBestIndividual() {
        if(this.population.isEmpty()){
            return this.chromosomeFactory.getChromosome();
        }

        // for one main FF
        CrashProperties.FitnessFunction mainObjective;
        if(CrashProperties.fitnessFunctions.length > 1 &
                (FitnessFunctionHelper.containsFitness(CrashProperties.FitnessFunction.WeightedSum) ||
                        FitnessFunctionHelper.containsFitness(CrashProperties.FitnessFunction.IntegrationSingleObjective))){
            if (CrashProperties.fitnessFunctions[0] == CrashProperties.FitnessFunction.TestLen){
                mainObjective = CrashProperties.fitnessFunctions[1];
            }else {
                mainObjective = CrashProperties.fitnessFunctions[0];
            }
        }else {
            return this.population.get(0);
        }

        for(T individual: this.population){
            double currentFitness = FitnessFunctionHelper.getFitnessValue(individual,mainObjective);
            if (currentFitness == 0){
                return individual;
            }
        }
        return this.population.get(0);
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
