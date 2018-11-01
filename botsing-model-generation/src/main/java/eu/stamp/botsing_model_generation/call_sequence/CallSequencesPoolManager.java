package eu.stamp.botsing_model_generation.call_sequence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
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
            e.printStackTrace();
        }
    }

    public void readPoolFromTheFile(String fileName){
        try {
            String json = readFile(fileName);
            Gson gson = new GsonBuilder().create();
            Type listType = new TypeToken<HashMap<String, Set<List<MethodCall>>>>(){}.getType();
            this.reWritePool(gson.fromJson(json,listType));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private String readFile(String fileName) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }
            return sb.toString();
        } finally {
            br.close();
        }
    }
}
