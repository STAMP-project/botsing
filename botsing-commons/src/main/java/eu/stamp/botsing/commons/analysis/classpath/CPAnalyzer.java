package eu.stamp.botsing.commons.analysis.classpath;


import org.evosuite.setup.InheritanceTree;
import org.evosuite.setup.InheritanceTreeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class CPAnalyzer {
    private static final Logger LOG = LoggerFactory.getLogger(CPAnalyzer.class);

    private static InheritanceTree inheritanceTree = null;

    public static void analyzeClass( List<String> classPath) throws RuntimeException{
        initInheritanceTree(classPath);
    }

    private static void initInheritanceTree(List<String> classPath) {
        LOG.info("Calculate inheritance hierarchy"+classPath.toString());
        inheritanceTree = InheritanceTreeGenerator.createFromClassPath(classPath);
        InheritanceTreeGenerator.gatherStatistics(inheritanceTree);
    }


    public static InheritanceTree getInheritanceTree(){
        return inheritanceTree;
    }

}

