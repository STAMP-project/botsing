package eu.stamp.botsing.integration.integrationtesting;

import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import eu.stamp.botsing.commons.instrumentation.ClassPool;
import eu.stamp.botsing.commons.testgeneration.strategy.MOSuiteStrategy;
import eu.stamp.botsing.integration.IntegrationTestingProperties;
import eu.stamp.botsing.integration.fitnessfunction.FitnessFunctions;
import eu.stamp.botsing.integration.testgeneration.strategy.TestGenerationUtility;
import org.evosuite.Properties;
import org.evosuite.TestGenerationContext;
import org.evosuite.classpath.ClassPathHandler;
import org.evosuite.coverage.branch.BranchPool;
import org.evosuite.graphs.GraphPool;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.RawControlFlowGraph;
import org.evosuite.instrumentation.testability.BooleanTestabilityTransformation;
import org.evosuite.setup.DependencyAnalysis;
import org.evosuite.strategy.TestGenerationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IntegrationTestingUtility {

    private static final Logger LOG = LoggerFactory.getLogger(IntegrationTestingUtility.class);
    public static TestGenerationStrategy getTestGenerationFactory(){
        switch (IntegrationTestingProperties.searchAlgorithm){
            case MOSA:
                return new MOSuiteStrategy(new TestGenerationUtility(),new FitnessFunctions());
            default:
                return new MOSuiteStrategy(new TestGenerationUtility(),new FitnessFunctions());
        }
    }


    public static Class detectParentInHierarchyTree(Class caller, Class callee) {

        if(isExtendedBy(caller.getName(),callee.getName())){
            return callee;
        }else if(isExtendedBy(callee.getName(),caller.getName())){
            return caller;
        }
        return null;
    }

    public static Class detectSubClassInHierarchyTree(Class caller, Class callee) {
        if(isExtendedBy(caller.getName(),callee.getName())){
            return caller;
        }else if(isExtendedBy(callee.getName(),caller.getName())){
            return callee;
        }
        return null;
    }

    public static Class getCallerClass(){
        Class caller;
        try {

            caller = IntegrationTestingUtility.fetchClass(IntegrationTestingProperties.TARGET_CLASSES[1]);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("caller cannot be loaded");
        }

        return caller;
    }


    public static Class getCalleeClass(){
        Class caller;
        try {
            caller = IntegrationTestingUtility.fetchClass(IntegrationTestingProperties.TARGET_CLASSES[0]);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("caller cannot be loaded");
        }

        return caller;
    }

    public static boolean isExtendedBy(String loadedClass,String calledClass) {
        Class loadedClazz = null;
        try {
            loadedClazz = IntegrationTestingUtility.fetchClass(loadedClass);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        //  Checking the interfaces
        for (Class interfaceClazz :loadedClazz.getInterfaces()){
            if(interfaceClazz.getName().contains("evosuite")){
                continue;
            }
            if(interfaceClazz.getName().equals(calledClass)){

                return true;
            }
        }

        // Checking the abstract classes

        Class superClass = loadedClazz.getSuperclass();

        while(!superClass.getName().equals("java.lang.Object")){
            if(superClass.getName().equals(calledClass)){
                if(!BotsingTestGenerationContext.getInstance().getClassLoaderForSUT().getLoadedClasses().contains(superClass)){
                    try {
                        IntegrationTestingUtility.fetchClass(superClass.getName());
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                return true;
            }
            superClass = superClass.getSuperclass();
        }

        return false;
    }


    public static Class fetchClass(String className) throws ClassNotFoundException {
        String originalTargetClass = Properties.TARGET_CLASS;
        Properties.TARGET_CLASS = IntegrationTestingProperties.TARGET_CLASSES[1];
        Class cls = Class.forName(className,true,TestGenerationContext.getInstance().getClassLoaderForSUT());
        Properties.TARGET_CLASS = originalTargetClass;
        return cls;
//        return ClassPool.getInstance().fetchClass(className);
    }

    public static void registerBranch(BytecodeInstruction branchBC, BranchPool branchPool){
        String branchClassName = branchBC.getClassName();
        String originalTargetClass = Properties.TARGET_CLASS;
        Properties.TARGET_CLASS = branchClassName;
        branchPool.registerAsBranch(branchBC);
        Properties.TARGET_CLASS = originalTargetClass;
    }

    public static Set<String> collectSubClassMethodCallers(String subClassName, Set<String> methodsWithCallSite) {
        GraphPool graphPool = GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
        Set<String> result = new HashSet<>();
        Set<String> subClassMethods = graphPool.getRawCFGs(subClassName).keySet();
        for (RawControlFlowGraph rcfg: graphPool.getRawCFGs(subClassName).values()){
            for(BytecodeInstruction calls: rcfg.determineMethodCalls()){
                String calledMethod = calls.getCalledMethod();
                if(!subClassMethods.contains(calledMethod) && methodsWithCallSite.contains(calledMethod)){
                    result.add(rcfg.getMethodName());
                }
            }
        }
        return result;
    }

    public static void analyzeClassDependencies(String  className) {
        String cp = ClassPathHandler.getInstance().getTargetProjectClasspath();
        List<String> cpList = Arrays.asList(cp.split(File.pathSeparator));
        Properties.TARGET_CLASS=className;
        try {
            LOG.info("Starting the dependency analysis. The number of detected jar files is {}.",cpList.size());
            DependencyAnalysis.analyzeClass(className,Arrays.asList(cp.split(File.pathSeparator)));
            LOG.info("Analysing dependencies done!");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
