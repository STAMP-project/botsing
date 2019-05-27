package eu.stamp.botsing;

import org.apache.commons.cli.*;
import org.evosuite.Properties;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.util.ArrayList;


import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;

public class CrashPropertiesTest {

    private static final Logger LOG = LoggerFactory.getLogger(CrashPropertiesTest.class);

    @Rule
    public TestRule watcher = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            LOG.info(String.format("Starting test: %s()...",
                    description.getMethodName()));
        }
    };

    @Before
    public void resetCrashes(){
        CrashProperties.getInstance().clearStackTraceList();
    }

    @Test
    public void testSetupStackTrace() throws FileNotFoundException {
        CrashProperties properties = CrashProperties.getInstance();
        //assertNull(properties.getProjectClassPaths());

        BufferedReader givenStackTrace = new BufferedReader(new StringReader("java.lang.IllegalArgumentException:\n" +
                "\tat eu.stamp.ClassA.method2(ClassA.java:10)\n" +
                "\tat eu.stamp.ClassB.method1(ClassB.java:20)"));
        StackTrace target = Mockito.spy(new StackTrace());
        Mockito.doReturn(givenStackTrace).when(target).readFromFile(anyString());
        target.setup("", 2);
        CrashProperties.getInstance().setupStackTrace(target);

        properties.resetStackTrace(0);
        StackTrace crash = properties.getStackTrace(0);
        assertNull(crash.getExceptionType());
        assertNull(crash.getFrames());
        assertNull(crash.getTargetClass());
    }

    @Test
    public void testGetStringValue() throws Properties.NoSuchParameterException, IllegalAccessException {
        String value = CrashProperties.getInstance().getStringValue("D_OPT");
        LOG.debug(value);
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
        assertEquals("MAXTIME", cond.toString());
    }

    @Test
    public void testSetClasspath() {
        CrashProperties properties = CrashProperties.getInstance();
        properties.setClasspath("jar1:jar2");
        String[] jars = properties.getProjectClassPaths();

        assertEquals("jar1", jars[0]);
        assertEquals("jar2", jars[1]);
    }

    @Test
    public void testGetTargetException() {

        ArrayList<StackTraceElement> stackTrace = new ArrayList<StackTraceElement>();
        stackTrace.add(new StackTraceElement("eu.stamp.ClassA", "method2", "ClassA", 10));
        stackTrace.add(new StackTraceElement("eu.stamp.ClassB", "method1", "ClassB", 20));

        StackTrace trace = Mockito.mock(StackTrace.class);
        Mockito.when(trace.getNumberOfFrames()).thenReturn(2);
        Mockito.when(trace.getFrames()).thenReturn(stackTrace);

        CrashProperties.getInstance().setupStackTrace(trace);
        Throwable target = CrashProperties.getInstance().getTargetException(0);
        assertArrayEquals(target.getStackTrace(), stackTrace.toArray());
    }


}