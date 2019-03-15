package eu.stamp.botsing.model.generation.testusage;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class TestUsagePool {
    private static final Logger LOG = LoggerFactory.getLogger(TestUsagePool.class);
    protected Map<String, List<String>> pool = new HashMap<String, List<String>>();

    public void addTest(String usedClass,String testName){
        LOG.info("Adding test {} to class {}",testName,usedClass);
        if(!this.pool.containsKey(usedClass)){
            this.pool.put(usedClass,new ArrayList<>());
        }

        if(!this.pool.get(usedClass).contains(testName)){
            this.pool.get(usedClass).add(testName);
        }
    }
}
