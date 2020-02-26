package eu.stamp.botsing.commons.testgeneration.strategy;


import eu.stamp.botsing.commons.fitnessfunction.FitnessFunctions;
import eu.stamp.botsing.commons.ga.strategy.mosa.AbstractMOSA;
import eu.stamp.botsing.ga.stoppingconditions.SingleObjectiveZeroStoppingCondition;
import org.evosuite.Properties;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.ga.stoppingconditions.GlobalTimeStoppingCondition;
import org.evosuite.ga.stoppingconditions.MaxTimeStoppingCondition;
import org.evosuite.ga.stoppingconditions.StoppingCondition;
import org.evosuite.ga.stoppingconditions.ZeroFitnessStoppingCondition;
import org.evosuite.strategy.TestGenerationStrategy;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionTracer;
import org.evosuite.testsuite.RelativeSuiteLengthBloatControl;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.utils.ResourceController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;

public class MOSuiteStrategy extends TestGenerationStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(MOSuiteStrategy.class);

    private AbstractTestGenerationUtility utility;
    private FitnessFunctions fitnessFunctionCollector;

    public MOSuiteStrategy(AbstractTestGenerationUtility utility,FitnessFunctions fitnessFunctionCollector){
        this.utility = utility;
        this.fitnessFunctionCollector = fitnessFunctionCollector;
    }
    @Override
    public TestSuiteChromosome generateTests() {
        LOG.info("test generation strategy: MOSuite");
        TestSuiteChromosome suite;

        ExecutionTracer.enableTraceCalls();

        // Get the search algorithm
        GeneticAlgorithm<TestSuiteChromosome> ga = utility.getGA();
        if(!ga.getStoppingConditions().isEmpty()){
            Iterator it = ga.getStoppingConditions().iterator();
            while (it.hasNext()){
                ga.removeStoppingCondition((StoppingCondition) it.next());
            }
        }


        if(!(ga instanceof AbstractMOSA)){
            throw new IllegalArgumentException("The search algorithm of MOSuite should be MOSA");
        }

        // Add stopping conditions
        StoppingCondition stoppingCondition = getStoppingCondition();

        stoppingCondition.setLimit(Properties.SEARCH_BUDGET);

        List<TestFitnessFunction> fitnessFunctions = fitnessFunctionCollector.getFitnessFunctionList();
        boolean containsMainFF = false;
        if (fitnessFunctions.size() > 1) {
            for (TestFitnessFunction ff : fitnessFunctions) {
                String ffClassName = ff.getClass().getName();
                if (ffClassName.equals("eu.stamp.botsing.fitnessfunction.WeightedSum") ||
                        ffClassName.equals("eu.stamp.botsing.fitnessfunction.IntegrationTestingFF") ||
                        ffClassName.equals("eu.stamp.botsing.fitnessfunction.ITFFForIndexedAccess")) {
                    containsMainFF = true;
                    ga.addStoppingCondition(new SingleObjectiveZeroStoppingCondition(ff));
                }
            }
            if (!containsMainFF) {
                ga.addStoppingCondition(new ZeroFitnessStoppingCondition());
            }
        }
        if (Properties.STOP_ZERO) {
            ga.addStoppingCondition(new ZeroFitnessStoppingCondition());
        }

        if (!(stoppingCondition instanceof MaxTimeStoppingCondition)) {
            ga.addStoppingCondition(new GlobalTimeStoppingCondition());
        }else{
            ga.addStoppingCondition(stoppingCondition);
        }

        if (Properties.CHECK_BEST_LENGTH) {
            RelativeSuiteLengthBloatControl bloat_control = new org.evosuite.testsuite.RelativeSuiteLengthBloatControl();
            ga.addBloatControl(bloat_control);
            ga.addListener(bloat_control);
        }
        ga.addListener(new ResourceController());

        ga.resetStoppingConditions();

        // Add listener

        if (Properties.SHOW_PROGRESS){
            ga.addListener(progressMonitor);
        }


        // Add fitnes functions
        LOG.info("The number of goals are {}: ",fitnessFunctions.size());

        ga.addFitnessFunctions((List)fitnessFunctions);

        // Start the search process
        ga.generateSolution();

        List<TestSuiteChromosome> bestSuites = ga.getBestIndividuals();
        if (bestSuites.isEmpty()) {
            LOG.warn("Could not find any suitable chromosome");
            return new TestSuiteChromosome();
        }else{
            suite = bestSuites.get(0);
        }

        return suite;
    }
}
