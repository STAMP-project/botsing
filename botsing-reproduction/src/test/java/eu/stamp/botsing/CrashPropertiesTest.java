package eu.stamp.botsing;

import org.apache.commons.cli.*;
import org.evosuite.Properties;
import org.junit.Test;

import org.mockito.Mockito;

import java.util.ArrayList;


import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;

public class CrashPropertiesTest {

    @Test
    public void testSetupStackTrace() throws ParseException {
        CrashProperties properties = CrashProperties.getInstance();
        //assertNull(properties.getProjectClassPaths());
        properties.resetStackTrace();
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

    @ Test
    public void testGetTargetException() throws Exception{

        ArrayList<StackTraceElement> stackTrace = new ArrayList<StackTraceElement>();
        stackTrace.add(new StackTraceElement("eu.stamp.ClassA", "method2", "ClassA", 10));
        stackTrace.add(new StackTraceElement("eu.stamp.ClassB", "method1", "ClassB", 20));

        StackTrace trace =  Mockito.mock(StackTrace.class);
        Mockito.when(trace.getNumberOfFrames()).thenReturn(2);
        Mockito.when(trace.getFrames()).thenReturn(stackTrace);

        CrashProperties.getInstance().setupStackTrace(trace);
        Throwable target = CrashProperties.getInstance().getTargetException();
        assertArrayEquals(target.getStackTrace(),stackTrace.toArray());
    }



}