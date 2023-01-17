package eu.stamp.botsing.ga.strategy.metaheuristics;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.StackTrace;
import eu.stamp.botsing.commons.ga.strategy.operators.Mutation;
import eu.stamp.botsing.fitnessfunction.CallDiversity;
import eu.stamp.botsing.fitnessfunction.FitnessFunctionHelper;
import eu.stamp.botsing.fitnessfunction.IntegrationTestingFF;
import eu.stamp.botsing.fitnessfunction.WeightedSum;
import eu.stamp.botsing.fitnessfunction.calculator.diversity.CallDiversityFitnessCalculator;
import eu.stamp.botsing.fitnessfunction.calculator.diversity.HammingDiversity;
import eu.stamp.botsing.fitnessfunction.testcase.factories.StackTraceChromosomeFactory;
import eu.stamp.botsing.fitnessfunction.utils.WSEvolution;
import eu.stamp.botsing.ga.GAUtil;
import org.evosuite.Properties;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.ConstructionFailedException;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.operators.crossover.CrossOverFunction;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.utils.Randomness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SPEA2<T extends Chromosome<T>> extends org.evosuite.ga.metaheuristics.SPEA2<T>  {

    private static final Logger LOG = LoggerFactory.getLogger(SPEA2.class);

    Mutation mutation;
    private int populationSize;

    private Set<TestChromosome> crashReproducingTestCases;

    private CallDiversityFitnessCalculator<T> diversityCalculator;

    public SPEA2(ChromosomeFactory factory, CrossOverFunction crossOverOperator, Mutation mutationOperator) {
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


        // initialize diversity calculator if it is needed
        if (FitnessFunctionHelper.containsFitness(CrashProperties.FitnessFunction.CallDiversity)){
            StackTrace targetTrace = ((StackTraceChromosomeFactory) this.chromosomeFactory).getTargetTrace();
            diversityCalculator = HammingDiversity.getInstance(targetTrace);
        }

        if (!CrashProperties.stopAfterFirstCrashReproduction){
            crashReproducingTestCases = new HashSet<>();
        }
    }

    @Override
    protected void evolve() {
        List<T> offspringPopulation = new ArrayList<T>(population.size());

        while (offspringPopulation.size() < population.size()) {
            // Selection
            T parent1 = selectionFunction.select(archive);
            T parent2 = selectionFunction.select(archive);

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

        // Fitness function evaluation (excluding callDiversity) for individuals in the initial population.
        // CallDiversity will be calculated in updateArchive
        for (T element : offspringPopulation) {
            this.calculateFitness(element,false);
        }


        // Replacement
        this.population.clear();
        this.population.addAll(offspringPopulation);

        this.currentIteration++;

    }

    @Override
    public void generateSolution() {
        // Check if only zero value of only one objective  is important for us
        boolean containsSinglecObjectiveZeroSC = GAUtil.getSinglecObjectiveZeroSC(stoppingConditions);

        // generate initial population
        LOG.info("Initializing the first population with size of {} individuals",this.populationSize);
        Boolean initialized = false;
        this.notifySearchStarted();
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
            }else if (!CrashProperties.stopAfterFirstCrashReproduction){

            }

            evolve();
            this.updateArchive();
            if(!CrashProperties.stopAfterFirstCrashReproduction){
                this.updateCrashReproducingSet();
            }
            this.notifyIteration();
            this.writeIndividuals(this.archive);
        }
    }

    private void updateCrashReproducingSet() {
        for (T individual: this.archive){
            for (FitnessFunction<?> fitnessFunction: individual.getFitnessValues().keySet()){
                if (fitnessFunction instanceof WeightedSum || fitnessFunction instanceof IntegrationTestingFF){
                    double fitnessValue = individual.getFitnessValues().get(fitnessFunction);
//                    individual.
                    if(fitnessValue == 0){
                        // A crash reproducing test
                        if(!this.crashReproducingTestCases.contains(individual)){
                            org.evosuite.testcase.TestCaseMinimizer minimizer = new org.evosuite.testcase.TestCaseMinimizer((TestFitnessFunction) fitnessFunction);
                            TestChromosome copy = (TestChromosome) ((TestChromosome) individual).clone();
                            minimizer.minimize(copy);

                            if (((TestFitnessFunction) fitnessFunction).isCovered(copy)){
                                if (isNewcrashReproducingTestCase(copy)){
                                    LOG.info("Detect a new crash reproducing test case: {}",copy.getTestCase().toCode());
                                    this.crashReproducingTestCases.add(copy);
                                }
                            }else{
                                LOG.error("goal is not covered anymore by test {}",copy.getTestCase().toCode());
                            }

                        }
                    }
                }
            }
        }
    }

    private boolean isNewcrashReproducingTestCase(TestChromosome copy) {
        for (TestChromosome chromosome: this.crashReproducingTestCases){
            if(chromosome.getTestCase().toCode().equals(copy.getTestCase().toCode())){
                return false;
            }
        }
        return true;
    }

    @Override
    public void initializePopulation(){
        this.currentIteration=0;
        // Generate initial population
        this.generateInitialPopulation(Properties.POPULATION);

        // Create an empty archive
        this. archive = new ArrayList<T>(Properties.POPULATION);

        // prepare diversity calculator if needed
        if (FitnessFunctionHelper.containsFitness(CrashProperties.FitnessFunction.CallDiversity)) {
            diversityCalculator.updateIndividuals(this.population,true);
        }



        // Fitness function evaluation (excluding callDiversity) for individuals in the initial population.
        // CallDiversity will be calculated in updateArchive
        for (T element : this.population) {
            this.calculateFitness(element,false);
        }

        this.updateArchive();

        this.writeIndividuals(this.archive);

        this.notifyIteration();
    }

    @Override
    protected void updateArchive() {
        List<T> union = new ArrayList(2 * Properties.POPULATION);
        union.addAll(this.population);
        union.addAll(this.archive);
        // Add individuals to diversityCalculator if we have callDiversity among search objectives
        if (FitnessFunctionHelper.containsFitness(CrashProperties.FitnessFunction.CallDiversity)) {
            this.diversityCalculator.updateIndividuals(union, true);
            this.calculateDiversity(union);
        }

        this.computeStrength(union);
        this.archive = this.environmentalSelection(union);

        LOG.debug("Archive in generation {}:",this.currentIteration);
        // Print archive in Debug mode
        for (T individual : this.archive){
            LOG.debug("{} with distance {}",individual.getFitnessValues().toString(),individual.getDistance());
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

        // Update WSEvolution if we are running a multi-objectivization search
        if (FitnessFunctionHelper.containsFitness(CrashProperties.FitnessFunction.LineCoverage) &&
                FitnessFunctionHelper.containsFitness(CrashProperties.FitnessFunction.ExceptionType) &&
                FitnessFunctionHelper.containsFitness(CrashProperties.FitnessFunction.StackTraceSimilarity)){
            GAUtil.informWSEvolution(offspring);
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

    public Set<TestChromosome> getCrashReproducingTestCases() {
        return  (Set<TestChromosome>) crashReproducingTestCases;
    }
}
