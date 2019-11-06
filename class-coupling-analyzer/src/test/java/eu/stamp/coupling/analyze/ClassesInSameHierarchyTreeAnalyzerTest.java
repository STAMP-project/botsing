package eu.stamp.coupling.analyze;

import eu.stamp.coupling.analyze.calls.ClassPair;
import eu.stamp.coupling.analyze.hierarchy.ClassesInSameHierarchyTreeAnalyzer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ClassesInSameHierarchyTreeAnalyzerTest {

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
    public void testClassesInSameHierarchyTreeAnalyzer_noInterestingClass(){
        Analyzer analyzer = new ClassesInSameHierarchyTreeAnalyzer(cps,"eu.stamp.botsing.coupling");
        analyzer.execute();
        Assert.assertEquals(0, analyzer.getFinalList().size());
    }

    @Test
    public void testClassesInSameHierarchyTreeAnalyzer_NotComplexEnough(){
        Analyzer analyzer = new ClassesInSameHierarchyTreeAnalyzer(cps,"eu.stamp.botsing.poly");
        analyzer.execute("eu.stamp.botsing.poly.LessComplexSubClass");
        Assert.assertEquals(0, analyzer.getFinalList().size());
    }

    @Test
    public void testClassesInSameHierarchyTreeAnalyzer(){
        Analyzer analyzer = new ClassesInSameHierarchyTreeAnalyzer(cps,"eu.stamp.botsing.poly");
        analyzer.execute();
        List<ClassPair> finalResult = analyzer.getFinalList();
        Assert.assertEquals(1, finalResult.size());

        ClassPair pair = finalResult.get(0);
        Assert.assertEquals(5, pair.getTotalScore());
        Assert.assertEquals(14, pair.getNumberOfBranchesInClass1()+pair.getNumberOfBranchesInClass2());
    }
}
