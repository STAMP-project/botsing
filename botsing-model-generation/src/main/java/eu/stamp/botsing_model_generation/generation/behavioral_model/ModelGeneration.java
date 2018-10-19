package eu.stamp.botsing_model_generation.generation.behavioral_model;


import eu.stamp.botsing_model_generation.analysis.classpath.CPAnalysor;
import eu.stamp.botsing_model_generation.generation.behavioral_model.model.Model;
import org.evosuite.classpath.ClassPathHandler;
import org.evosuite.setup.InheritanceTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.List;


public class ModelGeneration {
    private static final Logger LOG = LoggerFactory.getLogger(ModelGeneration.class);

    private String[] projectClassPaths;
    public ModelGeneration(String cp){
        projectClassPaths=cp.split(File.pathSeparator);
    }
    public ModelGeneration(String[] jarsCp ){
        projectClassPaths = jarsCp.clone();
    }


    public Model generate(){
        if(projectClassPaths == null){
            LOG.error("Project classpath should be set before the model generation.");
            return null;
        }

        ClassPathHandler.getInstance().changeTargetClassPath(projectClassPaths);
        List<String> cpList = Arrays.asList(projectClassPaths);
        try {
            CPAnalysor.analyzeClass(cpList);
        } catch (ClassNotFoundException e) {
            LOG.error("The passed class could not be found! please revise your input.");
        }

        InheritanceTree projectTree = CPAnalysor.getInheritanceTree();
        LOG.info("number of classes: "+projectTree.getNumClasses());
        return null;
    }
}
