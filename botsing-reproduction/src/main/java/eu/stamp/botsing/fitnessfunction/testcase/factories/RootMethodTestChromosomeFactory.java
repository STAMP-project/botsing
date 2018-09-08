package eu.stamp.botsing.fitnessfunction.testcase.factories;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.fitnessfunction.WeightedSum;
import org.evosuite.Properties;
import org.evosuite.ga.ConstructionFailedException;
import org.evosuite.runtime.System;
import org.evosuite.setup.TestCluster;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFactory;
import org.evosuite.testcase.execution.ExecutionTracer;
import org.evosuite.testcase.factories.AllMethodsTestChromosomeFactory;
import org.evosuite.utils.LoggingUtils;
import org.evosuite.utils.Randomness;
import org.evosuite.utils.generic.GenericAccessibleObject;
import org.evosuite.utils.generic.GenericConstructor;
import org.evosuite.utils.generic.GenericMethod;

import java.util.*;

public class RootMethodTestChromosomeFactory extends AllMethodsTestChromosomeFactory {

    private static Set<GenericAccessibleObject<?>> publicParentCalls = new HashSet<GenericAccessibleObject<?>>();
    private static Set<GenericAccessibleObject<?>> attemptedPublicParents = new HashSet<GenericAccessibleObject<?>>();

    private static List<GenericAccessibleObject<?>> allMethods = new LinkedList<GenericAccessibleObject<?>>();

    public RootMethodTestChromosomeFactory(){
        allMethods.clear();
        allMethods.addAll(TestCluster.getInstance().getTestCalls());
        Randomness.shuffle(allMethods);
        reset();
    }

    @Override
    public TestChromosome getChromosome() {
        TestChromosome c = new TestChromosome();
        try {
            c.setTestCase(getRandomTestCase(CrashProperties.getIntValue("chromosome_length")));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (Properties.NoSuchParameterException e) {
            e.printStackTrace();
        }
        return c;
    }

    private TestCase getRandomTestCase(int size) {
        boolean tracerEnabled = ExecutionTracer.isEnabled();
        if (tracerEnabled)
            ExecutionTracer.disable();

        // Counts the number of injected target calls in the created test.
        int target_counter = 0;
        int max_rounds = 0;
        TestCase test = null;
        // Loop until the created method has at least one target call or it reaches the maximum number of rounds.
        while (target_counter < 1 && (max_rounds < CrashProperties.max_target_injection_tries)) {
            max_rounds++;
            test = getNewTestCase();
            // Choose a random length in 0 - size
            double length = Randomness.nextInt(size);
            while (length < 2)
                length = Randomness.nextInt(size);

            double prob = 1/length;
            boolean isIncluded = false;
            while (test.size() < length) {

                // If all public parents have been attempted,
                // reset the set of parents to start over injecting them.
                if (publicParentCalls.size() == 0) {
                    reset();
                }

                GenericAccessibleObject<?> call;
                boolean injecting = false;
                if (Randomness.nextDouble() <= prob) {
                    call = Randomness.choice(publicParentCalls);
                    publicParentCalls.remove(call);
                    attemptedPublicParents.add(call);
                    injecting = true;
                }else {
                    call = Randomness.choice(allMethods);
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
                        isIncluded = true;
                        target_counter++;
                        prob = 1/length;
                    }
//					else {
//						assert (false) : "Found test call that is neither method nor constructor";
//					}
                } catch (ConstructionFailedException e) {
                    if (injecting)
                        prob = 1/(length-test.size()+1);
                }
            }

        } // a test case is created which has at least 1 target call.

        if (target_counter < 1 && max_rounds >= CrashProperties.max_target_injection_tries){
            LoggingUtils.getEvoLogger().error("Guided initialization failed. Please revise the target class and method!");
            System.exit(0);
        }

        if (logger.isDebugEnabled())
            logger.debug("Randomized test case:" + test.toCode());

        if (tracerEnabled)
            ExecutionTracer.enable();

        return test;
    }

    public void reset(){
        fillPublicCalls();
    }

    private void fillPublicCalls(){
        Iterator<String> iterateParents = WeightedSum.publicCalls.iterator();

        // Fill up the set of parent calls by assessing the method names
        while (iterateParents.hasNext()) {
            String nextCall = iterateParents.next();
            for (int i=0; i<allMethods.size(); i++) {
                if (allMethods.get(i).getName().equals(nextCall)) {
                    publicParentCalls.add(allMethods.get(i));
                }
            }
        }
    }
}
