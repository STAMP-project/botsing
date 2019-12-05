package eu.stamp.cbc.extraction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CoverageDataPool {
    // Main pool <test, <coveredClass, <CoveredMethod, <LineNumber, #ofCoverage>>>>
    private Map<String, Map<String, Map<String, Map<Integer, Integer>>>> pool = new HashMap<>();

    private static CoverageDataPool instance;


    private CoverageDataPool(){
        pool.clear();
    }

    public static CoverageDataPool getInstance() {
        if(instance == null){
            instance = new CoverageDataPool();
        }
        return instance;
    }


    public void registerNewCoverageData(String testName, Map<String, Map<String, Map<Integer, Integer>>> coverageData){
        if(this.pool.containsKey(testName)){
            throw new IllegalStateException("Coverage data for test"+testName+"is already available.");
        }

        this.pool.put(testName,coverageData);
    }


    public Set<String> getExecutedTests(){
        return this.pool.keySet();
    }


    public Map<String, Map<String, Map<Integer, Integer>>> getCoverageData(String testName){
        if(!this.pool.containsKey(testName)){
            throw new IllegalArgumentException("Coverage data of test "+testName+" is not available!");
        }

        return this.pool.get(testName);
    }


}
