package eu.stamp.botsing;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

public class FrameTest {

    private static final Logger LOG = LoggerFactory.getLogger(FrameTest.class);

    @Rule
    public TestRule watcher = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            LOG.info(String.format("Starting test: %s()...",
                    description.getMethodName()));
        }
    };

    @Test
    public void testNewRegularFrame() {
        String className = "clazz";
        String method = "method";
        String fileName = "file.java";
        int lineNumber = 42;
        Frame frame = new Frame(className, method, fileName, lineNumber);
        assertThat(frame.getClassName(), equalTo(className));
        assertThat(frame.getMethodName(), equalTo(method));
        assertThat(frame.getFileName(), equalTo(fileName));
        assertThat(frame.getLineNumber(), equalTo(lineNumber));
        assertThat(frame.isUnknown(), is(false));
        assertThat(frame.isNative(), is(false));
    }

    @Test
    public void testNewUnknownFrame() {
        String className = "clazz";
        String method = "method";
        Frame frame = new Frame(className, method);
        assertThat(frame.getClassName(), equalTo(className));
        assertThat(frame.getMethodName(), equalTo(method));
        assertThat(frame.getFileName(), nullValue());
        assertThat(frame.getLineNumber(), equalTo(Frame.IS_UNKNOWN));
        assertThat(frame.isUnknown(), is(true));
        assertThat(frame.isNative(), is(false));
    }

    @Test
    public void testNewNativeFrame() {
        String className = "clazz";
        String method = "method";
        String fileName = null;
        int lineNumber = Frame.IS_NATIVE;
        Frame frame = new Frame(className, method, fileName, lineNumber);
        assertThat(frame.getClassName(), equalTo(className));
        assertThat(frame.getMethodName(), equalTo(method));
        assertThat(frame.getFileName(), nullValue());
        assertThat(frame.getLineNumber(), equalTo(lineNumber));
        assertThat(frame.isUnknown(), is(false));
        assertThat(frame.isNative(), is(true));
    }


    @Test
    public void testSetLocation() {
        String className = "clazz";
        String method = "method";
        String fileName = "file.java";
        int lineNumber = 42;
        Frame frame = new Frame(className, method, fileName, lineNumber);
        assertThat(frame.getClassName(), equalTo(className));
        assertThat(frame.getMethodName(), equalTo(method));
        assertThat(frame.getFileName(), equalTo(fileName));
        assertThat(frame.getLineNumber(), equalTo(lineNumber));
        assertThat(frame.isUnknown(), is(false));
        assertThat(frame.isNative(), is(false));

        frame.setLocation(null, Frame.IS_UNKNOWN);
        assertThat(frame.getClassName(), equalTo(className));
        assertThat(frame.getMethodName(), equalTo(method));
        assertThat(frame.getFileName(), nullValue());
        assertThat(frame.getLineNumber(), equalTo(Frame.IS_UNKNOWN));
        assertThat(frame.isUnknown(), is(true));
        assertThat(frame.isNative(), is(false));

        frame.setLocation(null, Frame.IS_NATIVE);
        assertThat(frame.getClassName(), equalTo(className));
        assertThat(frame.getMethodName(), equalTo(method));
        assertThat(frame.getFileName(), nullValue());
        assertThat(frame.getLineNumber(), equalTo(Frame.IS_NATIVE));
        assertThat(frame.isUnknown(), is(false));
        assertThat(frame.isNative(), is(true));
    }

    @Test
    public void testSetClassName() {
        String className = "clazz";
        String method = "method";
        String fileName = "file.java";
        int lineNumber = 42;
        Frame frame = new Frame(className, method, fileName, lineNumber);
        assertThat(frame.getClassName(), equalTo(className));
        assertThat(frame.getMethodName(), equalTo(method));
        assertThat(frame.getFileName(), equalTo(fileName));
        assertThat(frame.getLineNumber(), equalTo(lineNumber));
        assertThat(frame.isUnknown(), is(false));
        assertThat(frame.isNative(), is(false));

        className = "clazz2";
        frame.setClassName(className);
        assertThat(frame.getClassName(), equalTo(className));
        assertThat(frame.getMethodName(), equalTo(method));
        assertThat(frame.getFileName(), equalTo(fileName));
        assertThat(frame.getLineNumber(), equalTo(lineNumber));
        assertThat(frame.isUnknown(), is(false));
        assertThat(frame.isNative(), is(false));
    }

    @Test
    public void testSetMethodName() {
        String className = "clazz";
        String method = "method";
        String fileName = "file.java";
        int lineNumber = 42;
        Frame frame = new Frame(className, method, fileName, lineNumber);
        assertThat(frame.getClassName(), equalTo(className));
        assertThat(frame.getMethodName(), equalTo(method));
        assertThat(frame.getFileName(), equalTo(fileName));
        assertThat(frame.getLineNumber(), equalTo(lineNumber));
        assertThat(frame.isUnknown(), is(false));
        assertThat(frame.isNative(), is(false));

        method = "method2";
        frame.setMethodName(method);
        assertThat(frame.getClassName(), equalTo(className));
        assertThat(frame.getMethodName(), equalTo(method));
        assertThat(frame.getFileName(), equalTo(fileName));
        assertThat(frame.getLineNumber(), equalTo(lineNumber));
        assertThat(frame.isUnknown(), is(false));
        assertThat(frame.isNative(), is(false));
    }

    @Test
    public void testEquals() {
        String className = "clazz";
        String method = "method";
        String fileName = "file.java";
        int lineNumber = 42;
        Frame frame = new Frame(className, method, fileName, lineNumber);

        assertThat(frame.equals(frame), is(true));
        assertThat(frame.equals(new Frame(className, method, fileName, lineNumber)), is(true));

        assertThat(frame.equals(new Frame("other", method, fileName, lineNumber)), is(false));
        assertThat(frame.equals(new Frame(className, "other", fileName, lineNumber)), is(false));
        assertThat(frame.equals(new Frame(className, method, "other", lineNumber)), is(false));
        assertThat(frame.equals(new Frame(className, method, fileName, 24)), is(false));

        assertThat(frame.equals(new Frame(className, method)), is(false));
    }


    @Test
    public void testHashCode() {
        String className = "clazz";
        String method = "method";
        String fileName = "file.java";
        int lineNumber = 42;
        Frame frame = new Frame(className, method, fileName, lineNumber);

        assertThat("Hash codes generated on the same objects should be equal!",
                frame.hashCode(), equalTo(frame.hashCode()));
        assertThat("Hash codes generated on equal objects should be equal!",
                frame.hashCode(), equalTo(new Frame(className, method, fileName, lineNumber).hashCode()));

        assertThat("Hash codes generated on non equal objects should be different!",
                frame.hashCode(), not(new Frame("other", method, fileName, lineNumber).hashCode()));
        assertThat("Hash codes generated on non equal objects should be different!",
                frame.hashCode(), not(new Frame(className, "other", fileName, lineNumber).hashCode()));
        assertThat("Hash codes generated on non equal objects should be different!",
                frame.hashCode(), not(new Frame(className, method, "other", lineNumber).hashCode()));
        assertThat("Hash codes generated on non equal objects should be different!",
                frame.hashCode(), not(new Frame(className, method, fileName, 24).hashCode()));
    }

    @Test
    public void testToString() {
        String className = "clazz";
        String method = "method";
        String fileName = "file.java";
        int lineNumber = 42;
        Frame frame = new Frame(className, method, fileName, lineNumber);
        assertThat(frame.toString(), not(emptyString()));
    }

}