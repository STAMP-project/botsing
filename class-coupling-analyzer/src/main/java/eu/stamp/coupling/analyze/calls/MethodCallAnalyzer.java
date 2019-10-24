package eu.stamp.coupling.analyze.calls;

import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import eu.stamp.coupling.analyze.Analyzer;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.BytecodeInstructionPool;
import org.evosuite.setup.InheritanceTree;
import org.evosuite.setup.InheritanceTreeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.*;

public class MethodCallAnalyzer extends Analyzer {

    private static final Logger LOG = LoggerFactory.getLogger(MethodCallAnalyzer.class);

    private HashMap<String, HashMap<String, Integer>> callMap = new HashMap<>();

    public MethodCallAnalyzer(List<String> classPathEntries, String projectPrefix) {
        super(classPathEntries, projectPrefix);
        // Initialize map with 0 values in each cell
        initializeMap();
    }


    private void initializeMap() {
        callMap.clear();
        for (String callerClass : interestingClasses) {
            callMap.put(callerClass, new HashMap<>());
            for (String calleeClass : interestingClasses) {
                if (!callerClass.equals(calleeClass)) {
                    callMap.get(callerClass).put(calleeClass, 0);
                }
            }
        }
    }

    public void execute() {
        LOG.info("Start analyzing {} classes.", interestingClasses.size());
        // All of the bytecode instructions for the interesting classes should be stored in this pool.
        BytecodeInstructionPool bcInstPool = BytecodeInstructionPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());

        for (String classUnderAnalysis : interestingClasses) {
            // Check if bytecode instructions of classUnderAnalysis is available.
            if (!bcInstPool.knownClasses().contains(classUnderAnalysis)) {
                LOG.warn("Bytecode instructions of class {} is not available.", classUnderAnalysis);
                // If it is not available, we will skip analyzing it.
                continue;
            }
            List<BytecodeInstruction> instructions = bcInstPool.getInstructionsIn(classUnderAnalysis);

            for (BytecodeInstruction currentInst : instructions) {
                // Skip bytecode instructions which are not a method call
                if (!currentInst.isMethodCall()) {
                    continue;
                }

                String calledMethodClass = currentInst.getCalledMethodsClass();

                // Caller class and callee class should exists in the callMap. Otherwise, the call method is not interesting for us.
                if (!callMap.containsKey(classUnderAnalysis) || !callMap.get(classUnderAnalysis).containsKey(calledMethodClass)) {
                    continue;
                }

                // plus plus the value in the map
                int currentValue = callMap.get(classUnderAnalysis).get(calledMethodClass);
                callMap.get(classUnderAnalysis).put(calledMethodClass, currentValue + 1);
            }
        }

        LOG.info("Static analysis has been finished. Preparing the final list ...");
        prepareFinalList();


        LOG.info("Sorting the final list ...");
        List<ClassPair> paretoFront = collectParetoFront();
        finalList.clear();
        finalList.addAll(paretoFront);
        LOG.info("Final list is ready.");
    }

    private void prepareFinalList() {
        finalList.clear();
        for (String class1 : interestingClasses) {
            for (String class2 : interestingClasses) {
                // Skip same classes
                if (class1.equals(class2)) {
                    continue;
                }

                int score1 = callMap.get(class1).get(class2);
                int score2 = callMap.get(class2).get(class1);

                // We are not interested on class pairs which do not have any coupling
                if (score1 + score2 == 0) {
                    continue;
                }

                ClassPair currentPair = new ClassPair(class1, class2, score1, score2);
                addBranchScores(currentPair, class1, class2);
                // skip cases which has at least one class without branch
                if (currentPair.getNumberOfBranchesInClass1() == 0 || currentPair.getNumberOfBranchesInClass2() == 0) {
                    continue;
                }

                if (!finalList.contains(currentPair)) {
                    finalList.add(currentPair);
                } else {
                    LOG.debug("{} and {} pair is already added.", class1, class2);
                }
            }
        }
    }

    @Override
    public void execute(String targetClass) {
        LOG.info("Start analyzing {} classes for target class {}.", interestingClasses.size(), targetClass);
        if (!interestingClasses.contains(targetClass)) {
            throw new IllegalArgumentException("Target class is not valid");
        }
        // All of the bytecode instructions for the interesting classes should be stored in this pool.
        BytecodeInstructionPool bcInstPool = BytecodeInstructionPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
        LOG.info("Initializing the inheritance tree ...");
        InheritanceTree inheritanceTree = InheritanceTreeGenerator.createFromClassPath(this.classPathEntries);
        List<String> targetClasses = new ArrayList<>();
        for (String superClass : inheritanceTree.getSuperclasses(targetClass)) {
            if (this.interestingClasses.contains(superClass)) {
                targetClasses.add(superClass);
            }
        }


        if (bcInstPool.knownClasses().contains(targetClass)) {

            // analyzing bytecodes in the other classes in the project which are using the target class
            for (String classUnderAnalysis : interestingClasses) {

                if (classUnderAnalysis.equals(targetClass)) {
                    continue;
                }

                // class under analysis should not be a private inner class

                if (!bcInstPool.knownClasses().contains(classUnderAnalysis)) {
                    LOG.warn("Bytecode instructions of class {} is not available.", classUnderAnalysis);
                    // If it is not available, we will skip analyzing it.
                    continue;
                }

                List<BytecodeInstruction> instructions = bcInstPool.getInstructionsIn(classUnderAnalysis);

                for (BytecodeInstruction currentInst : instructions) {
                    // Skip bytecode instructions which are not a method call
                    if (!currentInst.isMethodCall()) {
                        continue;
                    }

                    // Skip bytecode instructions which are not calling a method from target class
                    String calledMethodClass = currentInst.getCalledMethodsClass();

                    if (!targetClasses.contains(calledMethodClass)) {
                        continue;
                    }


                    // Caller class and callee class should exists in the callMap. Otherwise, the call method is not interesting for us.
                    if (!callMap.containsKey(classUnderAnalysis) || !callMap.get(classUnderAnalysis).containsKey(calledMethodClass)) {
                        continue;
                    }

                    // plus plus the value in the map
                    int currentValue = callMap.get(classUnderAnalysis).get(calledMethodClass);
                    callMap.get(classUnderAnalysis).put(targetClass, currentValue + 1);
                }

            }


        } else {
            LOG.error("bytecodes of target class is not available");
        }

        LOG.info("Static analysis has been finished. Preparing the final list ...");
        prepareFinalList();
        LOG.info("Sorting the final list ...");
    }


}
