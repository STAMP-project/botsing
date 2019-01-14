package eu.stamp.botsing.seeding;

import be.vibes.dsl.io.Xml;
import be.vibes.dsl.selection.Dissimilar;
import be.vibes.ts.Action;
import be.vibes.ts.TestSet;
import be.vibes.ts.Transition;
import be.vibes.ts.UsageModel;
import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.StackTrace;
import org.apache.commons.lang3.StringUtils;
import org.evosuite.Properties;
import org.evosuite.TestGenerationContext;
import org.evosuite.graphs.GraphPool;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.RawControlFlowGraph;
import org.evosuite.seeding.ObjectPool;
import org.evosuite.setup.TestClusterUtils;
import org.evosuite.testcase.DefaultTestCase;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestFactory;
import org.evosuite.utils.generic.GenericClass;
import org.evosuite.utils.generic.GenericConstructor;
import org.evosuite.utils.generic.GenericMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.*;

public class ModelSeedingHelper {
    private static final Logger LOG = LoggerFactory.getLogger(ModelSeedingHelper.class);
    List<String> seedingClasses = new ArrayList<>();
    String modelsPath;


    public ModelSeedingHelper(String modelsPath){
        fetchUsefulClasses();
        this.modelsPath = modelsPath;
    }

    private void fetchUsefulClasses() {
        StackTrace crash = CrashProperties.getInstance().getStackTrace();
        for(StackTraceElement frame : crash.getAllFrames()){
            if(!seedingClasses.contains(frame.getClassName())) {
                seedingClasses.add(frame.getClassName());
                getAccessedClasses(frame.getClassName());
            }
        }
    }

    private void getAccessedClasses(String clazz) {
        GraphPool graphPool = GraphPool.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT());
        Map<String, RawControlFlowGraph> methodsGraphs = graphPool.getRawCFGs(clazz);
        if (methodsGraphs != null) {
            LOG.info("analyzing accessed classes in class: "+clazz);
            for (Map.Entry<String, RawControlFlowGraph> entry : methodsGraphs.entrySet()) {
                RawControlFlowGraph cfg = entry.getValue();
                List<BytecodeInstruction> bcList = cfg.determineMethodCalls();
                for (BytecodeInstruction bc : bcList) {
                    String calledMethodsClass = bc.getCalledMethodsClass();
                    if (!calledMethodsClass.equals(clazz) && !bc.toString().contains("evosuite")) {
                        if(!seedingClasses.contains(calledMethodsClass)){
                            seedingClasses.add(calledMethodsClass);
                        }
                    }
                }
            }
        }
    }


    public ObjectPool generatePool() {
        ObjectPool objectPool= new ObjectPool();
        File folder = new File(this.modelsPath);
        File[] listOfModels = folder.listFiles();
        LOG.info("Start carving model");
        for (File file : listOfModels) {
            if (file.isFile() && !file.getName().startsWith(".") && file.getName().endsWith(".xml") ) {
                String xmlClassName = file.getName().substring(0, file.getName().length() - 4);
//                if (xmlClassName.indexOf('.')== -1 || seedingClasses.contains(xmlClassName)){
                if (seedingClasses.contains(xmlClassName)){
                    LOG.info("working on model of " + xmlClassName);
                    try {
                        UsageModel um = Xml.loadUsageModel(Paths.get(folder.getAbsolutePath(), file.getName()).toString());
                        TestSet ts = Dissimilar.from(um).withGlobalMaxDistance(Dissimilar.jaccard()).during(5000).generate(Properties.POPULATION);
                        for (be.vibes.ts.TestCase abstractTestCase : ts) {
                            TestCase newTestCase = new DefaultTestCase();
                            GenericClass genericClass = null;
                            boolean addConstructor = true;
                            for (Transition transition : abstractTestCase) {
                                Action sequence = transition.getAction();
                                if (sequence.getName().indexOf(".") != -1) {
                                    // Class name:
                                    String className = sequence.getName().substring(0, sequence.getName().indexOf("("));
                                    className = className.substring(0, className.lastIndexOf('.'));
                                    // Method name:
                                    String methodName = StringUtils.substringAfterLast(sequence.getName().substring(0, sequence.getName().indexOf("(")), ".");
                                    String paramString = sequence.getName().substring(sequence.getName().indexOf("(") + 1);

                                    if (methodName.equals("<init>")) {
                                        addConstructor = false;
                                        break;
                                    }

                                    Method target = null;
                                    Class<?> sequenceClass = null;
                                    try {
                                        sequenceClass = Class.forName(className, true, TestGenerationContext.getInstance().getClassLoaderForSUT());
                                    } catch (ClassNotFoundException | ExceptionInInitializerError | NoClassDefFoundError e) {
                                        LOG.debug("could not load " + className);
                                    }
                                    if (sequenceClass != null) {
                                        Set<Method> methods = TestClusterUtils.getMethods(sequenceClass);
                                        for (Method m : methods) {
                                            if (m.getName().equals(methodName)) {
                                                target = m;
                                                break;
                                            } else {
                                                target = null;
                                            }
                                        }

                                        if (target != null) {
                                            GenericMethod genericMethod = new GenericMethod(target, sequenceClass);
                                            if (!genericMethod.isStatic()) {
                                                break;
                                            }
                                        }
                                    }
                                }

                            }


                            if (addConstructor) {
                                Transition transition = abstractTestCase.getFirst();
                                Action sequence = transition.getAction();
                                String className = sequence.getName().substring(0, sequence.getName().indexOf("("));
                                className = className.substring(0, className.lastIndexOf('.'));
                                Class<?> sequenceClass = null;
                                try {
                                    sequenceClass = Class.forName(className, true, TestGenerationContext.getInstance().getClassLoaderForSUT());
                                } catch (ClassNotFoundException | ExceptionInInitializerError | NoClassDefFoundError e) {
                                    LOG.debug("could not load " + className);
                                }
                                if (sequenceClass != null) {
                                    Set<Constructor<?>> constructors = TestClusterUtils.getConstructors(sequenceClass);
                                    int i = 0;
                                    int chosenConstructorIndex = new Random().nextInt(constructors.size());
                                    Constructor<?> chosenConstructor = null;
                                    for (Constructor<?> c : constructors) {
                                        if (i == chosenConstructorIndex) {
                                            chosenConstructor = c;
                                            break;
                                        }
                                        i++;
                                    }
                                    GenericConstructor genericConstructor = new GenericConstructor(chosenConstructor, sequenceClass);
                                    try {
                                        TestFactory.getInstance().addConstructor(newTestCase, genericConstructor, newTestCase.size(), 0);
                                        LOG.debug("constructor {} is added", genericConstructor.getName());
                                    } catch (Exception e) {
                                        LOG.debug("Error in addidng " + genericConstructor.getName() + "  " + e.getMessage());
                                    }
                                }
                            }


                            for (Transition transition : abstractTestCase) {
                                Action sequence = transition.getAction();
                                if (sequence.getName().indexOf(".") != -1) {
                                    // Class name:
                                    String className = sequence.getName().substring(0, sequence.getName().indexOf("("));
                                    className = className.substring(0, className.lastIndexOf('.'));
                                    // Method name:
                                    String methodName = StringUtils.substringAfterLast(sequence.getName().substring(0, sequence.getName().indexOf("(")), ".");
                                    String paramString = sequence.getName().substring(sequence.getName().indexOf("(") + 1);
                                    // Params:
                                    paramString = paramString.substring(0, paramString.indexOf(")"));
                                    String[] paramArr = paramString.split(",");
                                    //								try {
                                    //Getting the Class
                                    Class<?> sequenceClass = null;
                                    try {
                                        sequenceClass = Class.forName(className, true, TestGenerationContext.getInstance().getClassLoaderForSUT());
                                    } catch (ClassNotFoundException | ExceptionInInitializerError | NoClassDefFoundError e) {
                                        LOG.debug("could not load " + className);
                                    }
                                    if (sequenceClass != null) {
                                        genericClass = new GenericClass(sequenceClass);
                                        //Getting methods
                                        Set<Method> methods = TestClusterUtils.getMethods(sequenceClass);
                                        //Getting Constructors
                                        Set<Constructor<?>> constructors = TestClusterUtils.getConstructors(sequenceClass);

                                        // find the method that we want
                                        Method target = null;
                                        for (Method m : methods) {
                                            if (m.getName().equals(methodName)) {
                                                target = m;
                                                break;
                                            } else {
                                                target = null;
                                            }
                                        }

                                        // Find the constructor that we want
                                        Constructor targetC = null;
                                        for (Constructor c : constructors) {
                                            boolean same = true;
                                            int counter = 0;

                                            for (Class<?> cl : c.getParameterTypes()) {
                                                if (paramArr.length > counter && !cl.getName().equals(paramArr[counter])) {
                                                    same = false;
                                                }
                                                counter++;
                                            }
                                            if (same) {
                                                targetC = c;
                                                break;
                                            }
                                        }


                                        if (target != null) {
                                            GenericMethod genericMethod = new GenericMethod(target, sequenceClass);
                                            try {
                                                TestFactory.getInstance().addMethod(newTestCase, genericMethod, newTestCase.size(), 0);
                                                LOG.debug("method call {} is added", genericMethod.getName());
                                            } catch (Exception e) {
                                                LOG.debug("Error in addidng " + genericMethod.getName() + "  " + e.getMessage());
                                            }
                                        } else if (targetC != null) {
                                            GenericConstructor genericConstructor = new GenericConstructor(targetC, sequenceClass);
                                            try {
                                                TestFactory.getInstance().addConstructor(newTestCase, genericConstructor, newTestCase.size(), 0);
                                                LOG.debug("constructor {} is added", genericConstructor.getName());
                                            } catch (Exception e) {
                                                LOG.debug("Error in addidng " + genericConstructor.getName() + "  " + e.getMessage());
                                            }

                                        } else {
                                            LOG.debug("Fail to add the call to add!");
                                        }
                                    }
                                }

                            }

                            // Add test case to pool
                            if (genericClass != null){
                                LOG.debug("New test case added for class {}",genericClass.getClassName());
                                objectPool.addSequence(genericClass,newTestCase);
                            }

                        }
                    }catch (Exception e) {
                        LOG.debug("Could not load model " + file.getName());
                    }
                }
            }
        }
        return objectPool;
    }
}
