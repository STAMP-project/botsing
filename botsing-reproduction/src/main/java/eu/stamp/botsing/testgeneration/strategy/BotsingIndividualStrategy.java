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
import eu.stamp.botsing.fitnessfunction.FitnessFunctionHelper;
import eu.stamp.botsing.fitnessfunction.testcase.factories.RootMethodTestChromosomeFactory;
import eu.stamp.botsing.ga.strategy.GuidedGeneticAlgorithm;
import eu.stamp.botsing.ga.strategy.GuidedNSGAII;
import eu.stamp.botsing.ga.strategy.operators.GuidedSearchUtility;
import eu.stamp.botsing.seeding.ModelSeedingHelper;
import org.evosuite.Properties;
import org.evosuite.ga.ChromosomeFactory;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Resource;

// This strategy selects one coverage goal. In the current version, this single goal is crash coverage
public class BotsingIndividualStrategy extends TestGenerationStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(BotsingIndividualStrategy.class);

    @Resource
    FitnessFunctionHelper fitnessFunctionHelper =  new FitnessFunctionHelper();

    @Override
    public TestSuiteChromosome generateTests() {
        LOG.info("test generation strategy: Botsing individual");
        LOG.info("The single goal is crash coverage.");
        TestSuiteChromosome suite = new TestSuiteChromosome();
        StoppingCondition stoppingCondition = getStoppingCondition();
        try {
            stoppingCondition.setLimit(CrashProperties.getInstance().getLongValue("search_budget"));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (Properties.NoSuchParameterException e) {
            e.printStackTrace();
        }
        ExecutionTracer.enableTraceCalls();
        GeneticAlgorithm ga = getGA();

        if (Properties.CHECK_BEST_LENGTH) {
            org.evosuite.testcase.RelativeTestLengthBloatControl bloat_control = new org.evosuite.testcase.RelativeTestLengthBloatControl();
            ga.addBloatControl(bloat_control);
            ga.addListener(bloat_control);
        }
        ga.addStoppingCondition(stoppingCondition);
        ga.addStoppingCondition(new ZeroFitnessStoppingCondition());

        if (!(stoppingCondition instanceof MaxTimeStoppingCondition)) {
            ga.addStoppingCondition(new GlobalTimeStoppingCondition());
        }

        if (ga instanceof GuidedNSGAII){
        	List<TestFitnessFunction> fitnessFunctions = getFFs();
        	ga.addFitnessFunctions(fitnessFunctions);
        } else {
        	TestFitnessFunction ff = getFF();
        	ga.addFitnessFunction(ff);
        }

        // prepare model seeding before generating the solution
        if(Properties.MODEL_PATH != null){
            ModelSeedingHelper modelSeedingHelper = new ModelSeedingHelper(Properties.MODEL_PATH);
            ObjectPool pool = modelSeedingHelper.generatePool();
            ObjectPoolManager.getInstance().addPool(pool);
            Properties.ALLOW_OBJECT_POOL_USAGE=true;
        }
        ga.generateSolution();



        if (ga.getBestIndividual().getFitness() == 0.0) {
            TestChromosome solution = (TestChromosome) ga.getBestIndividual();
            LOG.info("* The target crash is covered. The generated test is: "+solution.getTestCase().toCode());
            suite.addTest(solution);


        }else{
            LOG.info("* The target crash is not covered! The best solution has "+ga.getBestIndividual().getFitness()+" fitness value.");
            LOG.info("The best test is:(non-minimized version:\n)",((TestChromosome) ga.getBestIndividual()).toString());
        }

        // after finishing the search check: ga.getBestIndividual().getFitness() == 0.0
        // add best individual to final result and return result (TestSuiteChromosome)


        return suite;
    }

    private GeneticAlgorithm getGA(){
        switch (CrashProperties.searchAlgorithm){
            case Single_Objective_GGA:
                return new GuidedGeneticAlgorithm(getChromosomeFactory());
            case Multi_Objective_GGA:
            	return new GuidedNSGAII(getChromosomeFactory());
            default:
                return new GuidedGeneticAlgorithm(getChromosomeFactory());
        }
    }

    private ChromosomeFactory<TestChromosome> getChromosomeFactory() {
        return new RootMethodTestChromosomeFactory(new GuidedSearchUtility());
    }

    private TestFitnessFunction getFF(){
        return fitnessFunctionHelper.getSingleObjective(0);
    }

    private List<TestFitnessFunction> getFFs(){
    	return new ArrayList<>(Arrays.asList(fitnessFunctionHelper.getMultiObjectives()));
    }
}
