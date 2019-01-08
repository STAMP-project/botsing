package eu.stamp.botsing.model.generation.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

public class LogReader {
    private static final Logger LOG = LoggerFactory.getLogger(LogReader.class);
    private ArrayList<String>  logs;
    private ArrayList<String>  involvedObjects = new ArrayList<>();
    public LogReader(ArrayList<String> logs){
        this.logs = logs;
    }


    public ArrayList<String> collectInvolvedObjects() {
        if(involvedObjects.size() == 0){
            parseObjects();
        }
        return involvedObjects;
    }

    private void parseObjects() {
        for(String log: logs){
            try {
                BufferedReader reader = readFromFile(log);
                reader.readLine();

                while(true) {
                    String tempFrame = reader.readLine();
                    if (tempFrame == null) {
                        break;
                    }
                    String detectedObj = fetchObject(tempFrame);
                    if(!involvedObjects.contains(detectedObj)){
                        involvedObjects.add(detectedObj);
                    }
                }

            }catch (FileNotFoundException e) {
                LOG.debug("Stack trace file not found!", e);
                throw new IllegalArgumentException("Stack trace file not found!", e);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String fetchObject(String tempFrame) {
        int startPoint = tempFrame.indexOf("at ") + 3;
        String usefulPart = tempFrame.substring(startPoint);
        int splitPoint = usefulPart.indexOf("(");
        String usefulForOtherParts = usefulPart.substring(0, splitPoint);
        String[] split = usefulForOtherParts.split("\\.");
        // class detection
        String clazz = String.join(".", Arrays.copyOfRange(split, 0, split.length - 1));
        return clazz;
    }


    private BufferedReader readFromFile(String filePath) throws FileNotFoundException {
        File file = new File(filePath);
        BufferedReader br = new BufferedReader(new FileReader(file));
        return br;
    }
}
