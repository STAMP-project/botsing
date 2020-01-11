package eu.stamp.botsing.ga.strategy.metaheuristics;

import com.sun.xml.internal.bind.v2.TODO;
import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.StackTrace;
import eu.stamp.botsing.commons.ga.strategy.operators.Mutation;
import eu.stamp.botsing.fitnessfunction.FitnessFunctionHelper;
import eu.stamp.botsing.fitnessfunction.calculator.diversity.CallDiversityFitnessCalculator;
import eu.stamp.botsing.fitnessfunction.calculator.diversity.HammingDiversity;
import eu.stamp.botsing.fitnessfunction.testcase.factories.StackTraceChromosomeFactory;
import eu.stamp.botsing.ga.GAUtil;
import org.evosuite.Properties;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.ConstructionFailedException;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.operators.crossover.CrossOverFunction;
import org.evosuite.utils.Randomness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class SPEA2<T extends Chromosome> extends org.evosuite.ga.metaheuristics.SPEA2<T>  {

    private static final Logger LOG = LoggerFactory.getLogger(SPEA2.class);

    Mutation mutation;
    private int populationSize;

    private CallDiversityFitnessCalculator<T> diversityCalculator;

    public SPEA2(ChromosomeFactory factory, CrossOverFunction crossOverOperator, Mutation mutationOperator) {
        super(factory);
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

        // Fitness Function Evaluation
        if (!FitnessFunctionHelper.containsFitness(CrashProperties.FitnessFunction.CallDiversity)) {
            for (T element : offspringPopulation) {
                for (final FitnessFunction<T> ff : this.getFitnessFunctions()) {
                    ff.getFitness(element);
                    notifyEvaluation(element);
                }
            }
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
        while (!initialized){
            try {
                initializePopulation();
                initialized=true;
            }catch (Exception |Error e){
                LOG.warn("Botsing was unsuccessful in generating the initial population. cause: {}",e.getMessage());
            }
        }
        // The main iteration
        while (!isFinished()) {
            LOG.info("Number of generations: {}",currentIteration+1);

            if(containsSinglecObjectiveZeroSC){
                GAUtil.reportBestFF(stoppingConditions);
            }

            evolve();
            this.updateArchive();
            this.notifyIteration();
            this.writeIndividuals(this.archive);
        }
    }

    @Override
    public void initializePopulation(){
        // TODO: override this to update indivuals in diversity calculator before fitness values calculation
    }

    @Override
    protected void updateArchive() {
        List<T> union = new ArrayList(2 * Properties.POPULATION);
        union.addAll(this.population);
        union.addAll(this.archive);
        if (FitnessFunctionHelper.containsFitness(CrashProperties.FitnessFunction.CallDiversity)) {
            this.diversityCalculator.updateIndividuals(union,true);
            // calculate fitness values, here
            for (T element : union) {
                for (final FitnessFunction<T> ff : this.getFitnessFunctions()) {
                    ff.getFitness(element);
                    notifyEvaluation(element);
                }
            }
        }

        this.computeStrength(union);
        this.archive = this.environmentalSelection(union);
    }

    @Override
    public T getBestIndividual() {
        if(this.population.isEmpty()){
            return this.chromosomeFactory.getChromosome();
        }

        // for one main FF
        CrashProperties.FitnessFunction mainObjective;
        if(CrashProperties.fitnessFunctions.length == 2){
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
