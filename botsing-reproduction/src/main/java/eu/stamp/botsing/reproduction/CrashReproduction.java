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
import static eu.stamp.botsing.commons.PostProcessUtility.*;
import static eu.stamp.botsing.commons.SetupUtility.analyzeClassDependencies;
import static eu.stamp.botsing.commons.SetupUtility.configureClassReInitializer;
import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.commons.instrumentation.ClassInstrumentation;
import eu.stamp.botsing.graphs.cfg.CFGGenerator;
import org.evosuite.Properties;
import org.evosuite.coverage.TestFitnessFactory;
import org.evosuite.result.TestGenerationResult;
import org.evosuite.result.TestGenerationResultBuilder;
import org.evosuite.rmi.ClientServices;
import org.evosuite.rmi.service.ClientState;
import org.evosuite.runtime.LoopCounter;
import org.evosuite.runtime.sandbox.Sandbox;
import org.evosuite.strategy.TestGenerationStrategy;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.TestCaseExecutor;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CrashReproduction {
    private static final Logger LOG = LoggerFactory.getLogger(CrashReproduction.class);


    public static List<TestGenerationResult> execute(){
        CrashProperties crashProperties = CrashProperties.getInstance();
        List<TestGenerationResult> generatedTests = new ArrayList<TestGenerationResult>();


        //Before starting search, activate the sandbox
        if (crashProperties.getBooleanValue("sandbox")) {
            Sandbox.initializeSecurityManagerForSUT();
        }


        generatedTests.add(generateCrashReproductionTests());


        // Resetting sandbox after test generation
        if (crashProperties.getBooleanValue("sandbox")) {
            Sandbox.resetDefaultSecurityManager();
        }


        return generatedTests;
    }


    private static TestGenerationResult generateCrashReproductionTests(){
//        List<TestGenerationResult> result= new ArrayList<>();
        ClientServices.getInstance().getClientNode().changeState(ClientState.INITIALIZATION);

        // Deactivate loop counter to make sure classes initialize properly
        LoopCounter.getInstance().setActive(false);

        //Initialize EvoSuite test executor
        TestCaseExecutor.initExecutor();

        // In the first step initialize the target class
        try{
            // TODO: make a factroy for class(es) initialization
            if(CrashProperties.integrationTesting){
                CFGGenerator cfgGenerator = new CFGGenerator();
                cfgGenerator.generateInterProceduralCFG();
                analyzeClassDependencies(CrashProperties.getInstance().getStackTrace(0).getTargetClass());
            }else{
                initializeTargetClass(0);
            }
        }catch (Exception e){
            LOG.error("Error in target initialization:");
            e.printStackTrace();
        }finally {
            if (CrashProperties.getInstance().getBooleanValue("reset_static_fields")) {
                configureClassReInitializer();
            }
            LoopCounter.getInstance().setActive(true);
        }

        // For seeding we should initializing the pool here


        if (!Properties.hasTargetClassBeenLoaded() && !CrashProperties.integrationTesting) {
            // initialization failed, then build error message
            return TestGenerationResultBuilder.buildErrorResult("Could not load target class");
        }


        TestGenerationStrategy strategy = CrashReproductionHelper.getTestGenerationFactory();
        TestSuiteChromosome testCases = strategy.generateTests();

        postProcessTests(testCases,getFitnessFactories());

        TestGenerationResult writingTest = writeJUnitTestsAndCreateResult(testCases, Properties.JUNIT_SUFFIX);
        writeJUnitFailingTests();
        TestCaseExecutor.pullDown();

        LOG.info("The solution test is saved!");

        return writingTest;

    }

//    private static void analyzeClassPaths() throws ClassNotFoundException{
//        String cp = ClassPathHandler.getInstance().getTargetProjectClasspath();
//        List<String> cpList = Arrays.asList(cp.split(File.pathSeparator));
//        LOG.info("Starting the dependency analysis. The number of detected jar files is {}.",cpList.size());
//        DependencyAnalysis.analyzeClass(CrashProperties.getInstance().getStackTrace().getTargetClass(),Arrays.asList(cp.split(File.pathSeparator)));
//        LOG.info("Analysing dependencies done!");
//    }



    private static List<TestFitnessFactory<? extends TestFitnessFunction>> getFitnessFactories() {
        List<TestFitnessFactory<? extends TestFitnessFunction>> goalsFactory = new ArrayList<TestFitnessFactory<? extends TestFitnessFunction>>();
        goalsFactory.add(new CrashReproductionGoalFactory());
        return goalsFactory;
    }



    private static void initializeTargetClass(int crashIndex) throws ClassNotFoundException {
        // Instrument the single target class
        ClassInstrumentation.instrumentClassByTestExecution(CrashProperties.getInstance().getStackTrace(crashIndex).getTargetClass());

        // Analyze dependencies
        analyzeClassDependencies(CrashProperties.getInstance().getStackTrace(crashIndex).getTargetClass());
    }

}
