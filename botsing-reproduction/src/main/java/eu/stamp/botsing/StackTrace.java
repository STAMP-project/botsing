package eu.stamp.botsing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.StringTokenizer;

public class StackTrace {
    private String exceptionType;
    private ArrayList<StackTraceElement> frames =  new ArrayList<StackTraceElement>();
    private int target_frame_level;
    private String targetClass;

    private static StackTrace instance;

    public static StackTrace getInstance(){
        if (instance == null)
            instance = new StackTrace();
        return instance;
    }


    private StackTrace(){
    }


    public void setup(String logPath,int frame_level){
        target_frame_level =  frame_level;
        try {
            File logFile = new File(logPath);
            BufferedReader reader = new BufferedReader(new FileReader(logFile));

            // Parse type of the exception
            StringTokenizer st = new StringTokenizer(reader.readLine(), ":");
            exceptionType =  st.nextToken();
            System.out.println("Exception type is detected: "+exceptionType);

            // Parse frames
            for(int counter=0;counter<frame_level;counter++){
                String tempFrame = reader.readLine();
                if (tempFrame == null){
                    break;
                }
                frames.add(stringToStackTraceElement(tempFrame));
            }
            System.out.println("Target frame is set to: "+frames.get(frame_level-1).toString());

            // Parse Target class
            targetClass = frames.get(frame_level-1).getClassName();
            org.evosuite.Properties.TARGET_CLASS = targetClass;
            System.out.println("Target Class is set to: "+targetClass);

        } catch (Exception e){
            System.out.println("Unable to parse the stack trace:");
            e.printStackTrace();
        }
    }

    private StackTraceElement stringToStackTraceElement(String frameString){
        int startPoint = frameString.indexOf("at ")+3;
        String usefulPart = frameString.substring(startPoint);
        int splitPoint = usefulPart.indexOf("(");
        String usefulForLineDetection = usefulPart.substring(splitPoint);
        String usefulForOtherParts = usefulPart.substring(0,splitPoint);

        //Line detection
        int lineFirstSplitpoint = usefulForLineDetection.indexOf(":")+1;
        int lineSecondSplitpoint = usefulForLineDetection.indexOf(")");

        int lineNumber = Integer.parseInt(usefulForLineDetection.substring(lineFirstSplitpoint,lineSecondSplitpoint));

        String[] split = usefulForOtherParts.split("\\.");
        // method Detection
        String methodName =  split[(split.length-1)];

        // class detection
        String clazz = String.join(".",Arrays.copyOfRange(split,0,split.length-1));
        return new StackTraceElement(clazz, methodName, split[(split.length-2)], lineNumber);
    }


    public int getTarget_frame_level() {
        return target_frame_level;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public String getTargetClass() {
        return targetClass;
    }

    public StackTraceElement getFrame(int index){
        return frames.get(index-1);
    }
}
