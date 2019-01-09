package eu.stamp.botsing.model.generation.analysis.testcases;

import eu.stamp.botsing.model.generation.callsequence.CallSequencesPoolManager;
import eu.stamp.botsing.model.generation.callsequence.MethodCall;
import eu.stamp.botsing.model.generation.testcase.carving.CarvingManager;
import org.evosuite.Properties;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.statements.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class DynamicAnalyser {
    private static final Logger LOG = LoggerFactory.getLogger(DynamicAnalyser.class);

    public void analyse(Map<String, List<String>> objectsTests, ArrayList<String> involvedObjects){
        List<String> testSuites =  new ArrayList<String>();
        if(involvedObjects.size()>0){
            for(String involvedObj: involvedObjects){
                if(objectsTests.containsKey(involvedObj)){
                    for(String candidateTS: objectsTests.get(involvedObj)){
                        testSuites.add(candidateTS);
                    }
                }
            }
        }else{
            for (Map.Entry<String, List<String>> entry : objectsTests.entrySet()){
                for(String test: entry.getValue()){
                    if(!testSuites.contains(test)){
                        testSuites.add(test);
                    }
                }
            }
        }


        if(testSuites.size()>0){
            CarvingManager manager = CarvingManager.getInstance();
            Properties.SELECTED_JUNIT = String.join(":", testSuites);
            Map<Class<?>, List<TestCase>> carvedTestCases = manager.getCarvedTestCases();
            LOG.info("Test carving is finished.");
            LOG.info("Collecting the call sequences.");
            savingMethodCallSequences(carvedTestCases);
        }else{
            LOG.info("No test suite detected for dynamic analysis!");
        }
        // Execute the test cases in the detected test suite, and run the dynamic analysis on them.
    }

    private void savingMethodCallSequences(Map<Class<?>, List<TestCase>> carvedTestCases){
        for (Class<?> targetClass : carvedTestCases.keySet()) {
            String className = targetClass.getName();
            for (TestCase test : carvedTestCases.get(targetClass)) {
                for (List<MethodCall> callSequence : getCallSequences(test)){
                    CallSequencesPoolManager.getInstance().addSequence(className,callSequence);
                }
            }
        }
    }

    private List<List<MethodCall>> getCallSequences(TestCase test) {
        List<List<MethodCall>> result = new LinkedList<>();
        boolean observedInit = false;

        List<MethodCall> callSequence =  new LinkedList<MethodCall>();
        for (int i = 0; i < test.size(); i++) {
            Statement statement = test.getStatement(i);
            if(validForCallSequence(statement)){
                if (statement.getAccessibleObject().isConstructor()){
                    if (observedInit){
                        result.add(callSequence);
                        callSequence =  new LinkedList<MethodCall>();
                    }else{
                        observedInit = true;
                    }
                }
                callSequence.add(new MethodCall(statement));
            }
        }
        result.add(callSequence);
        return result;
    }

    private boolean validForCallSequence(Statement statement) {
        return statement.getClass().getSimpleName().equals("MethodStatement") || statement.getClass().getSimpleName().equals("ConstructorStatement");
    }

}
