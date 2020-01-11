package eu.stamp.botsing.fitnessfunction.calculator.diversity;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.StackTrace;
import eu.stamp.botsing.commons.testgeneration.TestGenerationContextUtility;
import eu.stamp.botsing.fitnessfunction.FitnessFunctionHelper;
import eu.stamp.botsing.fitnessfunction.testcase.factories.StackTraceChromosomeFactory;
import eu.stamp.botsing.fitnessfunction.utils.CallDiversityUtility;
import eu.stamp.botsing.ga.strategy.operators.GuidedSearchUtility;
import org.evosuite.ga.Chromosome;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.setup.TestClusterUtils;
import org.evosuite.utils.generic.GenericAccessibleObject;
import org.evosuite.utils.generic.GenericConstructor;
import org.evosuite.utils.generic.GenericMethod;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

public abstract class CallDiversityFitnessCalculator<T extends Chromosome> {

    protected GuidedSearchUtility utility = new GuidedSearchUtility();
    protected StackTrace targetTrace;
    protected static Set<GenericAccessibleObject<?>> callables = new HashSet<GenericAccessibleObject<?>>();
    protected List<Individual> population;

    public CallDiversityFitnessCalculator(StackTrace targetTrace){
        this.targetTrace = targetTrace;
        population = new ArrayList<>();
        collectCallables();
    }

    public abstract double getSimilarityValue(T testChromosome);

    public void updateIndividuals(List<T> individuals, boolean isClear){
        if (FitnessFunctionHelper.containsFitness(CrashProperties.FitnessFunction.CallDiversity)){
            // We should update the population of call diversity calculator
            if(isClear){
                this.clearPopulation();
            }
            this.addToPopulation(individuals);
        }
    }

    public void clearPopulation(){
        population.clear();
    }

    public abstract void addToPopulation(List<T> chromosomes);


    private void collectCallables() {
        BytecodeInstruction publicCallInStackTrace =  utility.collectPublicCalls(targetTrace);

        Class targetClass = null;
        try {
            targetClass = Class.forName(publicCallInStackTrace.getClassName(),false, TestGenerationContextUtility.getTestGenerationContextClassLoader(false));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        // Collect callable methods
        Set<Method> methods = TestClusterUtils.getMethods(targetClass);
        for (Method method : methods){
            GenericAccessibleObject methodObj = new GenericMethod(method,targetClass);
            String methodName = method.getName()+ Type.getMethodDescriptor(method);
            if(!methodObj.isPrivate()){
                callables.add(methodObj);
            }
        }

        // Collect callable constructors
        Set<Constructor<?>> constructors = TestClusterUtils.getConstructors(targetClass);
        for (Constructor constructor : constructors){
            GenericAccessibleObject constructorObj = new GenericConstructor(constructor,targetClass);
            String constructorName = "<init>"+Type.getConstructorDescriptor(constructor);
            if(!constructorObj.isPrivate()){
                callables.add(constructorObj);
            }
        }
    }
}
