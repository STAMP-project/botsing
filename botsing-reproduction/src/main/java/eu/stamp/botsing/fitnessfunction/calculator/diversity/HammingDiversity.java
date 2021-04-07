package eu.stamp.botsing.fitnessfunction.calculator.diversity;

import eu.stamp.botsing.StackTrace;
import eu.stamp.botsing.fitnessfunction.utils.CallDiversityUtility;
import org.evosuite.ga.Chromosome;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.statements.Statement;
import org.evosuite.utils.generic.GenericAccessibleObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HammingDiversity<T extends Chromosome> extends CallDiversityFitnessCalculator<T> {

    private static final Logger LOG = LoggerFactory.getLogger(HammingDiversity.class);

    private static HammingDiversity instance = null;

    public static HammingDiversity getInstance(StackTrace targetTrace){
        if (instance == null){
            instance = new HammingDiversity(targetTrace);
        }
        if (!instance.targetTrace.equals(targetTrace)){
            throw new IllegalArgumentException("The target stack trace has been changed");
        }

        return instance;
    }

    private HammingDiversity(StackTrace targetTrace) {
        super(targetTrace);
    }

    @Override
    public double getSimilarityValue(T testChromosome){
        if(this.population.isEmpty()){
            throw new IllegalStateException("the population for checking diversity is empty");
        }

        double min = Double.MAX_VALUE;

        for (Individual<T> individual: this.population){
            min=Double.min(min,calculateHammingDistance(testChromosome,individual));
        }
        return min;
    }

    private double calculateHammingDistance(T testChromosome, Individual<T> individual) {
        Map<GenericAccessibleObject<?>, Integer> methodCallsOfGivenChromosome = calculateMethodCalls(testChromosome);
        Map<GenericAccessibleObject<?>, Integer> methodCallsOfIndividual = individual.getMethodCalls();

        if (methodCallsOfGivenChromosome.keySet().size() != methodCallsOfIndividual.keySet().size()){
            throw new IllegalArgumentException("Size of method calls are not the same");
        }

        int total = methodCallsOfGivenChromosome.keySet().size();

        int common = 0;

        for(GenericAccessibleObject<?> call: methodCallsOfGivenChromosome.keySet()){
            if(!methodCallsOfIndividual.containsKey(call)){
                throw new IllegalArgumentException("The keys of the given tests are not the same");
            }
            if(methodCallsOfGivenChromosome.get(call).intValue() == methodCallsOfIndividual.get(call).intValue()){
                common++;
            }
        }

        return ((double)common/total);
    }

    @Override
    public void addToPopulation(List<T> chromosomes){
        for (T chromosome : chromosomes){
            Map<GenericAccessibleObject<?>,Integer> methodCalls = calculateMethodCalls(chromosome);
            population.add(new Individual(chromosome,methodCalls));
        }
    }

    private Map<GenericAccessibleObject<?>, Integer> calculateMethodCalls(T chromosome) {
        if(callables.isEmpty()){
            throw new IllegalStateException("Callables list is empty");
        }

        if (!(chromosome instanceof TestChromosome)){
            throw new IllegalArgumentException("The given chromosome is not a test case");
        }

        Map<GenericAccessibleObject<?>, Integer> result = new HashMap<>();

        for (GenericAccessibleObject<?> call: callables){
            result.put(call,0);
        }

        TestChromosome givenTestChromosome = (TestChromosome) chromosome;

        int testSize = givenTestChromosome.getTestCase().size();
        // Check each of the statements
        for (int statementIndex=0; statementIndex <testSize; statementIndex++){
            Statement currentStatement = givenTestChromosome.getTestCase().getStatement(statementIndex);
            if(CallDiversityUtility.isInteresting(currentStatement,this.targetTrace.getTargetClass())){

                GenericAccessibleObject genObj = currentStatement.getAccessibleObject();

                if (!result.containsKey(genObj)){
                    LOG.debug("detected generic accessible object is not available  in the methods list.");
                }else{
                    result.put(genObj,1);
                }


            }
        }

        return result;
    }

}
