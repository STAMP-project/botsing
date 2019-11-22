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
import eu.stamp.botsing.StackTrace;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.testcase.TestFitnessFunction;

public class FitnessFunctionHelper {

    public boolean isConstructor(BytecodeInstruction targetInstruction) {
        String methodName = targetInstruction.getMethodName();
        methodName = methodName.substring(0, methodName.indexOf('('));
        String classPath = targetInstruction.getClassName();
        int lastOccurrence = classPath.lastIndexOf(".");
        if (lastOccurrence == -1) {
            return false;
        }
        String className = classPath.substring(lastOccurrence + 1);
        return className.equals(methodName);
    }

    public TestFitnessFunction getSingleObjective() {
        return getFF(CrashProperties.fitnessFunctions[0], CrashProperties.getInstance().getStackTrace(0));
    }

    public TestFitnessFunction[] getMultiObjectives() {
        if (CrashProperties.fitnessFunctions.length > 1) {
            // Here, we have 1 crash and multiple fitness functions
            TestFitnessFunction[] result = new TestFitnessFunction[CrashProperties.fitnessFunctions.length];
            StackTrace singleCrash = CrashProperties.getInstance().getStackTrace(0);
            for (int i = 0; i < CrashProperties.fitnessFunctions.length; i++) {
                result[i] = getFF(CrashProperties.fitnessFunctions[i], singleCrash);
            }
            return result;
        } else if (CrashProperties.getInstance().getCrashesSize() > 1) {
            // Here, we have multiple crashes and 1 fitness function
            int numberOfCrashes = CrashProperties.getInstance().getCrashesSize();
            TestFitnessFunction[] result = new TestFitnessFunction[numberOfCrashes];
            for (int i = 0; i < numberOfCrashes; i++) {
                result[i] = getFF(CrashProperties.fitnessFunctions[0], CrashProperties.getInstance().getStackTrace(i));
            }
            return result;
        } else {
            throw new IllegalStateException("Number of crashes and fitness functions are 1. Botsing cannot use a " +
                    "multi-objective algorithm");
        }
    }

    private TestFitnessFunction getFF(CrashProperties.FitnessFunction givenFFName, StackTrace crash) {
        switch (givenFFName) {
            case WeightedSum:
                return new WeightedSum(crash);
            case IntegrationSingleObjective:
                return new IntegrationTestingFF(crash);
            case IntegrationIndexedAccess:
                return new ITFFForIndexedAccess(crash);
            case TestLen:
                return new TestLenFF();
            default:
                return new WeightedSum(crash);
        }
    }


    public static double normalize(double value) throws IllegalArgumentException {
        if (value < 0d) {
            throw new IllegalArgumentException("Values to normalize cannot be negative");
        }
        if (Double.isInfinite(value)) {
            return 1.0;
        }
        return value / (1.0 + value);
    }
}
