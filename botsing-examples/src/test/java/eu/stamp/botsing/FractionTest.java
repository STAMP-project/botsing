package eu.stamp.botsing;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

public class FractionTest {

    private static final Logger LOG = LoggerFactory.getLogger(FractionTest.class);

    @Rule
    public TestRule watcher = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            LOG.info(String.format("Starting test: %s()...",
                    description.getMethodName()));
        }
    };

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