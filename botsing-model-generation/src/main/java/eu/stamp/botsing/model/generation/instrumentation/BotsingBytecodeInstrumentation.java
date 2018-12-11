package eu.stamp.botsing.model.generation.instrumentation;

import org.evosuite.Properties;
import org.evosuite.assertion.CheapPurityAnalyzer;
import org.evosuite.graphs.cfg.CFGClassAdapter;
import org.evosuite.instrumentation.*;
import org.evosuite.runtime.RuntimeSettings;
import org.evosuite.runtime.instrumentation.*;
import org.evosuite.runtime.util.ComputeClassWriter;
import org.evosuite.seeding.PrimitiveClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.SerialVersionUIDAdder;
import org.objectweb.asm.util.TraceClassVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;

public class BotsingBytecodeInstrumentation {

    private static final Logger LOG = LoggerFactory.getLogger(BotsingBytecodeInstrumentation.class);
    public byte[] transformBytes(ClassLoader classLoader, String className, ClassReader reader) {

        TransformationStatistics.reset();

        int asmFlags = ClassWriter.COMPUTE_FRAMES;
        ClassWriter writer = new ComputeClassWriter(asmFlags);

        ClassVisitor cv = writer;
        if (LOG.isDebugEnabled()) {
            cv = new TraceClassVisitor(cv, new PrintWriter(System.err));
        }

        if (Properties.RESET_STATIC_FIELDS) {
            cv = new StaticAccessClassAdapter(cv, className);
        }

        if (Properties.PURE_INSPECTORS) {
            CheapPurityAnalyzer purityAnalyzer = CheapPurityAnalyzer.getInstance();
            cv = new PurityAnalysisClassVisitor(cv, className, purityAnalyzer);
        }

        if (Properties.MAX_LOOP_ITERATIONS >= 0) {
            cv = new LoopCounterClassAdapter(cv);
        }

        cv = new RemoveFinalClassAdapter(cv);

        cv = new ExecutionPathClassAdapter(cv, className);

        cv = new CFGClassAdapter(classLoader, cv, className);

        // Collect constant values for the value pool
        cv = new PrimitiveClassAdapter(cv, className);

        cv = handleStaticReset(className, cv);

        cv = new MethodCallReplacementClassAdapter(cv, className);
        if(RuntimeSettings.applyUIDTransformation){
            cv = new SerialVersionUIDAdder(cv);
        }

        reader.accept(cv, ClassReader.SKIP_FRAMES);
        return writer.toByteArray();
    }

    private static ClassVisitor handleStaticReset(String className, ClassVisitor cv) {
        cv = new CreateClassResetClassAdapter(cv, className, Properties.RESET_STATIC_FINAL_FIELDS);
        // Adds a callback before leaving the <clinit> method
        cv = new EndOfClassInitializerVisitor(cv, className);

        return cv;
    }


    public static boolean checkIfCanInstrument(String className) {
        return RuntimeInstrumentation.checkIfCanInstrument(className);
    }
}
