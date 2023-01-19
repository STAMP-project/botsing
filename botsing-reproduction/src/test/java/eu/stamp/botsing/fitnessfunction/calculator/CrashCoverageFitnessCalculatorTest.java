package eu.stamp.botsing.fitnessfunction.calculator;

import eu.stamp.botsing.StackTrace;
import org.evosuite.coverage.ControlFlowDistance;
import org.evosuite.coverage.branch.BranchCoverageGoal;
import org.evosuite.coverage.branch.BranchCoverageTestFitness;
import org.evosuite.testcase.DefaultTestCase;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testcase.execution.ExecutionTrace;
import org.evosuite.testcase.execution.ExecutionTraceImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.evosuite.shaded.org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.evosuite.shaded.org.mockito.ArgumentMatchers.any;
import static org.evosuite.shaded.org.mockito.ArgumentMatchers.anyString;

public class CrashCoverageFitnessCalculatorTest {

    private static final Logger LOG = LoggerFactory.getLogger(CrashCoverageFitnessCalculatorTest.class);


    @Rule
    public TestRule watcher = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            LOG.info(String.format("Starting test: %s()...",
                    description.getMethodName()));
        }
    };

    private CrashCoverageFitnessCalculator calculator;


    @Before
    public void setupCalculator() throws FileNotFoundException {

        BufferedReader obj = new BufferedReader(new StringReader("java.lang.IllegalArgumentException:\n" +
                "\tat eu.stamp.ClassA.method2(ClassA.java:10)\n" +
                "\tat eu.stamp.ClassB.method1(ClassB.java:20)"));

        StackTrace target = Mockito.spy(new StackTrace());
        Mockito.doReturn(obj).when(target).readFromFile(anyString());
        target.setup("", 2);

        calculator = new CrashCoverageFitnessCalculator(target);
    }

    @Test
    public void testCalculateFrameSimilarity_zeroDistance() throws IOException {
        BufferedReader obj = new BufferedReader(new StringReader("java.lang.IllegalArgumentException:\n" +
                "\tat eu.stamp.ClassA.method2(ClassA.java:10)\n" +
                "\tat eu.stamp.ClassB.method1(ClassB.java:20)"));

        StackTrace target = Mockito.spy(new StackTrace());
        Mockito.doReturn(obj).when(target).readFromFile(anyString());
        target.setup("", 2);

        StackTraceElement[] trace = new StackTraceElement[2];
        trace[0] = new StackTraceElement("eu.stamp.ClassA", "method2", "ClassA", 10);
        trace[1] = new StackTraceElement("eu.stamp.ClassB", "method1", "ClassB", 20);

        CrashCoverageFitnessCalculator calculator = new CrashCoverageFitnessCalculator(target);
        double distance = calculator.calculateFrameSimilarity(trace, target);
        assertEquals(0, distance, 0.000001);
    }

    @Test
    public void testCalculateFrameSimilarity_nonZeroDistance() throws IOException {
        BufferedReader obj = new BufferedReader(new StringReader("java.lang.IllegalArgumentException:\n" +
                "\tat eu.stamp.ClassA.method2(ClassA.java:10)\n" +
                "\tat eu.stamp.ClassB.method1(ClassB.java:20)"));

        StackTrace target = Mockito.spy(new StackTrace());
        Mockito.doReturn(obj).when(target).readFromFile(anyString());
        target.setup("", 2);

        StackTraceElement[] trace = new StackTraceElement[2];
        trace[0] = new StackTraceElement("eu.stamp.ClassA", "method2", "ClassA", 10);
        trace[1] = new StackTraceElement("eu.stamp.ClassB", "method1", "ClassB", 21);

        double distance = calculator.calculateFrameSimilarity(trace, target);
        assertEquals(0.25, distance, 0.000001);
    }

    @Test
    public void testCalculateFrameSimilarity_containsExtraRows() throws IOException {
        BufferedReader obj = new BufferedReader(new StringReader("java.lang.IllegalArgumentException:\n" +
                "\tat eu.stamp.ClassA.method2(ClassA.java:10)\n" +
                "\tat eu.stamp.ClassB.method1(ClassB.java:20)" +
                "\tat eu.stamp.ClassB.invoke(ClassB.java:20)" +
                "\tat eu.stamp.ClassB.reflect(ClassB.java:20)" +
                "\tat eu.stamp.evosuite.Evosuite(Evosuite.java:20)"));

        StackTrace target = Mockito.spy(new StackTrace());
        Mockito.doReturn(obj).when(target).readFromFile(anyString());
        target.setup("", 2);
        CrashCoverageFitnessCalculator calculator = new CrashCoverageFitnessCalculator(target);
        StackTraceElement[] trace = new StackTraceElement[2];
        trace[0] = new StackTraceElement("eu.stamp.ClassA", "method2", "ClassA", 10);
        trace[1] = new StackTraceElement("eu.stamp.ClassB", "method1", "ClassB", 20);

        double distance = calculator.calculateFrameSimilarity(trace, target);
        assertEquals(0.0, distance, 0.000001);
    }

    @Test
    public void testGetFrameDistance_zeroDistance() {
        StackTraceElement generated = new StackTraceElement("eu.stamp.ClassA", "method2", "ClassA", 10);
        StackTraceElement target = new StackTraceElement("eu.stamp.ClassA", "method2", "ClassA", 10);

        double distance = calculator.getFrameDistance(generated, target);
        assertEquals(0.0, distance, 0.000001);
    }

    @Test
    public void testGetFrameDistance_differentClass() {
        StackTraceElement generated = new StackTraceElement("eu.stamp.ClassA", "method2", "ClassA", 10);
        StackTraceElement target = new StackTraceElement("eu.stamp.ClassB", "method2", "ClassA", 10);

        double distance = calculator.getFrameDistance(generated, target);
        assertEquals(0.75, distance, 0.000001);
    }

    @Test
    public void testGetFrameDistance_differentMethod() {
        StackTraceElement generated = new StackTraceElement("eu.stamp.ClassA", "method2", "ClassA", 10);
        StackTraceElement target = new StackTraceElement("eu.stamp.ClassA", "method1", "ClassA", 10);

        double distance = calculator.getFrameDistance(generated, target);
        assertEquals(2.0 / 3.0, distance, 0.000001);
    }

    @Test(expected = java.lang.IllegalArgumentException.class)
    public void testNormalize() {
        StackTraceElement generated = new StackTraceElement("eu.stamp.ClassA", "method2", "ClassA", Integer.MAX_VALUE);
        StackTraceElement target = new StackTraceElement("eu.stamp.ClassA", "method2", "ClassA", -1);

        double distance = calculator.getFrameDistance(generated, target);
        assertEquals(1.0 / 2.0, distance, 0.000001);
    }

    @Test
    public void testGetLineCoverageFitness_coveredLine() throws IOException {
        // setting the target stack trace
        BufferedReader obj = new BufferedReader(new StringReader("java.lang.IllegalArgumentException:\n" +
                "\tat eu.stamp.ClassA.method2(ClassA.java:10)\n" +
                "\tat eu.stamp.ClassB.method1(ClassB.java:20)"));

        StackTrace target = Mockito.spy(new StackTrace());
        Mockito.doReturn(obj).when(target).readFromFile(anyString());
        target.setup("", 2);

        // setting the coverage data
        Map<Integer, Integer> lineCoverage = new HashMap<>();
        lineCoverage.put(10, 1);
        Map<String, Map<Integer, Integer>> methodCoverage = new HashMap<>();
        methodCoverage.put("method2", lineCoverage);

        ExecutionTrace executionTrace = new ExecutionTraceImpl();
        Map<String, Map<String, Map<Integer, Integer>>> classCoverage = executionTrace.getCoverageData();
        classCoverage.put("eu.stamp.ClassB", methodCoverage);

        // setting the execution results of the test case
        ExecutionResult result = new ExecutionResult(new DefaultTestCase());
        result.setTrace(executionTrace);

        // compute fitness function
        calculator.setTargetCrash(target);
        double fitness = calculator.getLineCoverageFitness(result, 10);
        assertEquals(0, fitness, 0.0001);
    }

    @Test
    public void testGetLineCoverageFitness_unCoveredLine() throws IOException {
        // setting the target stack trace
        BufferedReader obj = new BufferedReader(new StringReader("java.lang.IllegalArgumentException:\n" +
                "\tat eu.stamp.ClassA.method2(ClassA.java:10)\n" +
                "\tat eu.stamp.ClassB.method1(ClassB.java:20)"));

        StackTrace target = Mockito.spy(new StackTrace());
        Mockito.doReturn(obj).when(target).readFromFile(anyString());
        target.setup("", 2);

        // setting the coverage data
        Map<Integer, Integer> lineCoverage = new HashMap<>();
        lineCoverage.put(10, 1);
        Map<String, Map<Integer, Integer>> methodCoverage = new HashMap<>();
        methodCoverage.put("method2", lineCoverage);

        ExecutionTrace executionTrace = new ExecutionTraceImpl();
        Map<String, Map<String, Map<Integer, Integer>>> classCoverage = executionTrace.getCoverageData();
        classCoverage.put("eu.stamp.ClassB", methodCoverage);

        // setting the execution results of the test case
        ExecutionResult result = new ExecutionResult(new DefaultTestCase());
        result.setTrace(executionTrace);

        // compute fitness function
        calculator.setTargetCrash(target);
        double fitness = calculator.getLineCoverageFitness(result, 11);
        assertEquals(Double.MAX_VALUE, fitness, 0.0001);
    }

    @Test
    public void testComputeBranchDistance() {
        // setting the coverage data
        Map<Integer, Integer> lineCoverage = new HashMap<>();
        lineCoverage.put(10, 1);
        Map<String, Map<Integer, Integer>> methodCoverage = new HashMap<>();
        methodCoverage.put("method2", lineCoverage);

        ExecutionTrace executionTrace = new ExecutionTraceImpl();
        Map<String, Map<String, Map<Integer, Integer>>> classCoverage = executionTrace.getCoverageData();
        classCoverage.put("eu.stamp.ClassB", methodCoverage);

        // setting the execution results of the test case
        ExecutionResult result = new ExecutionResult(new DefaultTestCase());
        result.setTrace(executionTrace);

        // setting the branch distance
        ControlFlowDistance distance = new ControlFlowDistance();
        distance.setApproachLevel(0);
        distance.setBranchDistance(1);
        BranchCoverageGoal goal = Mockito.mock(BranchCoverageGoal.class);
        Mockito.doReturn(distance).when(goal).getDistance(any());
        BranchCoverageTestFitness fitness = new BranchCoverageTestFitness(goal);

        double fitnessValue = calculator.computeBranchDistance(fitness, result);
        assertEquals(1d / 3d, fitnessValue, 0.0001);
    }

    @Test
    public void testComputeBranchDistance_zeroDistance() {
        // setting the coverage data
        Map<Integer, Integer> lineCoverage = new HashMap<>();
        lineCoverage.put(10, 1);
        Map<String, Map<Integer, Integer>> methodCoverage = new HashMap<>();
        methodCoverage.put("method2", lineCoverage);

        ExecutionTrace executionTrace = new ExecutionTraceImpl();
        Map<String, Map<String, Map<Integer, Integer>>> classCoverage = executionTrace.getCoverageData();
        classCoverage.put("eu.stamp.ClassB", methodCoverage);

        // setting the execution results of the test case
        ExecutionResult result = new ExecutionResult(new DefaultTestCase());
        result.setTrace(executionTrace);

        // setting the branch distance
        ControlFlowDistance distance = new ControlFlowDistance();
        distance.setApproachLevel(0);
        distance.setBranchDistance(0);
        BranchCoverageGoal goal = Mockito.mock(BranchCoverageGoal.class);
        Mockito.doReturn(distance).when(goal).getDistance(any(ExecutionResult.class));
        BranchCoverageTestFitness fitness = new BranchCoverageTestFitness(goal);

        double fitnessValue = calculator.computeBranchDistance(fitness, result);
        assertEquals(0.5, fitnessValue, 0.0001);
    }
}