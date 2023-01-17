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
import eu.stamp.botsing.ga.strategy.archive.GridArchive;
import eu.stamp.botsing.ga.strategy.operators.selection.PESAIISelection;
import org.evosuite.Properties;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.ConstructionFailedException;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.ga.operators.crossover.CrossOverFunction;
import org.evosuite.utils.Randomness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class PESAII<T extends Chromosome<T>> extends GeneticAlgorithm<T> {
    private static final Logger LOG = LoggerFactory.getLogger(PESAII.class);

    Mutation mutation;

    private int populationSize;

    protected GridArchive<T> archive;

    private CallDiversityFitnessCalculator<T> diversityCalculator;


    public PESAII(ChromosomeFactory factory, CrossOverFunction crossOverOperator, Mutation mutationOperator) {
        super(factory);
        this.stoppingConditions.clear();
        this.crossoverFunction = crossOverOperator;
        mutation = mutationOperator;
        // Initialize an empty archive
        archive = new GridArchive<>();
        // set population size
        try {
            this.populationSize = CrashProperties.getInstance().getIntValue("population");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (Properties.NoSuchParameterException e) {
            e.printStackTrace();
        }
        // initialize PESA selection
        selectionFunction = new PESAIISelection<T>();

        if (FitnessFunctionHelper.containsFitness(CrashProperties.FitnessFunction.CallDiversity)) {
            // initialize diversity calculator if it is needed
            StackTrace targetTrace = ((StackTraceChromosomeFactory) this.chromosomeFactory).getTargetTrace();
            diversityCalculator = HammingDiversity.getInstance(targetTrace);
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

        // The main iteration
        while (!isFinished()) {
            LOG.info("Number of generations: {}",currentIteration+1);

            if(containsSinglecObjectiveZeroSC){
                GAUtil.reportBestFF(stoppingConditions);
            }

            evolve();
            this.notifyIteration();
            this.writeIndividuals(this.archive.getSolutions());
        }


    }
    @Override
    protected void evolve() {
        List<T> offspringPopulation = new ArrayList<T>(population.size());
        // At the beginning of each evolve, we update the archive with the new generation of individuals

        // calculate diversity fitness values if it has diversity in the search objectives
        if (FitnessFunctionHelper.containsFitness(CrashProperties.FitnessFunction.CallDiversity)) {
            // add individuals
            this.diversityCalculator.updateIndividuals(this.archive.getSolutions(),true);
            this.diversityCalculator.updateIndividuals(population,false);

            // Calculate diversity fitness values in archive and the  population

            // Diversity Fitness Function Evaluation of the population
            this.calculateDiversity(population);

            // Diversity Fitness Function Evaluation of the individuals in the archive
            this.calculateDiversity(this.archive.getSolutions());

            if(isFinished()){
                return;
            }

            // Update grid because the fitness values of tests in the archive have been changed
            archive.getGrid().updateGrid(this.archive.getSolutions());
        }

        archive.updateArchive(population);
        while (offspringPopulation.size() < population.size()) {
            // PESA-II Selection
            T parent1 = (T) ((PESAIISelection)selectionFunction).select(archive);
            T parent2 = (T) ((PESAIISelection)selectionFunction).select(archive);

            // Crossover
            T offspring1 = (T) parent1.clone();
            T offspring2 = (T) parent2.clone();
            if (Randomness.nextDouble() <= Properties.CROSSOVER_RATE) {
                try {
                    this.crossoverFunction.crossOver(offspring1, offspring2);
                } catch (ConstructionFailedException e) {
                    LOG.error("Crossover failed: " + e.getMessage());
                }
            }


            // Mutation
            if (Randomness.nextDouble() <= Properties.MUTATION_RATE) {
                this.notifyMutation(offspring1);
                mutation.mutateOffspring(offspring1);
                this.notifyMutation(offspring2);
                mutation.mutateOffspring(offspring2);
            }

            offspringPopulation.add(offspring1);
            offspringPopulation.add(offspring2);
        }

            // Fitness Function Evaluation except diversity
            // If we have diversity search objective, we will calculate its fitness values before archive update
            for (T element : offspringPopulation) {
                this.calculateFitness(element,false);
            }


        // Replacement
        this.population.clear();
        this.population.addAll(offspringPopulation);

        this.currentIteration++;

    }

    @Override
    public void initializePopulation() {


        if (!population.isEmpty()) {
            return;
        }

        // Generate Initial Population
        LOG.debug("Initializing the population.");
        generatePopulation(this.populationSize);

        // Fitness Function Evaluation except diversity
        // If we have diversity search objective, we will calculate its fitness values before archive update
        for (T element : this.population) {
            this.calculateFitness(element,false);
        }

        this.notifyIteration();

    }

    protected void generatePopulation(int populationSize) {
        LOG.debug("Creating random population");
        for (int i = 0; i < populationSize; i++) {
            T individual;
            individual = chromosomeFactory.getChromosome();
            for (FitnessFunction<T> fitnessFunction : this.fitnessFunctions) {
                individual.addFitness(fitnessFunction);
            }

            population.add(individual);

            if (isFinished()){
                break;
            }
        }
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

}
