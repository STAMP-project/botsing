package eu.stamp.coupling.analyze.calls;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ClassPairTest {

    String class1 = "eu.stamp.example.class1";
    String class2 = "eu.stamp.example.class2";
    ClassPair classPair;

    @Before
    public void before(){
        classPair = newClassPair();
    }

    @Test
    public void testClassGetter(){
        Assert.assertEquals(class1, classPair.getClass1());
        Assert.assertEquals(class2, classPair.getClass2());
    }

    @Test
    public void testScoreGetters(){
        Assert.assertEquals(1, classPair.getCallScore1());
        Assert.assertEquals(5, classPair.getCallScore2());
        Assert.assertEquals(6, classPair.getTotalScore());
    }

    @Test
    public void testBranchNumber(){
        classPair.addTonumberOfBranches(class1, 3);
        classPair.addTonumberOfBranches(class1, 2);
        classPair.addTonumberOfBranches(class2, 1);
        Assert.assertEquals(5, classPair.getNumberOfBranchesInClass1());
        Assert.assertEquals(1, classPair.getNumberOfBranchesInClass2());
    }

    @Test
    public void testBranchNumberAdder(){
        try {
            classPair.addTonumberOfBranches("",2);
            fail("IllegalStateException expected!");
        }catch(IllegalStateException e){
            assertTrue(e.getMessage().contains("Class is not available!"));
        }
    }

    @Test
    public void testBranchNumberAdder2(){
        classPair.addTonumberOfBranches(class1,-1);
        Assert.assertEquals(0, classPair.getNumberOfBranchesInClass1());
        Assert.assertEquals(0, classPair.getNumberOfBranchesInClass2());
    }

    @Test
    public void testListOfObjectives(){
        List<Integer> expectedReult = Arrays.asList(
                new Integer[]{1, 5, 0, 0 });

        Assert.assertEquals(expectedReult, classPair.getListOfObjectivesValue());
    }

    @Test
    public void testEqualsMethod(){
        Assert.assertFalse(classPair.equals(null));
        Assert.assertFalse(classPair.equals(new String()));

        // Compare with real classPair objects
        ClassPair classPair2 = new ClassPair("eu.stamp.example.classx",class2,1, 5);
        Assert.assertFalse(classPair.equals(classPair2));

        classPair2 = new ClassPair(class1, "eu.stamp.example.classx",1, 5);
        Assert.assertFalse(classPair.equals(classPair2));

        classPair2 = new ClassPair(class1,class2,0, 0);
        Assert.assertTrue(classPair.equals(classPair2));
    }

    @Test
    public void testCompareToMethod(){

        try{
            Assert.assertEquals(1, classPair.compareTo(null));
            fail("NullPointerException was expected!");
        }catch (NullPointerException npe){
            // do nothing
        }

        ClassPair classPair2 = new ClassPair(class1,class2,0, 0);
        Assert.assertTrue( classPair.compareTo(classPair2) > 0);

        classPair2 = new ClassPair(class1,class2,10, 10);
        Assert.assertTrue( classPair.compareTo(classPair2) < 0);


        classPair2 = newClassPair();
        Assert.assertTrue( classPair.compareTo(classPair2) == 0);


        classPair2 = new ClassPair(class1,class2,10, 0);
        Assert.assertTrue( classPair.compareTo(classPair2) == 0);


    }


    private ClassPair newClassPair(){
        return new ClassPair(class1, class2, 1,5);
    }

}
