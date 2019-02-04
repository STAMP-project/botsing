package eu.stamp.botsing.commons;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.evosuite.testcase.DefaultTestCase;
import org.evosuite.testcase.execution.EvosuiteError;
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

public class CFGGenerator {

    private static Statement currentThreadStmt;
    private static Statement getContextClassLoaderStmt;
    private static BooleanPrimitiveStatement booleanStmnt;

    private static final Logger LOG = LoggerFactory.getLogger(CFGGenerator.class);

    public void generateCFGS(List<String> interestingClasses) {
        for(String clazz: interestingClasses){
            LOG.info("Analyzing class "+ clazz);
            DefaultTestCase test = buildLoadClassTestCase(clazz);
            if (test != null){
                ExecutionResult execResult = TestCaseExecutor.getInstance().execute(test, Integer.MAX_VALUE);
                if (!execResult.getAllThrownExceptions().isEmpty()) {
                    Throwable t = execResult.getAllThrownExceptions().iterator().next();
                    LOG.warn("analyzing class {} failed",clazz);
                }
                LOG.info("The process of generating CFG for class {} is finished.",clazz);
            }
        }
    }


    public static DefaultTestCase buildLoadClassTestCase(String className) throws EvosuiteError {
        DefaultTestCase test = new DefaultTestCase();

        StringPrimitiveStatement saveClassNameToStringStatement = new StringPrimitiveStatement(test, className);
        VariableReference string0 = test.addStatement(saveClassNameToStringStatement);

        try {
            if (currentThreadStmt == null){
                Method currentThreadMethod = Thread.class.getMethod("currentThread");
                currentThreadStmt = new MethodStatement(test,
                        new GenericMethod(currentThreadMethod, currentThreadMethod.getDeclaringClass()), null,
                        Collections.emptyList());
            }
            VariableReference currentThreadVar = test.addStatement(currentThreadStmt);

            if (getContextClassLoaderStmt == null){
                Method getContextClassLoaderMethod = Thread.class.getMethod("getContextClassLoader");
                getContextClassLoaderStmt = new MethodStatement(test,
                        new GenericMethod(getContextClassLoaderMethod, getContextClassLoaderMethod.getDeclaringClass()),
                        currentThreadVar, Collections.emptyList());
            }
            VariableReference contextClassLoaderVar = test.addStatement(getContextClassLoaderStmt);

            if (booleanStmnt == null) {
                booleanStmnt = new BooleanPrimitiveStatement(test, true);
            }
            VariableReference boolean0 = test.addStatement(booleanStmnt);

            Method forNameMethod = Class.class.getMethod("forName",String.class, boolean.class, ClassLoader.class);
            Statement forNameStmt = new MethodStatement(test,
                    new GenericMethod(forNameMethod, forNameMethod.getDeclaringClass()), null,
                    Arrays.asList(string0, boolean0, contextClassLoaderVar));
            test.addStatement(forNameStmt);
            return test;
        } catch (NoSuchMethodException | SecurityException e) {
            LOG.warn("Unexpected exception while creating test for instrumenting class "+className );
            return null;
        }

    }

}
