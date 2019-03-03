package eu.stamp.botsing.parsers;

import eu.stamp.botsing.EllipsisFrame;
import eu.stamp.botsing.Frame;
import eu.stamp.botsing.StackTrace;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class StackTracesParsingTest {

    private static final Logger LOG = LoggerFactory.getLogger(StackTracesParsingTest.class);

    @Rule
    public TestRule watcher = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            LOG.info(String.format("Starting test: %s()...",
                    description.getMethodName()));
        }
    };

    @Test
    public void testSimpleStackTrace() {
        String input = "org.apache.commons.lang3.SerializationException: ClassNotFoundException " +
                "while reading cloned object data\n" +
                "\tat org.apache.commons.lang3.SerializationUtils.clone(SerializationUtils.java:99)\n" +
                "\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n" +
                "\tat sun.reflect.GeneratedMethodAccessor4.invoke(Unknown Source)\n" +
                "\t... 44 more";
        List<StackTrace> stackTraces = StackTracesParsing.parseStackTraces(input);
        assertThat(stackTraces, hasSize(1));

        StackTrace st = stackTraces.get(0);
        LOG.trace("Stack trace is {}", st);
        assertThat(st.getExceptionClass(), equalTo("org.apache.commons.lang3.SerializationException"));
        assertThat(st.getErrorMessage(), equalTo("ClassNotFoundException while reading cloned object data"));
        assertThat(st.highestFrameLevel(), equalTo(4));

        assertThat(st.getFrame(1), equalTo(new Frame("org.apache.commons.lang3.SerializationUtils",
                "clone", "SerializationUtils.java", 99)));
        assertThat(st.getFrame(2), equalTo(new Frame("sun.reflect.NativeMethodAccessorImpl",
                "invoke0", null, Frame.IS_NATIVE)));
        assertThat(st.getFrame(3), equalTo(new Frame("sun.reflect.GeneratedMethodAccessor4",
                "invoke", null, Frame.IS_UNKNOWN)));
        assertThat(st.getFrame(4), equalTo(new EllipsisFrame(44)));
    }

    @Test
    public void testSimpleStackTraceWithInit() {
        String input = "org.apache.commons.lang3.SerializationException: ClassNotFoundException " +
                "while reading cloned object data\n" +
                "\tat org.apache.commons.lang3.SerializationUtils.<init>(SerializationUtils.java:99)\n" +
                "\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n" +
                "\tat sun.reflect.GeneratedMethodAccessor4.invoke(Unknown Source)\n" +
                "\t... 44 more";
        List<StackTrace> stackTraces = StackTracesParsing.parseStackTraces(input);
        assertThat(stackTraces, hasSize(1));

        StackTrace st = stackTraces.get(0);
        LOG.trace("Stack trace is {}", st);
        assertThat(st.getExceptionClass(), equalTo("org.apache.commons.lang3.SerializationException"));
        assertThat(st.getErrorMessage(), equalTo("ClassNotFoundException while reading cloned object data"));
        assertThat(st.highestFrameLevel(), equalTo(4));

        assertThat(st.getFrame(1), equalTo(new Frame("org.apache.commons.lang3.SerializationUtils",
                "<init>", "SerializationUtils.java", 99)));
        assertThat(st.getFrame(2), equalTo(new Frame("sun.reflect.NativeMethodAccessorImpl",
                "invoke0", null, Frame.IS_NATIVE)));
        assertThat(st.getFrame(3), equalTo(new Frame("sun.reflect.GeneratedMethodAccessor4",
                "invoke", null, Frame.IS_UNKNOWN)));
        assertThat(st.getFrame(4), equalTo(new EllipsisFrame(44)));
    }


    @Test
    public void testStackTraceWithCause() {
        String input = "org.apache.commons.lang3.SerializationException: ClassNotFoundException " +
                "while reading cloned object data\n" +
                "\tat org.apache.commons.lang3.SerializationUtils.<init>(SerializationUtils.java:99)\n" +
                "Caused by: java.lang.ClassNotFoundException: byte\n" +
                "\tat org.apache.tools.ant.AntClassLoader.findClassInComponents(AntClassLoader.java:1365)\n" +
                "\tat java.io.ObjectInputStream.readNonProxyDesc(ObjectInputStream.java:1613)\n";
        String cause = "java.lang.ClassNotFoundException: byte\n" +
                "\tat org.apache.tools.ant.AntClassLoader.findClassInComponents(AntClassLoader.java:1365)\n" +
                "\tat java.io.ObjectInputStream.readNonProxyDesc(ObjectInputStream.java:1613)\n";

        List<StackTrace> stackTraces = StackTracesParsing.parseStackTraces(input);
        assertThat(stackTraces, hasSize(1));

        StackTrace st = stackTraces.get(0);
        LOG.trace("Stack trace is {}", st);
        assertThat(st.getExceptionClass(), equalTo("org.apache.commons.lang3.SerializationException"));
        assertThat(st.getErrorMessage(), equalTo("ClassNotFoundException while reading cloned object data"));
        assertThat(st.highestFrameLevel(), equalTo(1));
        assertThat(st.getCause(), notNullValue());

        StackTrace causeSt = StackTracesParsing.parseStackTraces(cause).get(0);
        assertThat(st.getCause(), equalTo(causeSt));
    }

    @Test
    public void testMultipleStackTraces() {
        String input = "org.apache.commons.lang3.SerializationException: ClassNotFoundException " +
                "while reading cloned object data\n" +
                "\tat org.apache.commons.lang3.SerializationUtils.clone(SerializationUtils.java:99)\n" +
                "java.lang.ClassNotFoundException: byte\n" +
                "\tat org.apache.tools.ant.AntClassLoader.findClassInComponents(AntClassLoader.java:1365)\n" +
                "\tat java.io.ObjectInputStream.readNonProxyDesc(ObjectInputStream.java:1613)\n";
        String first = "org.apache.commons.lang3.SerializationException: ClassNotFoundException " +
                "while reading cloned object data\n" +
                "\tat org.apache.commons.lang3.SerializationUtils.clone(SerializationUtils.java:99)\n";
        String second = "java.lang.ClassNotFoundException: byte\n" +
                "\tat org.apache.tools.ant.AntClassLoader.findClassInComponents(AntClassLoader.java:1365)\n" +
                "\tat java.io.ObjectInputStream.readNonProxyDesc(ObjectInputStream.java:1613)\n";

        List<StackTrace> stackTraces = StackTracesParsing.parseStackTraces(input);
        assertThat(stackTraces, hasSize(2));

        StackTrace firstSt = StackTracesParsing.parseStackTraces(first).get(0);
        StackTrace secondSt = StackTracesParsing.parseStackTraces(second).get(0);

        assertThat(stackTraces.get(0), equalTo(firstSt));
        assertThat(stackTraces.get(1), equalTo(secondSt));
    }

    @Test
    public void testInvalidInput() {
        String input = "Lorem ipsum dolor sit amet";
        List<StackTrace> stackTraces = StackTracesParsing.parseStackTraces(input);
        assertThat(stackTraces, hasSize(0));
    }

}
