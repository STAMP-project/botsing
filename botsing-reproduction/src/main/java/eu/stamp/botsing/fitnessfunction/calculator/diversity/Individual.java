package eu.stamp.botsing.fitnessfunction.calculator.diversity;

import org.evosuite.ga.Chromosome;
import org.evosuite.utils.generic.GenericAccessibleObject;

import java.util.Map;
import java.util.Objects;

public class Individual<T extends Chromosome>  {

    T chromosome;
    Map<GenericAccessibleObject<?>,Integer> methodCalls;

    public Individual(T chromosome, Map<GenericAccessibleObject<?>, Integer> methodCalls) {
        this.chromosome=chromosome;
        this.methodCalls=methodCalls;
    }

    public T getChromosome() {
        return chromosome;
    }

    public Map<GenericAccessibleObject<?>, Integer> getMethodCalls() {
        return methodCalls;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o){
            return true;
        }
        if (o == null || getClass() != o.getClass()){
            return false;
        }

        Individual<?> that = (Individual<?>) o;
        return (Objects.equals(chromosome, that.chromosome) &&
                Objects.equals(methodCalls, that.methodCalls));
    }

    @Override
    public int hashCode() {
        return Objects.hash(chromosome, methodCalls);
    }
}
