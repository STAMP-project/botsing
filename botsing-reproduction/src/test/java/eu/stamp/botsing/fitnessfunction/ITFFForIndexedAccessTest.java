package eu.stamp.botsing.fitnessfunction;

import eu.stamp.botsing.StackTrace;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testcase.execution.ExecutionTraceImpl;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@RunWith(Parameterized.class)
public class ITFFForIndexedAccessTest extends IntegrationTestingFFTest {

    @Parameterized.Parameters(name = "index:{0}, length:{1}, expectedDistance:{2}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {-1, 1, 0},
                {2, 1, 0},
                {0, 2, (2.0 / 3.0)},
                {0, 3, 0.5},
                {1, 3, 1},
                {0, 4, 0.4},
                {1, 4, 0.8}
        });
    }

    @Parameterized.Parameter
    public int index;

    @Parameterized.Parameter(value = 1)
    public int arrayLength;

    @Parameterized.Parameter(value = 2)
    public double expectedDistance;

    @Override
    public void testExceptionCoverage_executionResultWithoutTargetCrash() throws FileNotFoundException {
        ExecutionResult executionResult = mock(ExecutionResult.class);
        HashSet<Integer> exceptionLocators = new HashSet<>();
        exceptionLocators.add(1);
        doReturn(exceptionLocators).when(executionResult).getPositionsWhereExceptionsWereThrown();

        ExecutionTraceImpl executionTrace = new ExecutionTraceImpl();
        executionTrace.indexAndLengthMap = new HashMap<>();
        executionTrace.indexAndLengthMap.put("eu.stamp.ClassB", new HashMap<>());
        executionTrace.indexAndLengthMap.get("eu.stamp.ClassB").put("method1", new HashMap<>());
        executionTrace.indexAndLengthMap.get("eu.stamp.ClassB").get("method1").put(0, new int[]{index, arrayLength});

        doReturn(executionTrace).when(executionResult).getTrace();

        Throwable resultException = Mockito.mock(Throwable.class);

        StackTraceElement[] trace = new StackTraceElement[2];
        trace[0] = new StackTraceElement("eu.stamp.ClassA", "method2", "ClassA", 10);
        trace[1] = new StackTraceElement("eu.stamp.ClassB", "method1", "ClassB", 20);

        Mockito.doReturn(trace).when(resultException).getStackTrace();
        Mockito.doReturn(resultException).when(executionResult).getExceptionThrownAtPosition(1);

        BufferedReader obj = new BufferedReader(new StringReader("java.lang.ArrayIndexOutOfBounds:\n\tat eu.stamp" +
                ".ClassC.method2(ClassA.java:10)\n\tat eu.stamp.ClassB.method1(ClassB.java:20)"));

        StackTrace target = Mockito.spy(new StackTrace());
        Mockito.doReturn(obj).when(target).readFromFile(anyString());
        target.setup("", 2);

        ITFFForIndexedAccess itffForIndexedAccess = new ITFFForIndexedAccess(target);
        Assert.assertEquals(expectedDistance, itffForIndexedAccess.exceptionCoverage(executionResult), 0.00000001);
    }
}