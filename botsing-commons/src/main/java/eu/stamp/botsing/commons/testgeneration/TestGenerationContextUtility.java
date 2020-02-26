package eu.stamp.botsing.commons.testgeneration;

import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import org.evosuite.TestGenerationContext;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.BytecodeInstructionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TestGenerationContextUtility {
    private static final Logger LOG = LoggerFactory.getLogger(TestGenerationContextUtility.class);

    public static ClassLoader getTestGenerationContextClassLoader(boolean isIntegration) {
        if (isIntegration) {
            return BotsingTestGenerationContext.getInstance().getClassLoaderForSUT();
        } else {
            return TestGenerationContext.getInstance().getClassLoaderForSUT();
        }
    }

    /**
     * @param isIntegration Whether integration testing is used. It will affect the context used for the class loader.
     * @param className     The name of the given class, separated by dots.
     * @param lineNumber    The line number in the given class.
     *
     * @return Full name of the method, with signature descriptors, at the given line number of the given class.
     *
     * @see #getTestGenerationContextClassLoader(boolean)
     */
    public static String derivingMethodFromBytecode(boolean isIntegration, String className, int lineNumber) {
        List<BytecodeInstruction> instructions = BytecodeInstructionPool.getInstance(getTestGenerationContextClassLoader(isIntegration)).getInstructionsIn(className);
        if (instructions != null) {
            for (BytecodeInstruction ins : instructions) {
                if (ins != null) {
                    if (ins.getLineNumber() == lineNumber) {
                        return ins.getMethodName();
                    }
                }
            }
            LOG.error("CrashCoverageTestfitness.derivingMethodFromBytecode: instruction for this line number " + lineNumber + " does not found!");
        } else {
            LOG.error("CrashCoverageTestfitness.derivingMethodFromBytecode: instruction for this class " + className + " was null!");
        }
        return null;
    }
}
