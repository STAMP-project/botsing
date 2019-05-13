package eu.stamp.botsing.fitnessfunction;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.StackTrace;
import eu.stamp.botsing.fitnessfunction.calculator.CrashCoverageFitnessCalculator;
import org.evosuite.coverage.mutation.WeakMutationSuiteFitness;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.execution.ExecutionResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

@RunWith(MockitoJUnitRunner.class)
public class WeightedSumTest {

    private static final Logger LOG = LoggerFactory.getLogger(WeightedSumTest.class);

    @Rule
    public TestRule watcher = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            LOG.info(String.format("Starting test: %s()...",
                    description.getMethodName()));
        }
    };

    // Mock CrashCoverageFitnessCalculator to control the value of the line coverage fitness and stack trace similarity fitness
    @Mock
    CrashCoverageFitnessCalculator fitnessCalculator;
    @InjectMocks
    WeightedSum weightedSum = new WeightedSum();


    @Before
    public void resetCrashes(){
        CrashProperties.getInstance().clearStackTraceList();
    }

    @Test
    public void testGetFitness_LineNotCovered() throws FileNotFoundException {

        //Prepare the given stack trace
        BufferedReader givenStackTrace = new BufferedReader(new StringReader("java.lang.IllegalArgumentException:\n" +
                "\tat eu.stamp.ClassA.method2(ClassA.java:10)\n" +
                "\tat eu.stamp.ClassB.method1(ClassB.java:20)"));
        StackTrace target = Mockito.spy(new StackTrace());
        Mockito.doReturn(givenStackTrace).when(target).readFromFile(anyString());
        target.setup("", 2);
        CrashProperties.getInstance().setupStackTrace(target);

        // Initialize the TestChromosome object as an input argument
        TestChromosome testChromosomeAsInput = new TestChromosome();

        // Mock the value of getLineCoverageFitness
        Mockito.doReturn(0.5).when(fitnessCalculator).getLineCoverageFitness(0, null,20);

        assertEquals(4.5, weightedSum.getFitness(testChromosomeAsInput,null),0.0);
    }



    @Test
    public void testGetFitness_ExceptionNotCovered() throws FileNotFoundException {
        // Prepare the given stack trace
        BufferedReader givenStackTrace = new BufferedReader(new StringReader("java.lang.IllegalArgumentException:\n" +
                "\tat eu.stamp.ClassA.method2(ClassA.java:10)\n" +
                "\tat eu.stamp.ClassB.method1(ClassB.java:20)"));
        StackTrace target = Mockito.spy(new StackTrace());
        Mockito.doReturn(givenStackTrace).when(target).readFromFile(anyString());
        TestChromosome testChromosomeAsInput = new TestChromosome();
        target.setup("", 2);
        CrashProperties.getInstance().setupStackTrace(target);

        // Prepare the generated stack trace
        StackTraceElement[] tt = {new StackTraceElement("ClassC",  "method3",
                "ClassC", 100), new StackTraceElement("ClassB",  "method1",
                "ClassB", 20)};
        Throwable generatedStackTrace =  new NullPointerException();
        generatedStackTrace.setStackTrace(tt);

        // Mock executionResult which is an input argument for target method
        Set<Integer> locations = new HashSet<>();
        locations.add(100);
        ExecutionResult executionResult =  Mockito.mock(ExecutionResult.class);
        Mockito.when(executionResult.getExceptionThrownAtPosition(anyInt())).thenReturn(generatedStackTrace);
        Mockito.when(executionResult.getPositionsWhereExceptionsWereThrown()).thenReturn(locations);

        // Mock the value of getLineCoverageFitness to zero
        Mockito.doReturn(0.0).when(fitnessCalculator).getLineCoverageFitness(0, executionResult,20);

        assertEquals(3.0, weightedSum.getFitness(testChromosomeAsInput,executionResult),0.0);
    }



    @Test
    public void testGetFitness_StackTraceSimilarityNotCovered() throws FileNotFoundException {
        // Prepare the given stack trace
        BufferedReader givenStackTrace = new BufferedReader(new StringReader("java.lang.IllegalArgumentException:\n" +
                "\tat eu.stamp.ClassA.method2(ClassA.java:10)\n" +
                "\tat eu.stamp.ClassB.method1(ClassB.java:20)"));
        StackTrace target = Mockito.spy(new StackTrace());
        Mockito.doReturn(givenStackTrace).when(target).readFromFile(anyString());
        TestChromosome testChromosomeAsInput = new TestChromosome();
        target.setup("", 2);
        CrashProperties.getInstance().setupStackTrace(target);

        // Prepare the generated stack trace
        StackTraceElement[] tt = {new StackTraceElement("ClassC",  "method3",
                "ClassC", 100), new StackTraceElement("ClassB",  "method1",
                "ClassB", 20)};
        Throwable generatedStackTrace =  new IllegalArgumentException();
        generatedStackTrace.setStackTrace(tt);

        // Mock executionResult which is an input argument for target method
        Set<Integer> locations = new HashSet<>();
        locations.add(100);
        ExecutionResult executionResult =  Mockito.mock(ExecutionResult.class);
        Mockito.when(executionResult.getExceptionThrownAtPosition(anyInt())).thenReturn(generatedStackTrace);
        Mockito.when(executionResult.getPositionsWhereExceptionsWereThrown()).thenReturn(locations);

        // Mock the value of getLineCoverageFitness and calculateFrameSimilarity in CrashCoverageFitnessCalculator
        Mockito.doReturn(0.0).when(fitnessCalculator).getLineCoverageFitness(0, executionResult,20);
        Mockito.doReturn(0.7).when(fitnessCalculator).calculateFrameSimilarity(0, tt);

        assertEquals(0.7, weightedSum.getFitness(testChromosomeAsInput,executionResult),0.0);
    }


    @Test
    public void testGetFitness_Zero() throws FileNotFoundException {
        // Prepare the given stack trace
        BufferedReader givenStackTrace = new BufferedReader(new StringReader("java.lang.IllegalArgumentException:\n" +
                "\tat eu.stamp.ClassA.method2(ClassA.java:10)\n" +
                "\tat eu.stamp.ClassB.method1(ClassB.java:20)"));
        StackTrace target = Mockito.spy(new StackTrace());
        Mockito.doReturn(givenStackTrace).when(target).readFromFile(anyString());
        TestChromosome testChromosomeAsInput = new TestChromosome();
        target.setup("", 2);
        CrashProperties.getInstance().setupStackTrace(target);

        // Prepare the generated stack trace
        StackTraceElement[] tt = {new StackTraceElement("ClassC",  "method3",
                "ClassC", 100), new StackTraceElement("ClassB",  "method1",
                "ClassB", 20)};
        Throwable generatedStackTrace =  new IllegalArgumentException();
        generatedStackTrace.setStackTrace(tt);

        // Mock executionResult which is an input argument for target method
        Set<Integer> locations = new HashSet<>();
        locations.add(100);
        ExecutionResult executionResult =  Mockito.mock(ExecutionResult.class);
        Mockito.when(executionResult.getExceptionThrownAtPosition(anyInt())).thenReturn(generatedStackTrace);
        Mockito.when(executionResult.getPositionsWhereExceptionsWereThrown()).thenReturn(locations);


        // Mock the value of getLineCoverageFitness and calculateFrameSimilarity in CrashCoverageFitnessCalculator into the zero value
        Mockito.doReturn(0.0).when(fitnessCalculator).getLineCoverageFitness(0, executionResult,20);
        Mockito.doReturn(0.0).when(fitnessCalculator).calculateFrameSimilarity(0, tt);

        assertEquals(0.0, weightedSum.getFitness(testChromosomeAsInput,executionResult),0.0);
    }

    @Test
    public void testEqual(){
        assertFalse(weightedSum.equals(null));

        WeightedSum ws = new WeightedSum();
        assertTrue(weightedSum.equals(ws));

        WeakMutationSuiteFitness fitnessFunction = new WeakMutationSuiteFitness();
        assertFalse(weightedSum.equals(fitnessFunction));
    }
}
