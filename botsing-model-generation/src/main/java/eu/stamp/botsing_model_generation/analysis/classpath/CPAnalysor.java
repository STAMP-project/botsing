package eu.stamp.botsing_model_generation.analysis.classpath;

import org.evosuite.TestGenerationContext;
import org.evosuite.setup.DependencyAnalysis;
import org.evosuite.setup.InheritanceTree;
import org.evosuite.setup.InheritanceTreeGenerator;
import org.evosuite.setup.TestClusterGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class CPAnalysor {
    private static final Logger LOG = LoggerFactory.getLogger(CPAnalysor.class);

    private static InheritanceTree inheritanceTree = null;

    public static void analyzeClass( List<String> classPath) throws RuntimeException,
            ClassNotFoundException {
        initInheritanceTree(classPath);
    }

    private static void initInheritanceTree(List<String> classPath) {
        LOG.info("Calculate inheritance hierarchy"+classPath.toString());
        inheritanceTree = InheritanceTreeGenerator.createFromClassPath(classPath);
        TestClusterGenerator clusterGenerator = new TestClusterGenerator(inheritanceTree);
        TestGenerationContext.getInstance().setTestClusterGenerator(clusterGenerator);
        InheritanceTreeGenerator.gatherStatistics(inheritanceTree);
    }


    public static InheritanceTree getInheritanceTree(){
        return inheritanceTree;
    }

}

