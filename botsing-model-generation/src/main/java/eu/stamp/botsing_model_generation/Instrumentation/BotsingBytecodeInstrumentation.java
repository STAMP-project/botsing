package eu.stamp.botsing_model_generation.Instrumentation;

import org.evosuite.Properties;
import org.evosuite.assertion.CheapPurityAnalyzer;
import org.evosuite.classpath.ResourceList;
import org.evosuite.graphs.cfg.CFGClassAdapter;
import org.evosuite.instrumentation.*;
import org.evosuite.instrumentation.error.ErrorConditionClassAdapter;
import org.evosuite.junit.writer.TestSuiteWriterUtils;
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


        int readFlags = ClassReader.SKIP_FRAMES;
        String classNameWithDots = ResourceList.getClassNameFromResourcePath(className);

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


            LOG.debug("Applying target transformation to class " + classNameWithDots);
            if (!Properties.TEST_CARVING && Properties.MAKE_ACCESSIBLE) {
                cv = new AccessibleClassAdapter(cv, className);
            }

            cv = new RemoveFinalClassAdapter(cv);

            cv = new ExecutionPathClassAdapter(cv, className);

            cv = new CFGClassAdapter(classLoader, cv, className);

            if (Properties.EXCEPTION_BRANCHES) {
                cv = new ExceptionTransformationClassAdapter(cv, className);
            }

            if (Properties.ERROR_BRANCHES) {
                cv = new ErrorConditionClassAdapter(cv, className);
            }


        // Collect constant values for the value pool
        cv = new PrimitiveClassAdapter(cv, className);

        if (Properties.RESET_STATIC_FIELDS) {
            cv = handleStaticReset(className, cv);
        }

        // Mock instrumentation (eg File and TCP).
        if (TestSuiteWriterUtils.needToUseAgent()) {
            cv = new MethodCallReplacementClassAdapter(cv, className);

            /*
             * If the class is serializable, then doing any change (adding hashCode, static reset, etc)
             * will change the serialVersionUID if it is not defined in the class.
             * Hence, if it is not defined, we have to define it to
             * avoid problems in serialising the class, as reading Master will not do instrumentation.
             * The serialVersionUID HAS to be the same as the un-instrumented class
             */
            if(RuntimeSettings.applyUIDTransformation){
                cv = new SerialVersionUIDAdder(cv);
            }
        }


        reader.accept(cv, readFlags);
        return writer.toByteArray();
    }


    private static ClassVisitor handleStaticReset(String className, ClassVisitor cv) {

        final CreateClassResetClassAdapter resetClassAdapter;
        if (Properties.RESET_STATIC_FINAL_FIELDS) {
            resetClassAdapter= new CreateClassResetClassAdapter(cv, className, true);
        } else {
            resetClassAdapter= new CreateClassResetClassAdapter(cv, className, false);
        }
        cv = resetClassAdapter;

        // Adds a callback before leaving the <clinit> method
        EndOfClassInitializerVisitor exitClassInitAdapter = new EndOfClassInitializerVisitor(cv, className);
        cv = exitClassInitAdapter;
        return cv;
    }
}
