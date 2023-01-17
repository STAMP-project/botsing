package eu.stamp.botsing.fitnessfunction;

import eu.stamp.botsing.StackTrace;
import org.evosuite.testcase.execution.ExecutionResult;
import org.junit.Assert;
import org.junit.Test;
import org.evosuite.shaded.org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyString;

public class IntegrationTestingFFTest {

    @Test
    public void testExceptionCoverage_executionResultWithoutTargetCrash() throws FileNotFoundException {
        ExecutionResult executionResult = Mockito.mock(ExecutionResult.class);
        Set<Integer> exceptionLocators = new HashSet<>();
        exceptionLocators.add(1);
        Mockito.doReturn(exceptionLocators).when(executionResult).getPositionsWhereExceptionsWereThrown();

        Throwable resultException = Mockito.mock(Throwable.class);
        Mockito.doReturn(new StackTraceElement[0]).when(resultException).getStackTrace();
        Mockito.doReturn(resultException).when(executionResult).getExceptionThrownAtPosition(1);
        IntegrationTestingFF integrationTestingFF = new IntegrationTestingFF(null);
        double exceptionCoverage = integrationTestingFF.exceptionCoverage(executionResult);
        assert (exceptionCoverage == 1);
    }

    @Test
    public void testExceptionCoverage_executionResultWithDifferentDeepestClass() throws FileNotFoundException {
        ExecutionResult executionResult = Mockito.mock(ExecutionResult.class);
        Set<Integer> exceptionLocators = new HashSet<>();
        exceptionLocators.add(1);
        Mockito.doReturn(exceptionLocators).when(executionResult).getPositionsWhereExceptionsWereThrown();

        Throwable resultException = Mockito.mock(Throwable.class);

        StackTraceElement[] trace = new StackTraceElement[2];
        trace[0] = new StackTraceElement("eu.stamp.ClassA", "method2", "ClassA", 10);
        trace[1] = new StackTraceElement("eu.stamp.ClassB", "method1", "ClassB", 20);

        Mockito.doReturn(trace).when(resultException).getStackTrace();
        Mockito.doReturn(resultException).when(executionResult).getExceptionThrownAtPosition(1);

        BufferedReader obj = new BufferedReader(new StringReader("java.lang.IllegalArgumentException:\n" +
                "\tat eu.stamp.ClassC.method2(ClassA.java:10)\n" +
                "\tat eu.stamp.ClassB.method1(ClassB.java:20)"));

        StackTrace target = Mockito.spy(new StackTrace());
        Mockito.doReturn(obj).when(target).readFromFile(anyString());
        target.setup("", 2);

        IntegrationTestingFF integrationTestingFF = new IntegrationTestingFF(target);
        double exceptionCoverage = integrationTestingFF.exceptionCoverage(executionResult);

        assert (exceptionCoverage == 1);
    }




    @Test
    public void testExceptionCoverage_executionResultWithSimilarExceptions() throws FileNotFoundException {
        ExecutionResult executionResult = Mockito.mock(ExecutionResult.class);
        Set<Integer> exceptionLocators = new HashSet<>();
        exceptionLocators.add(1);
        Mockito.doReturn(exceptionLocators).when(executionResult).getPositionsWhereExceptionsWereThrown();

        Throwable resultException = new IllegalArgumentException();
        StackTraceElement[] trace = new StackTraceElement[2];
        trace[0] = new StackTraceElement("eu.stamp.ClassA", "method2", "ClassA", 10);
        trace[1] = new StackTraceElement("eu.stamp.ClassB", "method1", "ClassB", 20);
        resultException.setStackTrace(trace);


        Mockito.doReturn(resultException).when(executionResult).getExceptionThrownAtPosition(1);

        BufferedReader obj = new BufferedReader(new StringReader("java.lang.IllegalArgumentException:\n" +
                "\tat eu.stamp.ClassA.method2(ClassA.java:10)\n" +
                "\tat eu.stamp.ClassB.method1(ClassB.java:20)"));

        StackTrace target = Mockito.spy(new StackTrace());
        Mockito.doReturn(obj).when(target).readFromFile(anyString());
        Mockito.doReturn("java.lang.IllegalArgumentException").when(target).getExceptionType();
        target.setup("", 2);

        IntegrationTestingFF integrationTestingFF = new IntegrationTestingFF(target);
        double exceptionCoverage = integrationTestingFF.exceptionCoverage(executionResult);

        assert (exceptionCoverage == 0);
    }

    @Test
    public void testEqual_false(){
        IntegrationTestingFF integrationTestingFF = new IntegrationTestingFF(null);
        Assert.assertFalse(integrationTestingFF.equals(null));
    }


    @Test
    public void testEqual_own(){
        IntegrationTestingFF integrationTestingFF = new IntegrationTestingFF(null);
        Assert.assertTrue(integrationTestingFF.equals(integrationTestingFF));
    }


    @Test
    public void testEqual_sameObject(){
        IntegrationTestingFF integrationTestingFF = new IntegrationTestingFF(null);
        IntegrationTestingFF integrationTestingFF2 = new IntegrationTestingFF(null);
        Assert.assertTrue(integrationTestingFF.equals(integrationTestingFF2));
    }
}
