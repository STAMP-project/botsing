package eu.stamp.botsing.fitnessfunction;

import eu.stamp.botsing.StackTrace;
import org.evosuite.testcase.execution.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ITFFForIndexedAccess extends IntegrationTestingFF {
    private static final Logger logger = LoggerFactory.getLogger(ITFFForIndexedAccess.class);

    public ITFFForIndexedAccess(StackTrace crash) {
        super(crash);
    }

    @Override
    protected double exceptionCoverage(ExecutionResult executionResult) {
        if (super.exceptionCoverage(executionResult) == 0) {
            return 0;
        }
        Map<Integer, int[]> arrayAccessInfo = executionResult.getTrace().getIndexedAccessInfo();
        if (arrayAccessInfo.isEmpty()) {
            return 1;
        }
        double exceptionCoverage = 0;
        for (Map.Entry<Integer, int[]> entry : arrayAccessInfo.entrySet()) {
            int[] indexAndLength = entry.getValue();
            exceptionCoverage += distance(indexAndLength[0], indexAndLength[1]);
        }
        return exceptionCoverage / arrayAccessInfo.size();
    }

    /**
     * Calculate the distance of a specific index being out of bounds given the length of the array.
     *
     * @param length the length of the array.
     *
     * @return If the index is negative or greater or equal to the length of the array, it is out of bounds and 0 will
     * be returned. Otherwise, return the ratio of index-to-bounds over mid-to-bounds.
     */
    private static double distance(int index, int length) {
        if (index < 0 || index >= length) {
            return 0;
        } else {
            double mid = (length - 1.0) / 2.0;
            return 1 - Math.abs(mid - index) / (length - mid);
        }
    }
}
