package eu.stamp.coupling.analyze.hierarchy;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CouplingScoresTest {

    CouplingScores couplingScores;

    @Before
    public void before(){
        couplingScores = new CouplingScores(2, 1);
    }

    @Test
    public void testIsZero(){
        Assert.assertTrue(couplingScores.isZero());
    }

    @Test
    public void testIncreaseMethods(){
        couplingScores.increaseSubClassScore();
        couplingScores.increaseSuperClassScore();
        Assert.assertFalse(couplingScores.isZero());
    }
}
