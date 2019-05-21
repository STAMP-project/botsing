package eu.stamp.botsing.testgeneration;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import org.evosuite.TestGenerationContext;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.BytecodeInstructionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TestGenerationContextUtility {
    private static final Logger LOG = LoggerFactory.getLogger(TestGenerationContextUtility.class);
    public static ClassLoader getTestGenerationContextClassLoader(){
        if(CrashProperties.integrationTesting){
            return BotsingTestGenerationContext.getInstance().getClassLoaderForSUT();
        }else{
            return TestGenerationContext.getInstance().getClassLoaderForSUT();
        }
    }


    public static  String derivingMethodFromBytecode(String className, int lineNumber){
        List<BytecodeInstruction> instructions = BytecodeInstructionPool.getInstance(getTestGenerationContextClassLoader()).getInstructionsIn(className);
        if (instructions != null) {
            for (BytecodeInstruction ins : instructions) {
                if(ins != null) {
                    if (ins.getLineNumber() == lineNumber){
                        String bytecodeMethodName = ins.getMethodName();
                        //						if (bytecodeMethodName.contains(methodName))
                        return bytecodeMethodName;
                    }
                } else {
                    LOG.error("CrashCoverageTestfitness.derivingMethodFromBytecode: instruction for this line number " + lineNumber+" was null!");
                }
            }
        } else {
            LOG.error("CrashCoverageTestfitness.derivingMethodFromBytecode: instruction for this class " + className +" was null!");
        }
        return null;
    }

}
