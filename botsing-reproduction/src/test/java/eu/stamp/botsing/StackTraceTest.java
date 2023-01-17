package eu.stamp.botsing;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.evosuite.shaded.org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.util.ArrayList;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;

@RunWith(MockitoJUnitRunner.class)
public class StackTraceTest {

    private static final Logger LOG = LoggerFactory.getLogger(StackTraceTest.class);

    @Rule
    public TestRule watcher = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            LOG.info(String.format("Starting test: %s()...",
                    description.getMethodName()));
        }
    };

    @Test
    public void testLogParsing() throws Exception {
        // this is the content of our (mocked) log file
        BufferedReader obj = new BufferedReader(new StringReader("java.lang.IllegalArgumentException:\n" +
                "\tat eu.stamp.ClassA.method2(ClassA.java:10)\n" +
                "\tat eu.stamp.ClassB.method1(ClassB.java:20)"));

        StackTrace trace = Mockito.spy(new StackTrace());
        Mockito.doReturn(obj).when(trace).readFromFile(anyString());
        trace.setup("", 2);

        assertEquals("java.lang.IllegalArgumentException", trace.getExceptionType());
        assertEquals("eu.stamp.ClassB", trace.getTargetClass());
        assertEquals("method1", trace.getTargetMethod());
        assertEquals(2, trace.getTargetFrameLevel());
        assertEquals(20, trace.getTargetLine());
    }

    @Test
    public void testFrame() throws Exception {
        BufferedReader obj = new BufferedReader(new StringReader("java.lang.IllegalArgumentException:\n" +
                "\tat eu.stamp.ClassA.method2(ClassA.java:10)\n" +
                "\tat eu.stamp.ClassB.method1(ClassB.java:20)"));

        StackTrace trace = Mockito.spy(new StackTrace());
        Mockito.doReturn(obj).when(trace).readFromFile(anyString());
        trace.setup("", 2);

        StackTraceElement frame = trace.getFrame(1);
        assertEquals("eu.stamp.ClassA", frame.getClassName());
        assertEquals("method2", frame.getMethodName());
        assertEquals(10, frame.getLineNumber());
    }

    @Test
    public void testFrames() throws Exception{
        BufferedReader obj = new BufferedReader(new StringReader("java.lang.IllegalArgumentException:\n" +
                "\tat eu.stamp.ClassA.method2(ClassA.java:10)\n" +
                "\tat eu.stamp.ClassB.method1(ClassB.java:20)"));

        StackTrace trace = Mockito.spy(new StackTrace());
        Mockito.doReturn(obj).when(trace).readFromFile(anyString());
        trace.setup("", 2);

        ArrayList<StackTraceElement> list = trace.getFrames();
        assertEquals("eu.stamp.ClassA", list.get(0).getClassName());
        assertEquals("method2", list.get(0).getMethodName());
        assertEquals(10, list.get(0).getLineNumber());
        assertEquals("eu.stamp.ClassB", list.get(1).getClassName());
        assertEquals("method1", list.get(1).getMethodName());
        assertEquals(20, list.get(1).getLineNumber());
        assertEquals(2, list.size());
        assertEquals(2, trace.getNumberOfFrames());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWrongFrameLevel() throws Exception{
        BufferedReader obj = new BufferedReader(new StringReader("java.lang.IllegalArgumentException:\n" +
                "\tat eu.stamp.ClassA.method2(ClassA.java:10)\n" +
                "\tat eu.stamp.ClassB.method1(ClassB.java:20)"));

        StackTrace trace = Mockito.spy(new StackTrace());
        Mockito.doReturn(obj).when(trace).readFromFile(anyString());
        trace.setup("", 4);

        trace.setup("mockedFile",4);
    }

    @Test(expected = FileNotFoundException.class)
    public void missingFile() throws FileNotFoundException {
        StackTrace trace = new StackTrace();
        trace.readFromFile("");
    }
}
