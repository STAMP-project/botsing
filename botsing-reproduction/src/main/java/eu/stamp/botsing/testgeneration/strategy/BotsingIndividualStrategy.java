package eu.stamp.botsing.testgeneration.strategy;

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
import eu.stamp.botsing.fitnessfunction.FitnessFunctions;
import eu.stamp.botsing.ga.stoppingconditions.SingleObjectiveZeroStoppingCondition;
import eu.stamp.botsing.seeding.ModelSeedingHelper;
import org.evosuite.Properties;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.ga.stoppingconditions.GlobalTimeStoppingCondition;
import org.evosuite.ga.stoppingconditions.MaxTimeStoppingCondition;
import org.evosuite.ga.stoppingconditions.StoppingCondition;
import org.evosuite.ga.stoppingconditions.ZeroFitnessStoppingCondition;
import org.evosuite.seeding.ObjectPool;
import org.evosuite.seeding.ObjectPoolManager;
import org.evosuite.strategy.TestGenerationStrategy;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionTracer;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.utils.ResourceController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;

public class BotsingIndividualStrategy extends TestGenerationStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(BotsingIndividualStrategy.class);

    private TestGenerationUtility utility = new TestGenerationUtility();

    private FitnessFunctions fitnessFunctionCollector = new FitnessFunctions();


    @Override
    public TestSuiteChromosome generateTests() {
        LOG.info("test generation strategy: Botsing individual");
        TestSuiteChromosome suite = new TestSuiteChromosome();

        ExecutionTracer.enableTraceCalls();

        // Get the search algorithm
        GeneticAlgorithm ga = utility.getGA();

        // Add stopping conditions
        StoppingCondition stoppingCondition = getStoppingCondition();
        try {
            stoppingCondition.setLimit(CrashProperties.getInstance().getLongValue("search_budget"));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (Properties.NoSuchParameterException e) {
            e.printStackTrace();
        }



        ga.addStoppingCondition(stoppingCondition);


        // Detect fitnes function(s)
        List<TestFitnessFunction> fitnessFunctions = fitnessFunctionCollector.getFitnessFunctionList();
        boolean containsMainFF = false;
        if(fitnessFunctions.size() > 1){
            // if we have multiple objectives, we should check the zero fitness value stopping condition
            for (TestFitnessFunction ff :fitnessFunctions){
                // if it has one of the main crash reproduction fitness functions, we onlu check the zero value of that
                String ffClassName = ff.getClass().getName();
                if (ffClassName.equals("eu.stamp.botsing.fitnessfunction.WeightedSum")|| ffClassName.equals("eu.stamp.botsing.fitnessfunction.IntegrationTestingFF")){
                    containsMainFF = true;
                    ga.addStoppingCondition(new SingleObjectiveZeroStoppingCondition(ff));
                }
            }
            if(! containsMainFF){
                ga.addStoppingCondition(new ZeroFitnessStoppingCondition());
            }
        }else if(CrashProperties.getInstance().fitnessFunctions.length == 1){
            ga.addStoppingCondition(new ZeroFitnessStoppingCondition());
        }else{
            throw new IllegalStateException("Lis of Fitness Functions is empty");
        }

        if (!(stoppingCondition instanceof MaxTimeStoppingCondition)) {
            ga.addStoppingCondition(new GlobalTimeStoppingCondition());
        }

        // Add listeners

        if (Properties.CHECK_BEST_LENGTH) {
            org.evosuite.testcase.RelativeTestLengthBloatControl bloat_control = new org.evosuite.testcase.RelativeTestLengthBloatControl();
            ga.addBloatControl(bloat_control);
            ga.addListener(bloat_control);
        }
        ga.addListener(new ResourceController());



        // Add fitness functions
        ga.addFitnessFunctions(fitnessFunctions);

        // prepare model seeding before generating the solution
        if(Properties.MODEL_PATH != null){
            ModelSeedingHelper modelSeedingHelper = new ModelSeedingHelper(Properties.MODEL_PATH);
            ObjectPool pool = modelSeedingHelper.generatePool();
            ObjectPoolManager.getInstance().addPool(pool);
            Properties.ALLOW_OBJECT_POOL_USAGE=true;
        }

        // Start the search process
        ga.generateSolution();
        double bestFF= Double.MAX_VALUE;
        if (containsMainFF){
            Iterator<StoppingCondition> itr = ga.getStoppingConditions().iterator();
            while (itr.hasNext()){
                StoppingCondition condition = itr.next();
                if(condition instanceof SingleObjectiveZeroStoppingCondition){
                    bestFF = condition.getCurrentValue();
                    break;
                }
            }
        }else{
            bestFF =ga.getBestIndividual().getFitness();
        }

        if (bestFF== 0.0) {
            TestChromosome solution = (TestChromosome) ga.getBestIndividual();
            LOG.info("* The target crash is covered. The generated test is: "+solution.getTestCase().toCode());
            LOG.info("{} thrown exception(s) are detected in the solution: ",solution.getLastExecutionResult().getAllThrownExceptions().size());
            for(Throwable t: solution.getLastExecutionResult().getAllThrownExceptions()){
                LOG.info(t.toString());
                for(StackTraceElement frame:t.getStackTrace()){
                    LOG.info(frame.toString());
                }

            }
            suite.addTest(solution);


        }else{
            LOG.info("* The target crash is not covered! The best solution has "+ga.getBestIndividual().getFitness()+" fitness value.");
            LOG.info("The best test is:(non-minimized version:\n)",((TestChromosome) ga.getBestIndividual()).toString());
        }

        // after finishing the search check: ga.getBestIndividual().getFitness() == 0.0
        // add best individual to final result and return result (TestSuiteChromosome)


        return suite;
    }



}
