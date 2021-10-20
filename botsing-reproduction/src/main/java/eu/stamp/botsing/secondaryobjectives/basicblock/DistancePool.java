package eu.stamp.botsing.secondaryobjectives.basicblock;

import org.evosuite.graphs.cfg.BasicBlock;

import java.util.HashMap;

public class DistancePool {
    private static DistancePool instance = null;

    private HashMap<BasicBlock, HashMap<BasicBlock, Integer>> distancePool;
    private DistancePool(){
        distancePool = new HashMap<>();
    }

    public static DistancePool getInstance(){
        if (instance == null){
            instance = new DistancePool();
        }
        return instance;
    }

    public boolean distanceExists(BasicBlock source, BasicBlock dest){
        if(distancePool.containsKey(source) &&
                distancePool.get(source).containsKey(dest)){
            return true;
        }

        return false;
    }

    public Integer getDistance(BasicBlock source, BasicBlock dest){
        if(distanceExists(source,dest)){
            return distancePool.get(source).get(dest);
        }
        return null;
    }

    public void addDistance(BasicBlock source, BasicBlock dest, Integer distance){
        if(! distancePool.containsKey(source) ){
            distancePool.put(source,new HashMap<>());
        }

        if(! distancePool.get(source).containsKey(dest) ){
            distancePool.get(source).put(dest,distance);
        }
    }
}
