package eu.stamp.botsing.fitnessfunction;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

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
    public void testExceptionCoverage_executionResultWithoutTargetCrash() {
        assertEquals(expectedDistance, ITFFForIndexedAccess.distance(index, arrayLength), 0.00000001);
    }
}