package eu.stamp.botsing;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.lang.IndexOutOfBoundsException;
import java.io.StringReader;
import java.util.ArrayList;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

@RunWith(MockitoJUnitRunner.class)
public class StackTraceTest {

    @Test
    public void testLogParsing() throws Exception {
        BufferedReader obj = new BufferedReader(new StringReader("java.lang.IllegalArgumentException:\n" +
                "\tat eu.stamp.ClassA.method2(ClassA.java:10)\n" +
                "\tat eu.stamp.ClassB.method1(ClassB.java:20)"));

        StackTrace trace =  Mockito.mock(StackTrace.class);
        Mockito.when(trace.readFromFile(anyString())).thenReturn(obj);
        Mockito.doCallRealMethod().when(trace).setup(anyString(),anyInt());
        Mockito.doCallRealMethod().when(trace).getExceptionType();
        Mockito.doCallRealMethod().when(trace).getTargetClass();
        Mockito.doCallRealMethod().when(trace).getTargetMethod();
        Mockito.doCallRealMethod().when(trace).getTarget_frame_level();
        Mockito.doCallRealMethod().when(trace).getTargetLine();

        trace.setup("",2);
        assertEquals("java.lang.IllegalArgumentException", trace.getExceptionType());
        assertEquals("eu.stamp.ClassB", trace.getTargetClass());
        assertEquals("method1", trace.getTargetMethod());
        assertEquals(2, trace.getTarget_frame_level());
        assertEquals(20, trace.getTargetLine());
    }

    @Test
    public void testFrame() throws Exception {
        // this is the content of our (mocked) log file
        BufferedReader obj = new BufferedReader(new StringReader("java.lang.IllegalArgumentException:\n" +
                "\tat eu.stamp.ClassA.method2(ClassA.java:10)\n" +
                "\tat eu.stamp.ClassB.method1(ClassB.java:20)"));

        StackTrace trace =  Mockito.mock(StackTrace.class);
        Mockito.when(trace.readFromFile(anyString())).thenReturn(obj);
        Mockito.doCallRealMethod().when(trace).setup(anyString(),anyInt());
        Mockito.doCallRealMethod().when(trace).getFrame(anyInt());

        trace.setup("mockedFile",2);
        StackTraceElement frame = trace.getFrame(1);
        assertEquals("eu.stamp.ClassA", frame.getClassName());
        assertEquals("method2", frame.getMethodName());
        assertEquals(10, frame.getLineNumber());
    }

    @Test
    public void testFrames() throws Exception{
        // this is the content of our (mocked) log file
        BufferedReader obj = new BufferedReader(new StringReader("java.lang.IllegalArgumentException:\n" +
                "\tat eu.stamp.ClassA.method2(ClassA.java:10)\n" +
                "\tat eu.stamp.ClassB.method1(ClassB.java:20)"));

        StackTrace trace =  Mockito.mock(StackTrace.class);
        Mockito.when(trace.readFromFile(anyString())).thenReturn(obj);
        Mockito.doCallRealMethod().when(trace).setup(anyString(),anyInt());
        Mockito.doCallRealMethod().when(trace).getFrames();
        Mockito.doCallRealMethod().when(trace).getNumberOfFrames();

        trace.setup("mockedFile",2);
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
        // this is the content of our (mocked) log file
        BufferedReader obj = new BufferedReader(new StringReader("java.lang.IllegalArgumentException:\n" +
                "\tat eu.stamp.ClassA.method2(ClassA.java:10)\n" +
                "\tat eu.stamp.ClassB.method1(ClassB.java:20)"));

        StackTrace trace =  Mockito.mock(StackTrace.class);
        Mockito.when(trace.readFromFile(anyString())).thenReturn(obj);
        Mockito.doCallRealMethod().when(trace).setup(anyString(),anyInt());

        trace.setup("mockedFile",4);
    }

    @Test(expected = FileNotFoundException.class)
    public void missingFile() throws FileNotFoundException {
        StackTrace trace = StackTrace.getInstance();
        trace.readFromFile("");
    }

}