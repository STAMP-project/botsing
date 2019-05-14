package eu.stamp.botsing.reproduction;

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
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testsuite.AbstractFitnessFactory;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CrashReproductionGoalFactory extends AbstractFitnessFactory<TestFitnessFunction> {

    @Resource
    FitnessFunctionHelper fitnessFunctionHelper;

    private static Map<String, TestFitnessFunction> goals = new LinkedHashMap<>();

    public CrashReproductionGoalFactory(){
        fitnessFunctionHelper = new FitnessFunctionHelper();
        goals.clear();
        if(CrashProperties.getInstance().getCrashesSize() > 1){
            // multi-objectives
            TestFitnessFunction[] fetchedGoals = fitnessFunctionHelper.getMultiObjectives();
            for(TestFitnessFunction goal: fetchedGoals){
                String key = getKey(goal);
                if (!goals.containsKey(key)) {
                    goals.put(key, goal);
                }
            }

        }else{
            // single Objective
            TestFitnessFunction goal = fitnessFunctionHelper.getSingleObjective();
            String key = getKey(goal);
            if (!goals.containsKey(key)) {
                goals.put(key, goal);
            }
        }
    }

    @Override
    public List<TestFitnessFunction> getCoverageGoals() {
        return  new ArrayList<TestFitnessFunction>(goals.values());
    }






    public String getKey(TestFitnessFunction goal){
        return goal.getTargetClass()+"_"+goal.getTargetMethod();

    }
}
