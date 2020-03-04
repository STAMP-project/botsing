package eu.stamp.cbc.extraction;

import org.evosuite.testcase.execution.ExecutionTrace;

import java.util.*;

public class ExecutionTracePool {
    // Main pool <test, ExecutionTrace>
    private Map<String, ExecutionTrace> pool = new HashMap<>();

    private static ExecutionTracePool instance;


    private ExecutionTracePool(){
        pool.clear();
    }

    public static ExecutionTracePool getInstance() {
        if(instance == null){
            instance = new ExecutionTracePool();
        }
        return instance;
    }


    public void registerNewCoverageData(String testName, ExecutionTrace executionTrace){
        if(this.pool.containsKey(testName)){
            throw new IllegalStateException("Coverage data for test"+testName+"is already available.");
        }

        this.pool.put(testName,executionTrace);
    }


    public Set<String> getExecutedTests(){
        return this.pool.keySet();
    }


    public ExecutionTrace getExecutionTrace(String testName){
        if(!this.pool.containsKey(testName)){
            throw new IllegalArgumentException("Coverage data of test "+testName+" is not available!");
        }

        return this.pool.get(testName);
    }

    public Collection<ExecutionTrace> getExecutionTraces(){
        return this.pool.values();
    }

    public void clear(){
        pool.clear();
    }


}
