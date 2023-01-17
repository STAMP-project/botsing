package eu.stamp.botsing.fitnessfunction.testcase.factories;

/*-
 * #%L
 * botsing-reproduction
 * %%
 * Copyright (C) 2017 - 2018 eu.stamp-project
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.StackTrace;
import eu.stamp.botsing.commons.testgeneration.TestGenerationContextUtility;
import eu.stamp.botsing.ga.strategy.operators.GuidedSearchUtility;
import org.evosuite.Properties;
import org.evosuite.ga.ConstructionFailedException;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.rmi.ClientServices;
import org.evosuite.rmi.service.ClientNodeLocal;
import org.evosuite.setup.TestClusterUtils;
import org.evosuite.statistics.RuntimeVariable;
import org.evosuite.testcarver.extraction.CarvingManager;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFactory;
import org.evosuite.testcase.execution.ExecutionTracer;
import org.evosuite.testcase.factories.AllMethodsTestChromosomeFactory;
import org.evosuite.utils.Randomness;
import org.evosuite.utils.generic.GenericAccessibleObject;
import org.evosuite.utils.generic.GenericConstructor;
import org.evosuite.utils.generic.GenericMethod;
import org.evosuite.shaded.org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

public class StackTraceChromosomeFactory extends AllMethodsTestChromosomeFactory {
    private static final Logger LOG = LoggerFactory.getLogger(StackTraceChromosomeFactory.class);
    private GuidedSearchUtility utility;

    private static Set<GenericAccessibleObject<?>> publicParentCalls = new HashSet<GenericAccessibleObject<?>>();
    private static Set<GenericAccessibleObject<?>> attemptedPublicParents = new HashSet<GenericAccessibleObject<?>>();

    private static List<GenericAccessibleObject<?>> allMethods = new LinkedList<GenericAccessibleObject<?>>();

    StackTrace targetTrace ;

    public StackTraceChromosomeFactory(StackTrace trace, GuidedSearchUtility utility) {
        allMethods.clear();
        publicParentCalls.clear();
        attemptedPublicParents.clear();
        this.utility = utility;
        targetTrace = trace;

        fillPublicCalls();
        Randomness.shuffle(allMethods);


    }

    @Override
    public TestChromosome getChromosome() {
        TestChromosome chromosome = new TestChromosome();
        if(Properties.CARVE_OBJECT_POOL && Properties.SELECTED_JUNIT != null){
            CarvingManager manager = CarvingManager.getInstance();
            final Class<?> targetClass = Properties.getTargetClassAndDontInitialise();
            List<TestCase> junitTests = manager.getTestsForClass(targetClass);
            ClientNodeLocal client = ClientServices.getInstance().getClientNode();
            client.trackOutputVariable(RuntimeVariable.CarvedTests, junitTests.size());
            client.trackOutputVariable(RuntimeVariable.CarvedCoverage,0.0);

            final int N_mutations = Properties.SEED_MUTATIONS;
            final double P_clone = Properties.SEED_CLONE;
            double r = Randomness.nextDouble();

            if (junitTests.size() > 0 && r <= P_clone){
                LOG.info("Cloning user test");
                TestCase test = Randomness.choice(junitTests);
                chromosome.setTestCase(test.clone());
                if (N_mutations > 0) {
                    int numMutations = Randomness.nextInt(N_mutations);
                    logger.debug("Mutations: " + numMutations);
                    // doing the mutations on the cloned test case
                    for (int i = 0; i < numMutations; i++) {
                        chromosome.mutate();
                    }
                }
                return chromosome;
            }
        }
        try {
            chromosome.setTestCase(getRandomTestCase(CrashProperties.getInstance().getIntValue("chromosome_length")));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (Properties.NoSuchParameterException e) {
            e.printStackTrace();
        }
        return chromosome;
    }

    private TestCase getRandomTestCase(int size) {
        boolean tracerEnabled = ExecutionTracer.isEnabled();
        if (tracerEnabled) {
            ExecutionTracer.disable();
        }

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
            while (length < 2) {
                length = Randomness.nextInt(size);
            }
            double prob = 1/length;
            boolean isIncluded = false;
            while (test.size() < length) {

                // If all public parents have been attempted,
                // reset the set of parents to start over injecting them.
                if (publicParentCalls.size() == 0) {
                    this.reset();
                }

                GenericAccessibleObject<?> call = null;
                boolean injecting = false;
                while(call == null){
                if (Randomness.nextDouble() <= prob) {
                    call = Randomness.choice(publicParentCalls);
                    publicParentCalls.remove(call);
                    attemptedPublicParents.add(call);
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

        } // a test case is created which has at least 1 target call.

        if (target_counter < 1 && max_rounds >= CrashProperties.max_target_injection_tries){
            LOG.error("Guided initialization failed. Please revise the target class and method!");
            throw new IllegalStateException("Guided initialization failed. Please revise the target class and method!");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Randomized test case:" + test.toCode());
        }

        if (tracerEnabled) {
            ExecutionTracer.enable();
        }

        return test;
    }

    public void reset(){
        if(publicParentCalls.isEmpty() && attemptedPublicParents.isEmpty()){
            return;
        }
        publicParentCalls.addAll(attemptedPublicParents);
        attemptedPublicParents.clear();
    }

    private void fillPublicCalls(){

        BytecodeInstruction publicCallInStackTrace =  utility.collectPublicCalls(targetTrace);
        Class targetClass = null;
        try {
            targetClass = Class.forName(publicCallInStackTrace.getClassName(),false,TestGenerationContextUtility.getTestGenerationContextClassLoader(false));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        Set<Method> methods = TestClusterUtils.getMethods(targetClass);
        for(Method method: methods){
            GenericAccessibleObject methodObj = new GenericMethod(method,targetClass);
            allMethods.add(methodObj);
            String methodName = method.getName()+Type.getMethodDescriptor(method);
            if(methodName.equals(publicCallInStackTrace.getMethodName())){
                publicParentCalls.add(methodObj);
            }
        }

        Set<Constructor<?>> constructors = TestClusterUtils.getConstructors(targetClass);
        for (Constructor constructor : constructors){
            GenericAccessibleObject constructorObj = new GenericConstructor(constructor,targetClass);
            allMethods.add(constructorObj);
            String constructorName = "<init>"+Type.getConstructorDescriptor(constructor);
            if(constructorName.equals(publicCallInStackTrace.getMethodName())){
                publicParentCalls.add(constructorObj);
            }
        }
    }

    public Set<GenericAccessibleObject<?>> getPublicCalls(){
        Set<GenericAccessibleObject<?>> result = new HashSet<GenericAccessibleObject<?>>();
        result.addAll(this.publicParentCalls);
        result.addAll(this.attemptedPublicParents);

        return result;
    }

    public StackTrace getTargetTrace() {
        return targetTrace;
    }
}
