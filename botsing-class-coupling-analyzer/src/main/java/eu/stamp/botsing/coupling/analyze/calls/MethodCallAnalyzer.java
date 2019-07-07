package eu.stamp.botsing.coupling.analyze.calls;

import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import eu.stamp.botsing.coupling.analyze.Analyzer;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.BytecodeInstructionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.*;

public class MethodCallAnalyzer extends Analyzer {

    private static final Logger LOG = LoggerFactory.getLogger(MethodCallAnalyzer.class);

    private HashMap<String,HashMap<String, Integer>> callMap = new HashMap<>();

    public MethodCallAnalyzer(List<String> classPathEntries, String projectPrefix) {
        super(classPathEntries,projectPrefix);
        // Initialize map with 0 values in each cell
        initializeMap();
    }



    private void initializeMap() {
        callMap.clear();
        for (String callerClass : interestingClasses){
            callMap.put(callerClass,new HashMap<>());
            for(String calleeClass : interestingClasses){
                if(!callerClass.equals(calleeClass)){
                    callMap.get(callerClass).put(calleeClass,0);
                }
            }
        }
    }

    public void execute() {
        LOG.info("Start analyzing {} classes.", interestingClasses.size());
        // All of the bytecode instructions for the interesting classes should be stored in this pool.
        BytecodeInstructionPool bcInstPool = BytecodeInstructionPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());

        for (String classUnderAnalysis: interestingClasses){
            // Check if bytecode instructions of classUnderAnalysis is available.
            if(!bcInstPool.knownClasses().contains(classUnderAnalysis)){
                LOG.warn("Bytecode instructions of class {} is not available.",classUnderAnalysis);
                // If it is not available, we will skip analyzing it.
                continue;
            }
            List<BytecodeInstruction> instructions =bcInstPool.getInstructionsIn(classUnderAnalysis);

            for(BytecodeInstruction currentInst : instructions){
                // Skip methods which are not a method call
                if(!currentInst.isMethodCall()){
                    continue;
                }

                String calledMethodClass = currentInst.getCalledMethodsClass();

                // Caller class and callee class should exists in the callMap. Otherwise, the call method is not interesting for us.
                if(!callMap.containsKey(classUnderAnalysis) || !callMap.get(classUnderAnalysis).containsKey(calledMethodClass) ){
                    continue;
                }

                // plus plus the value in the map
                int currentValue = callMap.get(classUnderAnalysis).get(calledMethodClass);
                callMap.get(classUnderAnalysis).put(calledMethodClass,currentValue+1);
            }
        }

        LOG.info("Static analysis has been finished. Preparing the final list ...");
        finalList.clear();
        for (String class1 : interestingClasses){
            for(String class2 : interestingClasses){
                // Skip same classes
                if(class1.equals(class2)){
                    continue;
                }

                int score1 = callMap.get(class1).get(class2);
                int score2 = callMap.get(class2).get(class1);

                ClassPair currentPair = new ClassPair(class1,class2,score1,score2);

                // We are not interested on class pairs which do not have any coupling
                if(score1+score2 == 0){
                    continue;
                }

                if(!finalList.contains(currentPair)){
                    finalList.add(currentPair);
                }else{
                    LOG.debug("{} and {} pair is already added.",class1,class2);
                }
            }
        }

        LOG.info("Sorting the final list ...");
        Collections.sort(finalList);
        Collections.reverse(finalList);
        LOG.info("Final list is ready.");
    }



}
