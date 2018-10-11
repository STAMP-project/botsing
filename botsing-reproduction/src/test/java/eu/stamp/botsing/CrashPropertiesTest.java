package eu.stamp.botsing;

import org.apache.commons.cli.*;
import org.evosuite.Properties;
import org.junit.Test;

import static org.junit.Assert.*;

public class CrashPropertiesTest {

    @Test
    public void testSetupStackTrace() throws ParseException {
        CrashProperties properties = CrashProperties.getInstance();
        //assertNull(properties.getProjectClassPaths());

        StackTrace crash = properties.getStackTrace();
        assertNull(crash.getExceptionType());
        assertNull(crash.getFrames());
        assertNull(crash.getTargetClass());
    }

    @Test
    public void testGetStringValue() throws Properties.NoSuchParameterException, IllegalAccessException {
        String value = CrashProperties.getInstance().getStringValue("D");
        System.out.println(value);
    }

    @Test
    public void testProperties() throws Properties.NoSuchParameterException, IllegalAccessException {
        int popsize = CrashProperties.getInstance().getIntValue("population");
        assertEquals(100, popsize);

        long budget = CrashProperties.getInstance().getLongValue("search_budget");
        assertEquals(1800, budget);

        boolean bool = CrashProperties.getInstance().getBooleanValue("sandbox");
        assertEquals(true, bool);

        Properties.StoppingCondition cond = CrashProperties.getInstance().getStoppingCondition();
        assertEquals("MAXTIME",cond.toString());
    }

    @Test
    public void testSetClasspath() throws Properties.NoSuchParameterException, IllegalAccessException {
        CrashProperties properties = CrashProperties.getInstance();
        properties.setClasspath("jar1:jar2");
        String[] jars = properties.getProjectClassPaths();

        assertEquals("jar1", jars[0]);
        assertEquals("jar2", jars[1]);
    }

}