package eu.stamp.coupling.analyze;

import eu.stamp.coupling.analyze.calls.ClassPair;
import eu.stamp.coupling.analyze.calls.MethodCallAnalyzer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class MethodCallAnalyzerTest {

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
    public void testMethodCallAnalyzer(){
        // Execute methodCall analyzer
        Analyzer methodCallAnalyzer = new MethodCallAnalyzer(cps,"eu.stamp.botsing.coupling");
        methodCallAnalyzer.execute();
        List<ClassPair> finalResult = methodCallAnalyzer.getFinalList();

        Assert.assertEquals(2, finalResult.size());

        ClassPair pair1 = finalResult.get(0);
        ClassPair pair2 = finalResult.get(1);
        if(pair1.getClass1()== "eu.stamp.botsing.coupling.Caller" || pair1.getClass2() == "eu.stamp.botsing.coupling.Caller"){
            Assert.assertEquals(6, pair1.getNumberOfBranchesInClass1()+pair1.getNumberOfBranchesInClass2());
            Assert.assertEquals(2, pair2.getNumberOfBranchesInClass1()+pair2.getNumberOfBranchesInClass2());
        }else {
            Assert.assertEquals(2, pair1.getNumberOfBranchesInClass1()+pair1.getNumberOfBranchesInClass2());
            Assert.assertEquals(6, pair2.getNumberOfBranchesInClass1()+pair2.getNumberOfBranchesInClass2());
        }
        Assert.assertEquals(2, pair1.getTotalScore());
        Assert.assertEquals(2, pair2.getTotalScore());

    }

    @Test
    public void testMethodCallAnalyzerWithTargetClass(){
        Analyzer methodCallAnalyzer = new MethodCallAnalyzer(cps,"eu.stamp.botsing.coupling");
        try{
            methodCallAnalyzer.execute("IllegalClass");
            Assert.fail("IllegalArgumentException is expected!");
        }catch (IllegalArgumentException e){
            Assert.assertTrue(e.getMessage().contains("Target class is not valid"));
        }

        methodCallAnalyzer.execute("eu.stamp.botsing.coupling.Callee");
        List<ClassPair> finalResult = methodCallAnalyzer.getFinalList();
        Assert.assertEquals(2, finalResult.size());

        ClassPair pair1 = finalResult.get(0);
        ClassPair pair2 = finalResult.get(1);
        if(pair1.getClass1()== "eu.stamp.botsing.coupling.Caller" || pair1.getClass2() == "eu.stamp.botsing.coupling.Caller"){
            Assert.assertEquals(6, pair1.getNumberOfBranchesInClass1()+pair1.getNumberOfBranchesInClass2());
            Assert.assertEquals(2, pair2.getNumberOfBranchesInClass1()+pair2.getNumberOfBranchesInClass2());
        }else {
            Assert.assertEquals(2, pair1.getNumberOfBranchesInClass1()+pair1.getNumberOfBranchesInClass2());
            Assert.assertEquals(6, pair2.getNumberOfBranchesInClass1()+pair2.getNumberOfBranchesInClass2());
        }
        Assert.assertEquals(2, pair1.getTotalScore());
        Assert.assertEquals(2, pair2.getTotalScore());


    }
}
