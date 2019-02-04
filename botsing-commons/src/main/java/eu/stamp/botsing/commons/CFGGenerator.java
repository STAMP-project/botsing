package eu.stamp.botsing.commons;

import java.util.*;

import org.evosuite.graphs.GraphPool;
import org.evosuite.graphs.cfg.RawControlFlowGraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CFGGenerator {
//
//    private static Statement currentThreadStmt;
//    private static Statement getContextClassLoaderStmt;
//    private static BooleanPrimitiveStatement booleanStmnt;

    private static final Logger LOG = LoggerFactory.getLogger(CFGGenerator.class);
    private List<RawControlFlowGraph> cfgs = new ArrayList<>();

    public void generateCFGS(List<String> interestingClasses) {
        List<Class> instrumentedClasses = instrumentClasses(interestingClasses);
        if(!instrumentedClasses.isEmpty()){
            collectCFGS(instrumentedClasses);
        }else{
            LOG.error("There is no instrumented classes!");
        }
    }

    private void collectCFGS(List<Class> instrumentedClasses) {
        GraphPool graphPool = GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
        for (Class clazz : instrumentedClasses){
            LOG.info("CLAZZZ: {}",clazz.getName());
            Map<String, RawControlFlowGraph> methodsGraphs = graphPool.getRawCFGs(clazz.getName());
            if (methodsGraphs != null) {
                for (Map.Entry<String, RawControlFlowGraph> entry : methodsGraphs.entrySet()) {
                    RawControlFlowGraph cfg = entry.getValue();
                    cfgs.add(cfg);
                }
            } else {
                LOG.warn("The generated control flow graphs for class {} was empty.", clazz);
            }
        }
    }

    public List<RawControlFlowGraph> getCfgs(){
        return cfgs;
    }

    private List<Class> instrumentClasses(List<String> interestingClasses){
        List<Class> instrumentedClasses = new ArrayList<>();
        for(String clazz: interestingClasses){
            LOG.info("Instrumenting class "+ clazz);
            Class<?> cls = null;
            try {
                cls = Class.forName(clazz,false, BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
                instrumentedClasses.add(cls);
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                LOG.warn("Error in loading {}",clazz);
            }



//            DefaultTestCase test = buildLoadClassTestCase(clazz);
//            if (test != null){
//                ExecutionResult execResult = TestCaseExecutor.getInstance().execute(test, Integer.MAX_VALUE);
//                if (!execResult.getAllThrownExceptions().isEmpty()) {
//                    Throwable t = execResult.getAllThrownExceptions().iterator().next();
//                    LOG.warn("analyzing class {} failed",clazz);
//                }
//                LOG.info("The process of generating CFG for class {} is finished.",clazz);
//            }
        }
        return instrumentedClasses;
    }


//    private static DefaultTestCase buildLoadClassTestCase(String className) throws EvosuiteError {
//        DefaultTestCase test = new DefaultTestCase();
//
//        StringPrimitiveStatement saveClassNameToStringStatement = new StringPrimitiveStatement(test, className);
//        VariableReference string0 = test.addStatement(saveClassNameToStringStatement);
//
//        try {
//            if (currentThreadStmt == null){
//                Method currentThreadMethod = Thread.class.getMethod("currentThread");
//                currentThreadStmt = new MethodStatement(test,
//                        new GenericMethod(currentThreadMethod, currentThreadMethod.getDeclaringClass()), null,
//                        Collections.emptyList());
//            }
//            VariableReference currentThreadVar = test.addStatement(currentThreadStmt);
//
//            if (getContextClassLoaderStmt == null){
//                Method getContextClassLoaderMethod = Thread.class.getMethod("getContextClassLoader");
//                getContextClassLoaderStmt = new MethodStatement(test,
//                        new GenericMethod(getContextClassLoaderMethod, getContextClassLoaderMethod.getDeclaringClass()),
//                        currentThreadVar, Collections.emptyList());
//            }
//            VariableReference contextClassLoaderVar = test.addStatement(getContextClassLoaderStmt);
//
//            if (booleanStmnt == null) {
//                booleanStmnt = new BooleanPrimitiveStatement(test, true);
//            }
//            VariableReference boolean0 = test.addStatement(booleanStmnt);
//
//            Method forNameMethod = Class.class.getMethod("forName",String.class, boolean.class, ClassLoader.class);
//            Statement forNameStmt = new MethodStatement(test,
//                    new GenericMethod(forNameMethod, forNameMethod.getDeclaringClass()), null,
//                    Arrays.asList(string0, boolean0, contextClassLoaderVar));
//            test.addStatement(forNameStmt);
//            return test;
//        } catch (NoSuchMethodException | SecurityException e) {
//            LOG.warn("Unexpected exception while creating test for instrumenting class "+className );
//            return null;
//        }
//
//    }

}
