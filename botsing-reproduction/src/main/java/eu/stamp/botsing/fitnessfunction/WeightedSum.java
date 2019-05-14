package eu.stamp.botsing.fitnessfunction;

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

import eu.stamp.botsing.StackTrace;
import eu.stamp.botsing.fitnessfunction.calculator.CrashCoverageFitnessCalculator;
import eu.stamp.botsing.testgeneration.strategy.BotsingIndividualStrategy;
import org.evosuite.coverage.exception.ExceptionCoverageHelper;

import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;


public class WeightedSum extends TestFitnessFunction {

    private static final Logger LOG = LoggerFactory.getLogger(BotsingIndividualStrategy.class);

    @Resource
    CrashCoverageFitnessCalculator fitnessCalculator;
    StackTrace targetCrash;
//    private static CrashCoverageFitnessCalculator calculator = new CrashCoverageFitnessCalculator();

    public WeightedSum(StackTrace crash){
        fitnessCalculator = new CrashCoverageFitnessCalculator(crash);
        targetCrash = crash;
    }

    public void setFitnessCalculator(CrashCoverageFitnessCalculator fitnessCalculator) {
        this.fitnessCalculator = fitnessCalculator;
    }

    @Override
    public double getFitness(TestChromosome testChromosome, ExecutionResult executionResult) {
        LOG.debug("Fitness calculation ... ");
        double exceptionCoverage = 1.0;
        double frameSimilarity = 1.0;
        // Priority 1) Line coverage
        double LineCoverageFitness = fitnessCalculator.getLineCoverageFitness(  executionResult, targetCrash.getTargetLine());

        if(LineCoverageFitness == 0.0){
            //Priority 2) Exception coverage
            for (Integer ExceptionLocator : executionResult.getPositionsWhereExceptionsWereThrown()) {
                String thrownException = ExceptionCoverageHelper.getExceptionClass(executionResult, ExceptionLocator).getName();
                exceptionCoverage = 1;
                frameSimilarity = 1;
                if (thrownException.equals(targetCrash.getExceptionType())){
                    exceptionCoverage = 0.0;
                    // Priority 3) Frame similarity
                    double tempFitness = fitnessCalculator.calculateFrameSimilarity( executionResult.getExceptionThrownAtPosition(ExceptionLocator).getStackTrace());
                    if (tempFitness == 0.0){
                        frameSimilarity = 0.0;
                        break;
                    }else if (tempFitness<frameSimilarity){
                        frameSimilarity = tempFitness;
                    }
                }
            }
        }
        double fitnessValue = 3 * LineCoverageFitness  + 2 * exceptionCoverage + frameSimilarity;
        LOG.debug("Fitness Function: "+fitnessValue);
        testChromosome.setFitness(this,fitnessValue);
        testChromosome.increaseNumberOfEvaluations();
        return fitnessValue;
    }

    @Override
    public int compareTo(TestFitnessFunction testFitnessFunction) {
        // TODO Add this when we have multple fitness functions
        return 0;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( targetCrash.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        return getClass() == obj.getClass();
    }

    @Override
    public String getTargetClass() {
        return targetCrash.getTargetClass();
    }

    @Override
    public String getTargetMethod() {
        return targetCrash.getTargetMethod();
    }


}
