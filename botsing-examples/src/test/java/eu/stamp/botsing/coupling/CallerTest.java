package eu.stamp.botsing.coupling;

import org.junit.Test;

public class CallerTest {

    @Test
    public void test0(){
        Caller caller = new Caller(10,20);
        caller.firstNumberIsBigger();
    }
}
