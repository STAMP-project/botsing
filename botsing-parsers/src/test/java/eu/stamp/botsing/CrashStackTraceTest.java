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

public class CrashStackTraceTest {

    private static final Logger LOG = LoggerFactory.getLogger(CrashStackTraceTest.class);

    @Rule
    public TestRule watcher = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            LOG.info(String.format("Starting test: %s()...",
                    description.getMethodName()));
        }
    };

    @Test
    public void testNewSimpleStackTrace() {
        String exception = "NullPointerOfCourse";
        String message = "because why not!";
        CrashStackTrace st = new CrashStackTrace(exception, message);

        assertThat(st.getExceptionClass(), equalTo(exception));
        assertThat(st.getErrorMessage(), equalTo(message));
        assertThat(st.getCause(), nullValue());
        assertThat(st.highestFrameLevel(), equalTo(0));
        assertThat(st, iterableWithSize(0));

        st.addFrame(new Frame("clazz", "method"));
        st.addFrame(new Frame("clazz2", "method2"));
    }

    @Test
    public void testAddFrame() {
        String exception = "NullPointerOfCourse";
        String message = "because why not!";
        CrashStackTrace st = new CrashStackTrace(exception, message);
        st.addFrame(new Frame("clazz", "method"));
        st.addFrame(new Frame("clazz2", "method2"));

        assertThat(st.getExceptionClass(), equalTo(exception));
        assertThat(st.getErrorMessage(), equalTo(message));
        assertThat(st.getCause(), nullValue());
        assertThat(st.highestFrameLevel(), equalTo(2));
        assertThat(st, iterableWithSize(2));
    }

    @Test
    public void testRemoveFrame() {
        String exception = "NullPointerOfCourse";
        String message = "because why not!";
        CrashStackTrace st = new CrashStackTrace(exception, message);
        st.addFrame(new Frame("clazz", "method"));
        st.addFrame(new Frame("clazz2", "method2"));

        assertThat(st.highestFrameLevel(), equalTo(2));
        assertThat(st, iterableWithSize(2));

        st.removeFrame(2);
        assertThat(st.highestFrameLevel(), equalTo(1));
        assertThat(st, iterableWithSize(1));

        st.removeFrame(1);
        assertThat(st.highestFrameLevel(), equalTo(0));
        assertThat(st, iterableWithSize(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddEllipsisFrame() {
        String exception = "NullPointerOfCourse";
        String message = "because why not!";
        CrashStackTrace st = new CrashStackTrace(exception, message);
        st.addFrame(new Frame("clazz", "method"));
        st.addFrame(new Frame("clazz2", "method2"));

        assertThat(st.highestFrameLevel(), equalTo(2));
        assertThat(st, iterableWithSize(2));

        st.addFrame(new EllipsisFrame(34));

        assertThat(st.highestFrameLevel(), equalTo(3));
        assertThat(st, iterableWithSize(3));

        st.addFrame(new Frame("clazz3", "method3"));
    }


    @Test
    public void testEquals() {
        String exception = "NullPointerOfCourse";
        String message = "because why not!";
        CrashStackTrace st = new CrashStackTrace(exception, message);
        CrashStackTrace st2 = new CrashStackTrace();
        st2.setExceptionClass(exception);
        st2.setErrorMessage(message);

        assertThat(st.equals(st), is(true));
        assertThat(st.equals(st2), is(true));
        assertThat(st.equals(new CrashStackTrace(exception, message, st2)), is(false));

        st.addFrame(new Frame("clazz", "method"));
        st.addFrame(new Frame("clazz2", "method2"));
        assertThat(st.equals(st), is(true));
        assertThat(st.equals(st2), is(false));

        st2.addFrame(new Frame("clazz", "method"));
        st2.addFrame(new Frame("clazz2", "method2"));
        assertThat(st.equals(st), is(true));
        assertThat(st.equals(st2), is(true));

        st.setCause(new CrashStackTrace("except", message));
        assertThat(st.equals(st), is(true));
        assertThat(st.equals(st2), is(false));

        st2.setCause(new CrashStackTrace("except", message));
        assertThat(st.equals(st), is(true));
        assertThat(st.equals(st2), is(true));
    }

    @Test
    public void testHashCode() {
        String exception = "NullPointerOfCourse";
        String message = "because why not!";
        CrashStackTrace st = new CrashStackTrace(exception, message);
        CrashStackTrace st2 = new CrashStackTrace(exception, message);

        assertThat("Hash codes generated on the same objects should be equal!",
                st.hashCode(), equalTo(st.hashCode()));
        assertThat("Hash codes generated on equal objects should be equal!",
                st.hashCode(), equalTo(st2.hashCode()));
        assertThat("Hash codes generated on non equal objects should be different!",
                st.hashCode(), not(new CrashStackTrace(exception, message, st2).hashCode()));

        st.addFrame(new Frame("clazz", "method"));
        st.addFrame(new Frame("clazz2", "method2"));

        assertThat("Hash codes generated on the same objects should be equal!",
                st.hashCode(), equalTo(st.hashCode()));
        assertThat("Hash codes generated on non equal objects should be different!",
                st.hashCode(), not(st2.hashCode()));

        st2.addFrame(new Frame("clazz", "method"));
        st2.addFrame(new Frame("clazz2", "method2"));

        assertThat("Hash codes generated on the same objects should be equal!",
                st.hashCode(), equalTo(st.hashCode()));
        assertThat("Hash codes generated on equal objects should be equal!",
                st.hashCode(), equalTo(st2.hashCode()));

        st.setCause(new CrashStackTrace("except", message));

        assertThat("Hash codes generated on the same objects should be equal!",
                st.hashCode(), equalTo(st.hashCode()));
        assertThat("Hash codes generated on non equal objects should be different!",
                st.hashCode(), not(st2.hashCode()));

        st2.setCause(new CrashStackTrace("except", message));

        assertThat("Hash codes generated on the same objects should be equal!",
                st.hashCode(), equalTo(st.hashCode()));
        assertThat("Hash codes generated on equal objects should be equal!",
                st.hashCode(), equalTo(st2.hashCode()));
    }

    @Test
    public void testToString() {
        String exception = "NullPointerOfCourse";
        String message = "because why not!";
        CrashStackTrace st = new CrashStackTrace(exception, message);
        st.addFrame(new Frame("clazz", "method"));
        st.setCause(new CrashStackTrace("except", message));
        LOG.debug("Stack trace is: {}", st);
        assertThat(st.toString(), not(emptyString()));
    }

}