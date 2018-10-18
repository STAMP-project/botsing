package au.stamp.botsing;

import eu.stamp.botsing.Fraction;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FractionTest {

    @Test (expected = java.lang.ArithmeticException.class)
    public void getShiftedValue_crash() {
        Fraction f = new Fraction(1,2);
        f.getShiftedValue(-2);
    }

    @Test (expected = java.lang.IllegalArgumentException.class)
    public void getValue_zeroCase(){
        Fraction f = new Fraction(1,0);
        f.getValue();
    }

    @Test
    public void getValue(){
        Fraction f = new Fraction(1,2);
        double value = f.getValue();
        assertEquals (0.5, value, 0.0000001);
    }

    @Test
    public void getShiftedValue(){
        Fraction f = new Fraction(1,2);
        double value = f.getShiftedValue(1);
        assertEquals (0.0, value, 0.0000001);
    }

    @Test (expected = java.lang.IllegalArgumentException.class)
    public void getShiftedValue_zero(){
        Fraction f = new Fraction(1,0);
        double value = f.getShiftedValue(1);
    }

}