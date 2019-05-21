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
import eu.stamp.botsing.testgeneration.strategy.BotsingIndividualStrategy;
import eu.stamp.botsing.testgeneration.strategy.MOSuiteStrategy;
import org.evosuite.strategy.TestGenerationStrategy;

public class CrashReproductionHelper {

    public static TestGenerationStrategy getTestGenerationFactory(){
                switch (CrashProperties.searchAlgorithm){
                    case Guided_MOSA:
                        return new MOSuiteStrategy();
                    case DynaMOSA:
                        return new MOSuiteStrategy();
                    default:
                        return new BotsingIndividualStrategy();
                }
        }
    }
