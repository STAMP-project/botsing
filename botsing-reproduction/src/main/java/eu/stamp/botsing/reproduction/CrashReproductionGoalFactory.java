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
import eu.stamp.botsing.fitnessfunction.WeightedSum;
import org.evosuite.testsuite.AbstractFitnessFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CrashReproductionGoalFactory extends AbstractFitnessFactory<WeightedSum> {

    private static Map<String, WeightedSum> goals = new LinkedHashMap<>();

    public CrashReproductionGoalFactory(){
        Throwable targetException = CrashProperties.getTargetException();
        WeightedSum goal = new WeightedSum(targetException);
        String key = goal.getKey();
        if (!goals.containsKey(key)) {
            goals.put(key, goal);
        }
    }

    @Override
    public List<WeightedSum> getCoverageGoals() {
        return  new ArrayList<WeightedSum>(goals.values());
    }
}
