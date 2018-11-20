package eu.stamp.botsing.model.generation.callsequence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

public class CallSequencesPoolManager extends CallSequencesPool {
    private static final Logger LOG = LoggerFactory.getLogger(CallSequencesPoolManager.class);

    private static CallSequencesPoolManager instance = null;
    private CallSequencesPoolManager() {}


    public static CallSequencesPoolManager getInstance() {
        if(instance == null) {
            instance = new CallSequencesPoolManager();
        }
        return instance;
    }



    public void report(){
        for (Map.Entry<String, Set<List<MethodCall>>> entry : this.pool.entrySet()) {
            String clazz = entry.getKey();
            Set<List<MethodCall>> callSequences = entry.getValue();
            LOG.info("^^^^^^^^^^^^^^^^^");
            LOG.info("Exported call sequences for class "+ clazz);
            LOG.info("Number of call sequences "+ callSequences.size());
            int counter = 1;
            for (List<MethodCall> callSequence:callSequences){
                LOG.info("=================");
                LOG.info("call sequences #"+ counter);
                for(MethodCall methodCall: callSequence){
                    LOG.info(methodCall.getMethodName()+" - "+java.util.Arrays.toString(methodCall.getParams()));
                }
                counter++;
            }
        }
    }


    public void savePool(String outputPath){
        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(this.pool,pool.getClass());

        try (PrintWriter out = new PrintWriter(outputPath)) {
            out.println(json);
        } catch (FileNotFoundException e) {
            LOG.error("The output directory is not valid.");
        }
    }


    public Map<String, Set<List<MethodCall>>> getPool(){
        return this.pool;
    }


}
