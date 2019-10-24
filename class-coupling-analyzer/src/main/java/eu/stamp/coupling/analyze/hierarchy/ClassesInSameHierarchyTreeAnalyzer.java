package eu.stamp.coupling.analyze.hierarchy;

import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import eu.stamp.coupling.analyze.Analyzer;
import eu.stamp.coupling.analyze.calls.ClassPair;
import org.evosuite.graphs.GraphPool;
import org.evosuite.graphs.ccg.ClassCallGraph;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.RawControlFlowGraph;
import org.evosuite.setup.InheritanceTree;
import org.evosuite.setup.InheritanceTreeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ClassesInSameHierarchyTreeAnalyzer extends Analyzer {
    private static final Logger LOG = LoggerFactory.getLogger(ClassesInSameHierarchyTreeAnalyzer.class);
    private InheritanceTree inheritanceTree;
    // <SuperClass,<SubClass,scores>>
    private HashMap<String,HashMap<String, CouplingScores>> superClassesMap = new HashMap<>();

    public ClassesInSameHierarchyTreeAnalyzer(List<String> classPathEntries, String projectPrefix) {
        super(classPathEntries,projectPrefix);

        LOG.info("Initializing the inheritance tree ...");
        inheritanceTree = InheritanceTreeGenerator.createFromClassPath(this.classPathEntries);

        LOG.info("Initializing the super class map ...");
        initializeMap();
    }

    private void initializeMap() {
        superClassesMap.clear();
        for (String subClass : interestingClasses){
            for(String superClass : inheritanceTree.getSuperclasses(subClass)){
                if(!superClass.equals(subClass) && interestingClasses.contains(superClass)){

                    if(!superClassesMap.containsKey(subClass)){
                        superClassesMap.put(subClass, new HashMap<>());
                    }

                    superClassesMap.get(subClass).put(superClass,new CouplingScores(0,0));
                }
            }
        }
    }

    @Override
    public void execute() {
        LOG.info("Start analyzing {} classes.", interestingClasses.size());
        GraphPool graphPool = GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());

        for(String subClass : superClassesMap.keySet()){

            // If evosuite can not instrument a class, we will skip it.
            if(graphPool.getRawCFGs(subClass) == null){
                LOG.warn("sub class {} does not have the control flow graph.",subClass);
                continue;
            }

            detectSuperClassesForSingleClass(subClass);


        }

        LOG.info("Static analysis has been finished. Preparing the final list ...");
        // Clean super map from the 0 values
        removeZeroScores();
        // Create the final list
        createFinalList();

        LOG.info("Sorting the final list ...");
        List<ClassPair> paretoFront = collectParetoFront();
        finalList.clear();
        finalList.addAll(paretoFront);
        LOG.info("Final list is ready.");

    }


    private void detectSuperClassesForSingleClass(String subClass){
        GraphPool graphPool = GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
        Map<String,RawControlFlowGraph> subClassCFGs = graphPool.getRawCFGs(subClass);
        ClassCallGraph subClassCallGraph = new ClassCallGraph(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT(),subClass);
        for(String superClass : superClassesMap.get(subClass).keySet()){
            // If evosuite can not instrument a class, we will skip it.
            if(graphPool.getRawCFGs(superClass) == null){
                LOG.warn("super class {} does not have the control flow graph.",superClass);
                continue;
            }

            Map<String,RawControlFlowGraph> superClassCFGs = graphPool.getRawCFGs(superClass);
            ClassCallGraph superClassCallGraph = new ClassCallGraph(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT(),superClass);

            // Iterate on bytecode instructions of super class
            for(String methodName : superClassCFGs.keySet()){
                RawControlFlowGraph methodCFG = superClassCFGs.get(methodName);
                for(BytecodeInstruction callInst : methodCFG.determineMethodCalls()){
                    // Skip irrelevant method calls
                    if(!callInst.getCalledMethodsClass().equals(superClass) && !callInst.getCalledMethodsClass().equals(subClass)){
                        continue;
                    }

                    String calledMethod = callInst.getCalledMethod();

                    // Skip if subClass does not override the calledMethod
                    if(subClassCallGraph.getNodeByMethodName(calledMethod) == null){
                        continue;
                    }

                    // Here, we know that this bytecode instruction is a call to subClass
                    // So, we increase the score of super class in the map.
                    superClassesMap.get(subClass).get(superClass).increaseSuperClassScore();
                }
            }

            // Iterate on bytecode instructions of sub class
            for(String methodName : subClassCFGs.keySet()){
                RawControlFlowGraph methodCFG = subClassCFGs.get(methodName);
                for(BytecodeInstruction callInst : methodCFG.determineMethodCalls()){
                    // Skip irrelevant method calls
                    if(!callInst.getCalledMethodsClass().equals(superClass) && !callInst.getCalledMethodsClass().equals(subClass)){
                        continue;
                    }

                    String calledMethod = callInst.getCalledMethod();

                    // Skip if subClass contains the called method
                    if(subClassCallGraph.getNodeByMethodName(calledMethod) != null){
                        continue;
                    }

                    // Skip if super class does not contain the called method
                    if(superClassCallGraph.getNodeByMethodName(calledMethod) == null){
                        continue;
                    }

                    // Here, we know that this bytecode instruction is a call to superClass
                    // So, we increase the score of sub class in the map.
                    superClassesMap.get(subClass).get(superClass).increaseSubClassScore();
                }
            }
        }
    }

    private void createFinalList() {
        finalList.clear();
        for(String subClass: superClassesMap.keySet()){
            for(String superClass : superClassesMap.get(subClass).keySet()){
                CouplingScores scores = superClassesMap.get(subClass).get(superClass);
                ClassPair newPair = new ClassPair(subClass,superClass,scores.subClassScore,scores.superClassScore);
                addBranchScores(newPair,subClass,superClass);
                // skip cases which has at least one class without branch
                if(newPair.getNumberOfBranchesInClass1() == 0 || newPair.getNumberOfBranchesInClass2() == 0){
                    continue;
                }


                if(!finalList.contains(newPair)){
                    finalList.add(newPair);
                }else{
                    LOG.debug("{} and {} pair is already added.",subClass,superClass);
                }
            }
        }
    }

    private void removeZeroScores() {
        Iterator<String> mapIterator = superClassesMap.keySet().iterator();
        while (mapIterator.hasNext()) {
            String subClass = mapIterator.next();
            Iterator<String> superClassesIterator = superClassesMap.get(subClass).keySet().iterator();
            while (superClassesIterator.hasNext()){
                String superClass = superClassesIterator.next();
                CouplingScores scores = superClassesMap.get(subClass).get(superClass);
                if(scores.isZero()){
                    superClassesIterator.remove();
                }
            }
            if(superClassesMap.get(subClass).isEmpty()){
                mapIterator.remove();
            }
        }
    }

    public void execute(String targetClass) {
        GraphPool graphPool = GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
        // If evosuite can not instrument a class, we will skip it.
        if(graphPool.getRawCFGs(targetClass) == null){
            LOG.warn("sub class {} does not have the control flow graph.",targetClass);
            return;
        }

        detectSuperClassesForSingleClass(targetClass);
    }
}
