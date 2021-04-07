package eu.stamp.botsing.fitnessfunction.utils;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.StackTrace;
import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import eu.stamp.botsing.commons.testgeneration.TestGenerationContextUtility;
import org.evosuite.TestGenerationContext;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.BytecodeInstructionPool;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SpecialCallersPool {
    static SpecialCallersPool  instance;

    Set<StackTraceElement> pool = new HashSet<>();

    private SpecialCallersPool(){
    }


    public static SpecialCallersPool getInstance(){
        if(instance == null){
            instance = new SpecialCallersPool();
        }

        return instance;
    }

    public void detectSpecialCallers(StackTrace targetCrash){
        int stackTraceSize = targetCrash.getFrames().size();
        for (int index=stackTraceSize;index>0;index--){
            StackTraceElement currentFrame = targetCrash.getFrame(index);
            if (isFirstLineSpecialCaller(currentFrame,targetCrash,index)){
                pool.add(currentFrame);
            }
        }
    }


    private boolean isFirstLineSpecialCaller(StackTraceElement currentFrame, StackTrace targetCrash, int index){
        int nextFrameLevel = index-1;
        if (nextFrameLevel==0){
            return false;
        }

        boolean result = false;

        ClassLoader classLoader;
        if(CrashProperties.integrationTesting){
            classLoader = BotsingTestGenerationContext.getInstance().getClassLoaderForSUT();
        }else{
            classLoader = TestGenerationContext.getInstance().getClassLoaderForSUT();
        }

        StackTraceElement deeperFrame = targetCrash.getFrame(nextFrameLevel);
        String deeperClass = deeperFrame.getClassName().replace(".","/");
        String deeperMethod = deeperFrame.getMethodName();
        String deeperMethodInvokeInstruction = "INVOKESPECIAL "+deeperClass+"."+deeperMethod;
        String methodName = TestGenerationContextUtility.derivingMethodFromBytecode(CrashProperties.integrationTesting, currentFrame.getClassName(), currentFrame.getLineNumber());
        List<BytecodeInstruction> instructions = BytecodeInstructionPool.getInstance(classLoader).getInstructionsIn(currentFrame.getClassName(),methodName);
//        Detect first line
        int firstLine = Integer.MAX_VALUE;
        for(BytecodeInstruction instruction : instructions){
            int lineNumber = instruction.getLineNumber();
            if(lineNumber < firstLine && lineNumber > 0){
                firstLine = lineNumber;
            }
        }


        for(BytecodeInstruction instruction : instructions){

            if(instruction.getLineNumber() > firstLine){
                break;
            }
            if(instruction.getASMNodeString() != null && instruction.getASMNodeString().contains(deeperMethodInvokeInstruction)){
                result = true;
                break;
            }

            if(instruction.getASMNodeString() != null && instruction.getASMNodeString().contains("INVOKE")){
                result = false;
                break;
            }

        }
        return result;
    }


    public boolean isFirstLineSpecialCaller(StackTraceElement frame){
        return pool.contains(frame);
    }

}
