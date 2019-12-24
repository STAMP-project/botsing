package eu.stamp.botsing;

/*-
 * #%L
 * botsing-reproduction
 * %%
 * Copyright (C) 2017 - 2018 eu.stamp-project
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.evosuite.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

public class StackTrace {

    private static final Logger LOG = LoggerFactory.getLogger(StackTrace.class);

    private String exceptionType;
    private int targetFrameLevel;

    /**
     * All frames that are in the original log file.
     */
    private ArrayList<StackTraceElement> allFrames;

    /**
     * Frames that will be reproduced. That is the frames above the target level.
     */
    private ArrayList<StackTraceElement> frames;
    private String targetClass;
    private ArrayList<Integer> irrelevantFrames = new ArrayList<>();
    private int publicTargetFrameLevel;

    /**
     * Sets up this object with the stack trace read from the given file and having the given target frame.
     *
     * @param logPath    The file with a stack trace.
     * @param frameLevel The target frame level.
     * @throws IllegalArgumentException If the file at logPath could not be found or does not contain a valid stack trace.
     */
    public void setup(String logPath, int frameLevel) throws IllegalArgumentException {
        targetFrameLevel = frameLevel;
        publicTargetFrameLevel = frameLevel;
        try {
            BufferedReader reader = readFromFile(logPath);
            // Parse type of the exception
            StringTokenizer st = new StringTokenizer(reader.readLine(), ":");
            exceptionType = st.nextToken();
            LOG.info("Exception type is detected: " + exceptionType);

            // clear the frames in this.frames (if any)
            if (frames == null) {
                frames = new ArrayList<>();
                allFrames = new ArrayList<>();
            } else {
                frames.clear();
                allFrames.clear();
            }

            // Parse frames
            for (int counter = 0; counter < frameLevel; counter++) {
                String tempFrame = reader.readLine();
                if (tempFrame == null) {
                    break;
                }

                frames.add(stringToStackTraceElement(tempFrame,counter,true));
                allFrames.add(stringToStackTraceElement(tempFrame,counter,false));
            }
            String tempFrame;
            int counter = frameLevel;
            while((tempFrame=reader.readLine())!=null && tempFrame.length()!=0 && tempFrame.contains("at")){
                allFrames.add(stringToStackTraceElement(tempFrame,counter,true));
                counter++;
            }
            LOG.info("Target frame is set to: " + frames.get(frameLevel - 1).toString());

            // Parse Target class
            targetClass = frames.get(frameLevel - 1).getClassName();
            Properties.TARGET_CLASS = targetClass;
            LOG.info("Target Class is set to: " + targetClass);

//             If the target exception is ArrayIndexOutOfBoundsException
            if (exceptionType.equals(ArrayIndexOutOfBoundsException.class.getName()) || exceptionType.equals(StringIndexOutOfBoundsException.class.getName())) {
                // Set the line number where the array access call is located at.
                Properties.TARGET_INDEXED_ACCESS_LINE = frames.get(0).getLineNumber();
            }
        } catch (FileNotFoundException e) {
            LOG.debug("Stack trace file not found!", e);
            throw new IllegalArgumentException("Stack trace file not found!", e);
        } catch (IOException e) {
            LOG.debug("Unable to read file {}!", logPath, e);
            throw new IllegalArgumentException("Unable to read file "+ logPath + "!", e);
        }
    }

    private StackTraceElement stringToStackTraceElement(String frameString, int counter, boolean report) {
        int startPoint = frameString.indexOf("at ") + 3;
        String usefulPart = frameString.substring(startPoint);
        int splitPoint = usefulPart.indexOf("(");
        String usefulForLineDetection = usefulPart.substring(splitPoint);
        String usefulForOtherParts = usefulPart.substring(0, splitPoint);

        //Line detection
        int lineFirstSplitpoint = usefulForLineDetection.indexOf(":") + 1;
        int lineSecondSplitpoint = usefulForLineDetection.indexOf(")");
        int lineNumber = Integer.MIN_VALUE;
        try{
            lineNumber = Integer.parseInt(usefulForLineDetection.substring(lineFirstSplitpoint, lineSecondSplitpoint));
        }catch (NumberFormatException e){
            if(report){
                LOG.warn("Missing line in frame {}",counter+1);
            }

        }


        String[] split = usefulForOtherParts.split("\\.");
        // method Detection
        String methodName = split[(split.length - 1)];

        // class detection
        String clazz = String.join(".", Arrays.copyOfRange(split, 0, split.length - 1));
        return new StackTraceElement(clazz, methodName, split[(split.length - 2)], lineNumber);
    }


    public int getTargetFrameLevel() {
        return targetFrameLevel;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public String getTargetClass() {
        return targetClass;
    }

    public StackTraceElement getFrame(int index) {
        if(targetFrameLevel == publicTargetFrameLevel){
            return frames.get(index - 1);
        }else{
            return allFrames.get(index-1);
        }

    }

    public int getNumberOfFrames() {
        return frames.size();
    }

    public String getTargetMethod() {
        return frames.get(targetFrameLevel - 1).getMethodName();
    }

    public int getTargetLine() {
        return frames.get(targetFrameLevel - 1).getLineNumber();
    }

    public ArrayList<StackTraceElement> getFrames() {
        return frames;
    }

    public ArrayList<StackTraceElement> getAllFrames() {
        return allFrames;
    }

    public BufferedReader readFromFile(String filePath) throws FileNotFoundException {
        File file = new File(filePath);
        BufferedReader br = new BufferedReader(new FileReader(file));
        return br;
    }

    public List<String> getTargetClasses() {
        List<String> classes = new ArrayList<>();
        for (StackTraceElement frame: frames) {
            classes.add(frame.getClassName());
            LOG.debug(frame.getClassName());
        }
        return classes;
    }

    public void addIrrelevantFrameLevel(int frameLevel){
        if (frameLevel > this.targetFrameLevel) {
            throw new IllegalArgumentException("The passed irrelevant frame (frame level "+frameLevel+") is higher than the target frame level.");
        }
        if (!this.irrelevantFrames.contains(frameLevel)) {
            this.irrelevantFrames.add(frameLevel);
        }
    }


    public boolean isIrrelevantFrame(int frameLevel) {
        return this.irrelevantFrames.contains(frameLevel);
    }

    public void updatePublicTargetFrameLevel(int newLevel) {
        publicTargetFrameLevel = newLevel;
    }

    public int getPublicTargetFrameLevel() {
        return publicTargetFrameLevel;
    }
}
