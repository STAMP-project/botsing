package eu.stamp.botsing.coupling.analyze;

import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import eu.stamp.botsing.commons.analysis.classpath.CPAnalyzer;
import eu.stamp.botsing.coupling.analyze.calls.ClassPair;
import org.evosuite.classpath.ClassPathHacker;
import org.evosuite.classpath.ClassPathHandler;
import org.evosuite.junit.CoverageAnalysis;
import org.evosuite.setup.InheritanceTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public abstract class Analyzer {

    private static final Logger LOG = LoggerFactory.getLogger(Analyzer.class);

    protected List<String> classPathEntries;
    protected String projectPrefix = "";
    protected String[] projectClassPaths;
    protected List<String> interestingClasses;

    protected List<ClassPair> finalList = new ArrayList<>();

    public Analyzer(List<String> classPathEntries, String projectPrefix) {
        this.classPathEntries = classPathEntries;
        this.projectPrefix = projectPrefix;
        projectClassPaths = classPathEntries.toArray(new String[classPathEntries.size()]);

        handleClassPath();
        // Collect interesting classes for analysis
        interestingClasses = detectInterestingClasses();
        // load classes and collect their Bytecode instrumentations into evosuite's BytecodeInstructionPool.
        loadNonTestClasses(interestingClasses);
    }

    public abstract void execute();


    protected List<String> detectInterestingClasses() {
        List<String> interestingClasses = new ArrayList<String>();
        InheritanceTree projectTree = CPAnalyzer.getInheritanceTree();
            for(String clazz : projectTree.getAllClasses()) {
                if(clazz.startsWith(projectPrefix)) {
                    interestingClasses.add(clazz);
                }
            }

        return interestingClasses;
    }


    protected void handleClassPath() {
        ClassPathHandler.getInstance().changeTargetClassPath(projectClassPaths);
        List<String> cpList = Arrays.asList(projectClassPaths);
        for(String cp : cpList) {
            try {
                ClassPathHacker.addFile(cp);
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
        CPAnalyzer.analyzeClass(cpList);
    }


    private void loadNonTestClasses(List<String> interestingClasses) {
        Iterator<String> classesIterator = interestingClasses.iterator();

        while(classesIterator.hasNext()) {
            String clazz = classesIterator.next();
            try {
                // instrument clazz
                Class<?> cls = Class.forName(clazz, false, BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
                // remove it from interesting classes if it is a test.
                if(CoverageAnalysis.isTest(cls)) {
                    classesIterator.remove();
                }

            } catch(ClassNotFoundException | NoClassDefFoundError e) {
                //                e.printStackTrace();
                LOG.warn("Error while loading {}", clazz, e);
            }
        }

    }

    public List<ClassPair> getFinalList() {
        return finalList;
    }
}
