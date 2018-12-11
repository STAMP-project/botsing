package eu.stamp.botsing.model.generation.callsequence;

import java.io.Serializable;
import java.util.*;

public class CallSequencesPool implements Serializable {
    protected Map<String, Set<List<MethodCall>>> pool = new HashMap<String, Set<List<MethodCall>>>();

    public void addSequence(String clazz, List<MethodCall> sequences) {
        if (!pool.containsKey(clazz)) {
            pool.put(clazz, new HashSet<List<MethodCall>>());
        }

        pool.get(clazz).add(sequences);
    }
}