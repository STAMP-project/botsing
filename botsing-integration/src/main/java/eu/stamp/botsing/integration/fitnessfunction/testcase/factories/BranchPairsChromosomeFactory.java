package eu.stamp.botsing.integration.fitnessfunction.testcase.factories;

import eu.stamp.botsing.integration.testgeneration.CallableMethodPool;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.ConstructionFailedException;
import org.evosuite.testcase.DefaultTestCase;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFactory;
import org.evosuite.testcase.execution.ExecutionTracer;
import org.evosuite.utils.Randomness;
import org.evosuite.utils.generic.GenericAccessibleObject;
import org.evosuite.utils.generic.GenericConstructor;
import org.evosuite.utils.generic.GenericMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import org.evosuite.Properties;


public class BranchPairsChromosomeFactory implements ChromosomeFactory<TestChromosome> {
    private static final Logger LOG = LoggerFactory.getLogger(BranchPairsChromosomeFactory.class);

    // Public or protected calls which reach to a call_site
    private Set<GenericAccessibleObject<?>> callableMethods = new HashSet<>();
    private Set<GenericAccessibleObject<?>> attemptedMethods = new HashSet<GenericAccessibleObject<?>>();
    // All of the available public and protected calls
    private List<GenericAccessibleObject<?>> allMethods = new LinkedList<GenericAccessibleObject<?>>();
    private int max_injections = 150;


    public BranchPairsChromosomeFactory(){
        allMethods.clear();
        allMethods.addAll(CallableMethodPool.getInstance().getAllMethods());
        Randomness.shuffle(allMethods);

        fillCallableMethods();


    }

    private void fillCallableMethods() {

        callableMethods.clear();

        callableMethods.addAll(CallableMethodPool.getInstance().getCallableMethods());
        attemptedMethods.clear();
    }


    @Override
    public TestChromosome getChromosome() {
        TestChromosome chromosome = new TestChromosome();
        chromosome.setTestCase(getRandomTestCase(Properties.CHROMOSOME_LENGTH));
        return chromosome;
    }

    private TestCase getRandomTestCase(int chromosomeLength) {
        boolean tracerEnabled = ExecutionTracer.isEnabled();
        if (tracerEnabled) {
            ExecutionTracer.disable();
        }
        // Counts the number of injected target calls in the created test.
        int target_counter = 0;
        int max_rounds = 0;
        TestCase test = null;
        // Loop until the created method has at least one target call or it reaches the maximum number of rounds.
        while (target_counter < 1 && (max_rounds < max_injections)) {
            max_rounds++;
            test = new DefaultTestCase();
            // Choose a random length in 0 - size
            double length = Randomness.nextInt(chromosomeLength);
            while (length < 2) {
                length = Randomness.nextInt(chromosomeLength);
            }
            double prob = 1/length;
            boolean isIncluded = false;
            while (test.size() < length) {

                // If all public parents have been attempted,
                // reset the set of parents to start over injecting them.
                if (callableMethods.size() == 0) {
                    this.reset();
                }

                GenericAccessibleObject<?> call = null;
                boolean injecting = false;
                while(call == null){
                    if (Randomness.nextDouble() <= prob) {
                        call = Randomness.choice(callableMethods);
                        callableMethods.remove(call);
                        attemptedMethods.add(call);
                        injecting = true;
                    }else {
                        call = Randomness.choice(allMethods);
                    }
                }
                try {
                    TestFactory testFactory = TestFactory.getInstance();
                    if (call.isMethod()) {
                        testFactory.addMethod(test, (GenericMethod) call, test.size(), 0);
                    } else if (call.isConstructor()) {
                        testFactory.addConstructor(test, (GenericConstructor) call,
                                test.size(), 0);
                    }

                    //at this point, if injecting, then we successfully injected a target call.
                    if (injecting){
                        target_counter++;
                        prob = 1/length;
                    }
                } catch (ConstructionFailedException | Error e) {
                    if (injecting) {
                        prob = 1 / (length - test.size() + 1);
                    }
                }
            }
        }

        if (target_counter < 1 && max_rounds >= max_injections){
            LOG.error("Guided initialization failed. Please revise the target class and method!");
            throw new IllegalStateException("Guided initialization failed. Please revise the target class and method!");
        }

        if (tracerEnabled) {
            ExecutionTracer.enable();
        }

        return test;
    }

    private void reset() {
        if(callableMethods.isEmpty() && attemptedMethods.isEmpty()){
            return;
        }
        callableMethods.addAll(attemptedMethods);
        attemptedMethods.clear();
    }
}
