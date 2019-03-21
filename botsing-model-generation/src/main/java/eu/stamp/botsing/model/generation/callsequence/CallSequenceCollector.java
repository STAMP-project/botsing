package eu.stamp.botsing.model.generation.callsequence;


import eu.stamp.botsing.model.generation.analysis.classpath.CPAnalysor;
import eu.stamp.botsing.model.generation.analysis.sourcecode.StaticAnalyser;
import eu.stamp.botsing.model.generation.analysis.testcases.DynamicAnalyser;
import eu.stamp.botsing.model.generation.testusage.TestUsagePoolManager;
import org.evosuite.classpath.ClassPathHacker;
import org.evosuite.classpath.ClassPathHandler;
import org.evosuite.setup.InheritanceTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class CallSequenceCollector {

    private static final Logger LOG = LoggerFactory.getLogger(CallSequenceCollector.class);

    StaticAnalyser staticAnalyser = new StaticAnalyser();
    DynamicAnalyser dynamicAnalyser = new DynamicAnalyser();
    private String[] projectClassPaths;


    public CallSequenceCollector(String cp) {
        this(cp.split(File.pathSeparator));
    }

    public CallSequenceCollector(String[] jarsCp) {
        projectClassPaths = jarsCp.clone();
    }

    public void collect(String targetClassIndicator, String outputFolder, List<String> involvedObejcts,
                        Boolean isPrefix) {

        //pre-processes before starting the analysis
        if(projectClassPaths == null) {
            LOG.error("Project classpath should be set before the model generation.");
        }
        // Class path handler
        handleClassPath();

        // Static Analysis
        List<String> interestingClasses = detectInterestingClasses(targetClassIndicator, isPrefix);
        //        generateCFGS();
        staticAnalyser.analyse(interestingClasses);

        // Dynamic Analysis
        dynamicAnalyser.analyse(staticAnalyser.getObjectsTests(), involvedObejcts);

        // Storing the object usage of test suites to the output directory
        TestUsagePoolManager.getInstance().savingTestsUsages(Paths.get(outputFolder, "carvedTests").toString());
        // Reporting the collected call sequences
        if(LOG.isDebugEnabled()) {
            CallSequencesPoolManager.getInstance().report();
        }
    }


    private void handleClassPath() {
        ClassPathHandler.getInstance().changeTargetClassPath(projectClassPaths);
        List<String> cpList = Arrays.asList(projectClassPaths);
        for(String cp : cpList) {
            try {
                ClassPathHacker.addFile(cp);
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
        CPAnalysor.analyzeClass(cpList);
    }


    private List<String> detectInterestingClasses(String targetClassIndicator, Boolean isPrefix) {
        List<String> interestingClasses = new ArrayList<String>();
        InheritanceTree projectTree = CPAnalysor.getInheritanceTree();
        if(isPrefix) {
            for(String clazz : projectTree.getAllClasses()) {
                if(clazz.startsWith(targetClassIndicator)) {
                    interestingClasses.add(clazz);
                }
            }
        } else {
            for(String clazz : projectTree.getAllClasses()) {
                if(clazz.contains("." + targetClassIndicator + ".")) {
                    interestingClasses.add(clazz);
                }
            }
        }
        return interestingClasses;
    }


}
