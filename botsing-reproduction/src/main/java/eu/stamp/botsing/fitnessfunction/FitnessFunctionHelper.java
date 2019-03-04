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
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.testcase.TestFitnessFunction;

public class FitnessFunctionHelper {

    public boolean isConstructor(BytecodeInstruction targetInstruction){
        String methodName = targetInstruction.getMethodName();
        methodName = methodName.substring(0, methodName.indexOf('('));
        String classPath = targetInstruction.getClassName();
        int lastOccurrence = classPath.lastIndexOf(".");
        if (lastOccurrence == -1){
            return false;
        }
        String className = classPath.substring(lastOccurrence+1);
        return className.equals(methodName);

    }

    public TestFitnessFunction getSingleObjective(int index){
        if(CrashProperties.fitnessFunctions.length < index){
            return null;
        }
        switch (CrashProperties.fitnessFunctions[index]){
            case WeightedSum:
                return new WeightedSum();
            case LineDistance:
            	return new LineDistance();
            case ExceptionDistance:
            	return new ExceptionDistance();
            case TraceDistance:
            	return new TraceDistance();
            default:
                return new WeightedSum();
        }
    }
    public TestFitnessFunction[] getMultiObjectives(){
        TestFitnessFunction[] result =  new TestFitnessFunction[CrashProperties.fitnessFunctions.length];
        for (int i = 0; i<CrashProperties.fitnessFunctions.length;i++){
            result[i]= getSingleObjective(i);
        }
        return result;
    }
}
