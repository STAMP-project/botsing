package eu.stamp.botsing.model.generation.analysis.sourcecode;

import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import eu.stamp.botsing.model.generation.callsequence.CallSequencesPoolManager;
import eu.stamp.botsing.model.generation.callsequence.MethodCall;
import org.evosuite.graphs.GraphPool;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.RawControlFlowGraph;
import org.evosuite.junit.CoverageAnalysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class StaticAnalyser {
    private static final Logger LOG = LoggerFactory.getLogger(StaticAnalyser.class);


    private BytecodeInstruction oldBC = null;
    private String oldBCObject = null;
    private String oldBCBranch = null;

    private LinkedList testSuite = new LinkedList<>();
    private Map<String,List<String>> objectsTests =  new HashMap<>();


//    private Map<String, List<List<MethodCall>>> exportedMethodCalls =  new HashMap<String, List<List<MethodCall>>>();


    public void analyse(List<String> interestingClasses) {
        int counter = 0;
        for (String clazz : interestingClasses) {
            try{
                if (clazz.startsWith("org.xwiki.rendering.wikimodel.internal.common.javacc")){
                    continue;
                }
                    counter++;
                    LOG.info("Analyzing methods of class " + clazz + " ("+counter+"/"+interestingClasses.size()+")");
                    boolean isTest = false;
                    Class<?> cls = null;
                    try {
                        cls = Class.forName(clazz,false, BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
                        if(CoverageAnalysis.isTest(cls)){
                            LOG.info("The class {} is a testSuite",clazz);
                            isTest=true;
                            testSuite.add(clazz);
                        }
                    } catch (ClassNotFoundException | NoClassDefFoundError e) {
        //                e.printStackTrace();
                        LOG.warn("error in loading {}",clazz);
                    }


                    GraphPool graphPool = GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
                    Map<String, RawControlFlowGraph> methodsGraphs = graphPool.getRawCFGs(clazz);
                    if (methodsGraphs != null) {
                        for (Map.Entry<String, RawControlFlowGraph> entry : methodsGraphs.entrySet()) {
                            Map<String, Map<String, List<MethodCall>>> collectedCallSequencesForCurrentMethod = analyseMethod(clazz, entry.getKey(), entry.getValue(),isTest);
                            savingMethodCallSequences(collectedCallSequencesForCurrentMethod);
                        }
                    } else {
                        LOG.warn("The generated control flow graphs for class {} was empty. We cannot execute manual analysis withour the control flow graph.", clazz);
                    }
            }catch(Exception e){
                LOG.warn("Error in analyzing class {}",clazz);
                LOG.warn(e.toString());
            }
        }
    }

    private void savingMethodCallSequences(Map<String, Map<String, List<MethodCall>>> methodCallSequences) {
        for (Map.Entry<String, Map<String, List<MethodCall>>> CSEntry : methodCallSequences.entrySet()) {
            String clazz = CSEntry.getKey();
            Map<String, List<MethodCall>> sequences = CSEntry.getValue();
            for (Map.Entry<String, List<MethodCall>> callSequenceEntry : sequences.entrySet()) {
                List<MethodCall> callSequence = callSequenceEntry.getValue();
                CallSequencesPoolManager.getInstance().addSequence(clazz, callSequence);
            }
        }
    }

    private Map<String, Map<String, List<MethodCall>>> analyseMethod(String className, String methodname, RawControlFlowGraph cfg, boolean isTest) {
        LOG.info("Reading Call Sequences from method " + methodname);
        clearOldBC();
        Map<String, Map<String, List<MethodCall>>> callSequencesOfCurrentMethod = new HashMap<String, Map<String, List<MethodCall>>>();
        List<BytecodeInstruction> bcList = cfg.determineMethodCalls();
        for (BytecodeInstruction bc : bcList) {
            LOG.debug("analyzing byteCode " + bc.explain());
            String calledMethodsClass = bc.getCalledMethodsClass();
            if (!calledMethodsClass.equals(className) && !bc.toString().contains("evosuite")) {// Filter the internal method calls

                if (isTest){
                    if (!objectsTests.containsKey(calledMethodsClass)){
                        objectsTests.put(calledMethodsClass,new LinkedList<String>());
                    }
                    if (!objectsTests.get(calledMethodsClass).contains(className)){
                        objectsTests.get(calledMethodsClass).add(className);
                    }
                }

                if (bc.isConstructorInvocation()) {
                    // Here, we should instantiate a new call sequence
                    handleConstructorInvocation(bc, cfg, callSequencesOfCurrentMethod);
                } else if (bc.isCallToStaticMethod() || bc.toString().contains("INVOKESTATIC")) {
                    // Here, we have a call to a static method call. It means we may need a new call sequence.
                    handleStaticMethodCallInvocation(bc, callSequencesOfCurrentMethod);
                } else {
                    // Here, we have a regular method call. Also, We are sure that we do not need to initialize a new call sequence of call here. We should just add this to an existing call sequence.
                    handleRegularMethodInvocation(bc, cfg, callSequencesOfCurrentMethod);
                }
            } else {
                LOG.debug("The bytecode Instruction is filtered.");
                clearOldBC();
            }
        }
        return callSequencesOfCurrentMethod;
    }

    private void handleRegularMethodInvocation(BytecodeInstruction bc, RawControlFlowGraph cfg, Map<String, Map<String, List<MethodCall>>> callSequencesOfCurrentMethod) throws IllegalStateException{
        if (bc.getSourceOfMethodInvocationInstruction() != null) {
            if (bc.getSourceOfMethodInvocationInstruction().getVariableName() != null) {
                if (bc.getSourceOfMethodInvocationInstruction().getVariableName().length() > 0) {
                    // We have a variable for the object which is used for method invocation
                    String variableName = bc.getSourceOfMethodInvocationInstruction().getVariableName();
                    // We should have the type of the object in this slot
                    String parentType = detectParentType(variableName, callSequencesOfCurrentMethod);
                    recordRegularInvocation(bc, parentType, variableName, callSequencesOfCurrentMethod);
                } else {
                    LOG.warn("The variable name is empty: {} ", bc);
//                    throw new IllegalStateException("Variable with empty name discovered during static analysis for instruction: " + bc);
                }

            } else {
                if (bc.getSourceOfMethodInvocationInstruction().equals(this.oldBC)) {
                    // This method is invoked by the returned value of the previous method call. So, it should be store in the same call sequence.
                    recordInvocation(bc, this.oldBCObject, oldBCBranch, callSequencesOfCurrentMethod);
                } else {
                    LOG.error("The variable name of the current byteCode instruction is missing: " + bc.toString());
                }
            }
        } else {
            LOG.warn("Following regular method call cannot find its source of method invocation: " + bc.toString());
        }
    }

    private void recordRegularInvocation(BytecodeInstruction bc, String parentType, String variableName, Map<String, Map<String, List<MethodCall>>> callSequencesOfCurrentMethod) throws IllegalStateException {
        if (parentType == null || (parentType.equals(bc.getCalledMethodsClass()))) {
            // parent is initialized outside of the CUT. For instance, System.out variable in printing.
            recordInvocation(bc, bc.getCalledMethodsClass(), variableName, callSequencesOfCurrentMethod);
        } else if (callSequencesOfCurrentMethod.get(parentType).containsKey(variableName)) {
            // The object is initialized by another class. We will use this type for recording this invocation.
            recordInvocation(bc, parentType, variableName, callSequencesOfCurrentMethod);
        } else {
            LOG.error("Cannot detect the right call sequence for recording: {}", bc);
//            throw new IllegalStateException("Could not detect the right call sequence ofr recording " + bc);
        }
    }

    private String detectParentType(String branch, Map<String, Map<String, List<MethodCall>>> callSequencesOfCurrentMethod) {
        String parentType = null;
        for (Map.Entry<String, Map<String, List<MethodCall>>> CSEntry : callSequencesOfCurrentMethod.entrySet()) {
            Map<String, List<MethodCall>> sequences = CSEntry.getValue();
            if (sequences.containsKey(branch)) {
                // We found our type. We must achieve to this branch
                parentType = CSEntry.getKey();
//                LOG.info("parent type " + parentType);
                break;
            }
        }
        return parentType;
    }

    private void handleStaticMethodCallInvocation(BytecodeInstruction bc, Map<String, Map<String, List<MethodCall>>> callSequencesOfCurrentMethod) {
        LOG.debug("Detect a static call. Adding it to the static call sequence.");
        recordInvocation(bc, bc.getCalledMethodsClass(), "static", callSequencesOfCurrentMethod);
    }

    private void handleConstructorInvocation(BytecodeInstruction bc, RawControlFlowGraph cfg, Map<String, Map<String, List<MethodCall>>> callSequencesOfCurrentMethod) {
        String newVariableName = getNewVariableName(bc, cfg);
        if (newVariableName.length() > 0) {
            // Here, we need to add a new call sequence to the methodCallSequences
            recordInvocation(bc, bc.getCalledMethodsClass(), newVariableName, callSequencesOfCurrentMethod);
        } else {
            LOG.debug("Could not find a new variable. It is not a call sequence.");
        }

    }

    private void recordInvocation(BytecodeInstruction bc, String clazz, String branch, Map<String, Map<String, List<MethodCall>>> callSequencesOfCurrentMethod) {

        // Check if we already had this class in this method
        if (!callSequencesOfCurrentMethod.containsKey(clazz)) { // If we did not see it, we would add its key to  methodCallSequences
            Map<String, List<MethodCall>> newSequence = new HashMap<String, List<MethodCall>>();
            callSequencesOfCurrentMethod.put(clazz, newSequence);
            LOG.debug("add new class to callSequencesOfCurrentMethod: " + clazz);
        }


        // Check if we already had this branch in the selected class
        if (!callSequencesOfCurrentMethod.get(clazz).containsKey(branch)) {
            callSequencesOfCurrentMethod.get(clazz).put(branch, new LinkedList<MethodCall>());
            LOG.debug("add new branch to class {}: {}", clazz, branch);
        }

        // Add the call sequence
        callSequencesOfCurrentMethod.get(clazz).get(branch).add(new MethodCall(bc));
        LOG.debug("Add a new method call to a call sequence. Class: {}, Branch: {}, bc: {}", clazz, branch, bc);

        setOldBC(bc, clazz, branch);

    }

    private String getNewVariableName(BytecodeInstruction bc, RawControlFlowGraph cfg) {
        String newVariableName = ""; // If this value remains unchanged, we will not do anything

        // Try to change newVariableSlot
        int id = bc.getInstructionId() + 1;
        BytecodeInstruction next = cfg.getInstruction(id);
        if (next.getInstructionType().equals("ASTORE")) {
            // Only here we change the newVariableSlot
            newVariableName = next.getVariableName();
        } else {
            if (!next.getInstructionType().equals("ALOAD")) {
                LOG.warn("The returned value did not stored: "+next.explain());
            }
        }
        return newVariableName;
    }


    private void setOldBC(BytecodeInstruction bc, String objectName, String branchName) {
        oldBC = bc;
        oldBCBranch = branchName;
        oldBCObject = objectName;
    }

    private void clearOldBC() {
        oldBC = null;
        oldBCBranch = null;
        oldBCObject = null;
    }


    public LinkedList<String> getTestSuite(){
        return this.testSuite;
    }

    public Map<String,List<String>> getObjectsTests(){
        return this.objectsTests;
    }
}
