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

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.fitnessfunction.calculator.CrashCoverageFitnessCalculator;
import eu.stamp.botsing.testgeneration.strategy.BotsingIndividualStrategy;
import org.evosuite.coverage.exception.ExceptionCoverageHelper;

import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;


public class ExceptionDistance extends TestFitnessFunction {

    private static final Logger LOG = LoggerFactory.getLogger(BotsingIndividualStrategy.class);

    @Resource
    CrashCoverageFitnessCalculator fitnessCalculator;

    public ExceptionDistance(){
        fitnessCalculator =  new CrashCoverageFitnessCalculator();
    }


    @Override
    public double getFitness(TestChromosome testChromosome, ExecutionResult executionResult) {
        LOG.debug("Fitness calculation ... ");
        double fitnessValue = 1.0;

        for (Integer ExceptionLocator : executionResult.getPositionsWhereExceptionsWereThrown()) {
        	String thrownException = ExceptionCoverageHelper.getExceptionClass(executionResult, ExceptionLocator).getName();
            if (thrownException.equals(CrashProperties.getInstance().getStackTrace().getExceptionType())){
            	fitnessValue = 0.0;
                break;
            }
        }

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
        result = prime * result + ( CrashProperties.getInstance().getStackTrace().hashCode());
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
        return CrashProperties.getInstance().getStackTrace().getTargetClass();
    }

    @Override
    public String getTargetMethod() {
        return CrashProperties.getInstance().getStackTrace().getTargetMethod();
    }


}
