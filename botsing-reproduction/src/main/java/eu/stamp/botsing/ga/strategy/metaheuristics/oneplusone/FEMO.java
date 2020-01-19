package eu.stamp.botsing.ga.strategy.metaheuristics.oneplusone;

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
import eu.stamp.botsing.ga.comparators.DominanceComparator;
import org.evosuite.Properties;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.utils.Randomness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class FEMO<T extends Chromosome> extends org.evosuite.ga.metaheuristics.GeneticAlgorithm<T> {

    Mutation mutation;

    private int populationSize;


    protected List<T> archive;

    protected Map<T,Integer> weights;

    private CallDiversityFitnessCalculator<T> diversityCalculator;

    private DominanceComparator<T> comparator;

    private static final Logger LOG = LoggerFactory.getLogger(FEMO.class);

    public FEMO(ChromosomeFactory<T> factory, Mutation mutationOperator) {
        super(factory);
        // Set GA-based variables
        this.stoppingConditions.clear();
        mutation = mutationOperator;

        try {
            this.populationSize = CrashProperties.getInstance().getIntValue("population");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (Properties.NoSuchParameterException e) {
            e.printStackTrace();
        }

        // initialize archive and weights
        archive = new ArrayList<>();
        weights = new HashMap<>();

        // initialize diversity calculator if it is needed
        if (FitnessFunctionHelper.containsFitness(CrashProperties.FitnessFunction.CallDiversity)){
            StackTrace targetTrace = ((StackTraceChromosomeFactory) this.chromosomeFactory).getTargetTrace();
            diversityCalculator = HammingDiversity.getInstance(targetTrace);
        }
        // Initialize dominance comparator
        this.comparator = new DominanceComparator<T>();
    }



    @Override
    public void generateSolution() {
        // Check if only zero value of only one objective  is important for us
        boolean containsSinglecObjectiveZeroSC = GAUtil.getSinglecObjectiveZeroSC(stoppingConditions);

        // generate initial population
//        LOG.info("Initializing the first population with size of {} individuals",this.populationSize);
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
            LOG.info("Number of generations: {}", currentIteration+1);
            LOG.info("Size of archive: {}", this.archive.size());

            if(containsSinglecObjectiveZeroSC){
                GAUtil.reportBestFF(stoppingConditions);
            }

            evolve();
            this.notifyIteration();
            this.writeIndividuals(this.archive);
        }
    }

    @Override
    protected void evolve() {
        // 1- We need to select an individual from archive.
        // 1-1- get individuals with lowest weight
        List<T> selectionCandidates = this.collectIndividualsWithLowestWeight();
        // 1-2- select one of them as our parent
        T parent = Randomness.choice(selectionCandidates);
        addWeight(parent);

        // 2- Mutate the selected individual to generate a new offspring
        T offspring = (T) parent.clone();
        notifyMutation(offspring);
        mutation.mutateOffspring(offspring);

        // Fitness function calculation (excluding diversity).
        // Diversity will be calculated in updateArchive method.
        calculateFitness(offspring,false);

        // 4- Add offspring to archive if it is non-dominated
        updateArchive(offspring);
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

    private void updateArchive(T offspring) {
        // 1- calculate diversity if it is needed
        if (FitnessFunctionHelper.containsFitness(CrashProperties.FitnessFunction.CallDiversity)){
            // ad individuals to diversity calculator
            diversityCalculator.updateIndividuals(this.archive,true);
            diversityCalculator.updateIndividuals(Arrays.asList(offspring),false);

            // calculate diversity for
            // individuals in archive
            calculateDiversity(this.archive);
            // and offspring
            calculateDiversity(offspring);
        }

        // 2- Check if offspring is already in the archive
        // same object
        if(this.archive.contains(offspring)){
            return;
        }
        //same test case
        for (T individual : this.archive){
            if (((TestChromosome) individual).getTestCase().equals(((TestChromosome) offspring).getTestCase())){
                return;
            }
        }

        // 3- Remove individuals in archive, which are dominated by offspring.
        Iterator<T> iterator = archive.iterator();
        while (iterator.hasNext()){
            T individual = iterator.next();
            // check if offspring dominates individual
            if(comparator.compare(offspring,individual) < 0){
                // offspring dominates individual. So, remove individual from weights and archive
                // from weights
                removeFromWeights(individual);
                // from archive
                iterator.remove();
            }
        }



        // 4- Check if we can add offspring to archive
        boolean dominated = false;
        for (T individual : this.archive){
            // check if individual dominates offspring
            if(comparator.compare(offspring,individual) > 0){
                dominated = true;
                break;
            }

            if (comparator.isEqual(offspring,individual)){
                dominated = true;
                break;
            }
        }
        if (dominated){
            return;
        }

        // 5- Here, we know that offspring should be added to the archive.
        // Add offspring to archive
        this.archive.add(offspring);
        weights.put(offspring,0);
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

    private void removeFromWeights(T individual) {
        if (!weights.containsKey(individual)){
            throw new IllegalArgumentException("The given individual is not available in weights");
        }

        weights.remove(individual);
    }

    private void addWeight(T individual) {
        if (!weights.containsKey(individual)){
            throw new IllegalArgumentException("The given individual is not available in weights");
        }

        int currentWeight = weights.get(individual);
        weights.put(individual, currentWeight+1);
    }

    private List<T> collectIndividualsWithLowestWeight() {
        List<T> result = new ArrayList<>();
        int min = Integer.MAX_VALUE;
        for (T individual: this.archive){
            if (getWeight(individual) == min){
                result.add(individual);
            }else if(getWeight(individual) < min){
                min = getWeight(individual);
                result.clear();
                result.add(individual);
            }
        }

        return result;
    }

    private int getWeight(T individual) {
        if (!weights.containsKey(individual)){
            throw new IllegalArgumentException("The given individual is not available in weights");
        }

        return weights.get(individual).intValue();
    }

    // In this algorithm, we just need one individual, which will be added to archive
    @Override
    public void initializePopulation() {
        // Don't do anything if the archive is not empty
        if (!this.archive.isEmpty()){
            return;
        }

        LOG.info("Initialize the first individual");
        // Generate individual
        T individual = chromosomeFactory.getChromosome();
        // Add search objectives to it
        for (FitnessFunction<?> fitnessFunction : this.fitnessFunctions) {
            individual.addFitness(fitnessFunction);
        }
        // calculate its fitness functions
        calculateFitness(individual,false);
        // Set its weight to 0
        weights.put(individual,0);
        // Add individual to archive
        this.archive.add(individual);
    }

    @Override
    public List<T> getPopulation() {
        return this.archive;
    }


    @Override
    public T getBestIndividual() {
        if(this.archive.isEmpty()){
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
            return this.archive.get(0);
        }

        for(T individual: this.archive){
            double currentFitness = FitnessFunctionHelper.getFitnessValue(individual,mainObjective);
            if (currentFitness == 0){
                return individual;
            }
        }
        return this.archive.get(0);
    }
}
