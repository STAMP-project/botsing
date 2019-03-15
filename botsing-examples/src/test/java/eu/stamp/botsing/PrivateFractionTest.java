package eu.stamp.botsing;

import org.junit.Test;

import static org.junit.Assert.*;

public class PrivateFractionTest {

    @Test
    public void getShiftedValue() {
        double value = PrivateFraction.getShiftedValue(6,1);
        assertEquals(3, value, 0.0001);
    }

    @Test (expected = IllegalArgumentException.class)
    public void getShiftedValue_zero() {
        double value = PrivateFraction.getShiftedValue(6,0);
    }

    @Test (expected = ArithmeticException.class)
    public void getShiftedValue_crash() {
        double value = PrivateFraction.getShiftedValue(6,-1);
    }
}