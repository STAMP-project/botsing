package eu.stamp.botsing.commons.analysis.classpath;

import org.evosuite.classpath.ClassPathHandler;
import org.evosuite.setup.InheritanceTree;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class CPAnalyzerTest {

    List<String> cps = new ArrayList();;

    @Before
    public void before(){
        // setup of the directory with file *class and file *.log
        String user_dir = System.getProperty("user.dir"); // the current directory is the module
        // <b>botsing-reproduction</b>
        File file = new File(user_dir);
        String base_dir = Paths.get(file.getParent(), "botsing-examples").toString(); // the crash to replicate is
        // Prepare projectCP
        String projectCP = Paths.get(base_dir, "target", "classes").toString();
        cps.add(projectCP);
    }

    @Test
    public void testInheritanceTreeGeneration(){
        ClassPathHandler.getInstance().changeTargetClassPath(cps.toArray(new String[cps.size()]));
        CPAnalyzer.analyzeClass(cps);
        InheritanceTree inheritanceTree = CPAnalyzer.getInheritanceTree();
        assert(inheritanceTree.hasClass("eu.stamp.botsing.Fraction"));
        assert(inheritanceTree.hasClass("eu.stamp.botsing.poly.SuperClass"));
        assert(inheritanceTree.hasClass("eu.stamp.botsing.coupling.Caller"));
        assert(inheritanceTree.getSuperclasses("eu.stamp.botsing.poly.SubClass").contains("eu.stamp.botsing.poly.SuperClass"));
    }
}
