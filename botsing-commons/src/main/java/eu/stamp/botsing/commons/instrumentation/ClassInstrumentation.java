package eu.stamp.botsing.commons.instrumentation;

import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import org.evosuite.Properties;
import org.evosuite.classpath.ClassPathHandler;
import org.evosuite.setup.DependencyAnalysis;
import org.evosuite.testcase.DefaultTestCase;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testcase.execution.TestCaseExecutor;
import org.evosuite.testcase.statements.MethodStatement;
import org.evosuite.testcase.statements.Statement;
import org.evosuite.testcase.statements.StringPrimitiveStatement;
import org.evosuite.testcase.statements.numeric.BooleanPrimitiveStatement;
import org.evosuite.testcase.variable.VariableReference;
import org.evosuite.utils.generic.GenericMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class ClassInstrumentation {
    private static final Logger LOG = LoggerFactory.getLogger(ClassInstrumentation.class);
    public List<Class> instrumentClasses(List<String> interestingClasses, String testingClassName){
        List<Class> instrumentedClasses = new ArrayList<>();
        List<String> instrumentedClassesName = new ArrayList<>();
        List<String> nonDuplicatedClasses = interestingClasses.stream().distinct().collect(Collectors.toList());

        try{
            if(!Properties.INSTRUMENT_PARENT){
                String cp = ClassPathHandler.getInstance().getTargetProjectClasspath();

                for(String clazz : nonDuplicatedClasses){
                    if (clazz.equals(testingClassName)){
                        continue;
                    }
                    Properties.TARGET_CLASS=clazz;
                    DependencyAnalysis.analyzeClass(clazz, Arrays.asList(cp.split(File.pathSeparator)));
                }
            }


            Properties.TARGET_CLASS=testingClassName;
            instrumentClassByTestExecution(testingClassName);
        }catch (Exception e){
            LOG.warn("Could not instrument the target class!");
        }


        for(String clazz: nonDuplicatedClasses ){
            if(instrumentedClassesName.contains(clazz)){
                continue;
            }
            LOG.debug("Instrumenting class "+ clazz);
            Class<?> cls;
            try {
                Properties.TARGET_CLASS=clazz;
                cls = Class.forName(clazz,true, BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
                instrumentedClasses.add(cls);
                instrumentedClassesName.add(clazz);
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                LOG.warn("Error in loading {}",clazz);
            }
        }

        return instrumentedClasses;
    }


    public static void instrumentClassByTestExecution(String targetClass){
        DefaultTestCase test = generateTestForLoadingClass(targetClass);
        // execute the test contains the target class
        ExecutionResult execResult = TestCaseExecutor.getInstance().execute(test, Integer.MAX_VALUE);

        if (hasThrownInitializerError(execResult)) {
            // create single test suite with Class.forName()
            ExceptionInInitializerError ex = getInitializerError(execResult);
            throw ex;
        } else if (!execResult.getAllThrownExceptions().isEmpty()) {
            // some other exception has been thrown during initialization
            Throwable t = execResult.getAllThrownExceptions().iterator().next();
            try {
                throw t;
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }

    private static ExceptionInInitializerError getInitializerError(ExecutionResult execResult) {
        for (Throwable t : execResult.getAllThrownExceptions()) {
            if (t instanceof ExceptionInInitializerError) {
                ExceptionInInitializerError exceptionInInitializerError = (ExceptionInInitializerError)t;
                return exceptionInInitializerError;
            }
        }
        return null;
    }

    private static boolean hasThrownInitializerError(ExecutionResult execResult) {
        for (Throwable t : execResult.getAllThrownExceptions()) {
            if (t instanceof ExceptionInInitializerError) {
                return true;
            }
        }
        return false;
    }



    private static DefaultTestCase generateTestForLoadingClass(String targetClass) {

        DefaultTestCase test = new DefaultTestCase();
        StringPrimitiveStatement firstStatement = new StringPrimitiveStatement(test, targetClass);
        VariableReference string0 = test.addStatement(firstStatement);

        try{

            Method currentThreadMethod = Thread.class.getMethod("currentThread");
            Statement currentThreadStmt = new MethodStatement(test,
                    new GenericMethod(currentThreadMethod, currentThreadMethod.getDeclaringClass()), null,
                    Collections.emptyList());
            VariableReference currentThreadVar = test.addStatement(currentThreadStmt);

            Method getContextClassLoaderMethod = Thread.class.getMethod("getContextClassLoader");
            Statement getContextClassLoaderStmt = new MethodStatement(test,
                    new GenericMethod(getContextClassLoaderMethod, getContextClassLoaderMethod.getDeclaringClass()),
                    currentThreadVar, Collections.emptyList());
            VariableReference contextClassLoaderVar = test.addStatement(getContextClassLoaderStmt);

            Method loadClassMethod = ClassLoader.class.getMethod("loadClass", String.class);
            Statement loadClassStmt = new MethodStatement(test,
                    new GenericMethod(loadClassMethod, loadClassMethod.getDeclaringClass()), contextClassLoaderVar,
                    Collections.singletonList(string0));
            test.addStatement(loadClassStmt);

            BooleanPrimitiveStatement stmt1 = new BooleanPrimitiveStatement(test, true);
            VariableReference boolean0 = test.addStatement(stmt1);

            Method forNameMethod = Class.class.getMethod("forName",String.class, boolean.class, ClassLoader.class);
            Statement forNameStmt = new MethodStatement(test,
                    new GenericMethod(forNameMethod, forNameMethod.getDeclaringClass()), null,
                    Arrays.asList(string0, boolean0, contextClassLoaderVar));
            test.addStatement(forNameStmt);
        }catch(Exception e){
            LOG.error("! Error in loading the target class:");
            e.printStackTrace();
        }
        return test;
    }

}