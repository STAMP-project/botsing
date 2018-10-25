package eu.stamp.botsing_model_generation.generation.behavioral_model;


import eu.stamp.botsing_model_generation.BotsingTestGenerationContext;
import eu.stamp.botsing_model_generation.analysis.classpath.CPAnalysor;
import eu.stamp.botsing_model_generation.generation.behavioral_model.model.Model;
import eu.stamp.botsing_model_generation.testcase.execution.TestExecutor;
import org.evosuite.Properties;
import org.evosuite.classpath.ClassPathHandler;
import org.evosuite.graphs.GraphPool;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.RawControlFlowGraph;
import org.evosuite.setup.InheritanceTree;
import org.evosuite.testcase.DefaultTestCase;
import org.evosuite.testcase.execution.EvosuiteError;
import org.evosuite.testcase.execution.ExecutionResult;
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


public class ModelGeneration {
    private static final Logger LOG = LoggerFactory.getLogger(ModelGeneration.class);

    private String[] projectClassPaths;
    public ModelGeneration(String cp){
        projectClassPaths=cp.split(File.pathSeparator);
    }
    public ModelGeneration(String[] jarsCp ){
        projectClassPaths = jarsCp.clone();
    }
    private List<String> interestingClasses =  new ArrayList<String>();


    private static Statement currentThreadStmt;
    private static Statement getContextClassLoaderStmt;
    private static BooleanPrimitiveStatement booleanStmnt;


    public Model generate(){
        if(projectClassPaths == null){
            LOG.error("Project classpath should be set before the model generation.");
            return null;
        }

        ClassPathHandler.getInstance().changeTargetClassPath(projectClassPaths);
        List<String> cpList = Arrays.asList(projectClassPaths);
        try {
            CPAnalysor.analyzeClass(cpList);
        } catch (ClassNotFoundException e) {
            LOG.error("The passed class could not be found! please revise your input.");
        }

        detectInterestingClasses();
        generateCFGS();
        staticAnalysis();

        return null;
    }

    private void staticAnalysis() {
        for(String clazz: interestingClasses) {
            GraphPool graphPool = GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
            Map<String, RawControlFlowGraph> methodsGraphs = graphPool.getRawCFGs(clazz);
            for (Map.Entry<String, RawControlFlowGraph> entry : methodsGraphs.entrySet()) {
                String methodname = entry.getKey();
                LOG.info("Reading Call Sequences from method " + methodname);
                RawControlFlowGraph cfg = entry.getValue();
                List<BytecodeInstruction> bcList = cfg.determineMethodCalls();
                for (BytecodeInstruction bc : bcList) {
                    // TODO: Store them in CallSequence Pool
                    LOG.info(bc.toString());
                    String keyName = bc.getCalledMethodsClass();
                    if (!keyName.equals(clazz)){
//                        if (!temp.containsKey(keyName)){
//                            temp.put(keyName,new ArrayList<MethodCalls>());
//                        }
//                        temp.get(keyName).add(new MethodCalls(bc));
                    }
                }
            }
        }
    }

    private void generateCFGS() {
        for(String clazz: interestingClasses){
            LOG.info("Analyzing class "+ clazz);
            DefaultTestCase test = buildLoadClassTestCase(clazz);
            ExecutionResult execResult = TestExecutor.getInstance().execute(test, Integer.MAX_VALUE);
            // TODO: check the result of the execution
            LOG.info("The process of generating CFG for class -{} is finished.",clazz);
        }
    }

    private void detectInterestingClasses() {
        InheritanceTree projectTree = CPAnalysor.getInheritanceTree();
        for (String clazz:  projectTree.getAllClasses()){
            if (clazz.startsWith(Properties.TARGET_CLASS_PREFIX)){
                interestingClasses.add(clazz);
            }
        }

    }


    private static DefaultTestCase buildLoadClassTestCase(String className) throws EvosuiteError {
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
                    Arrays.<VariableReference>asList(string0, boolean0, contextClassLoaderVar));
            test.addStatement(forNameStmt);
            return test;
        } catch (NoSuchMethodException | SecurityException e) {
            throw new EvosuiteError("Unexpected exception while creating test for instrumenting class "+className );
        }

    }

}
