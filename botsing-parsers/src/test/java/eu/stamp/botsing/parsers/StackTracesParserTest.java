package eu.stamp.botsing.parsers;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

public class StackTracesParserTest {

    private static final Logger LOG = LoggerFactory.getLogger(StackTracesParserTest.class);

    @Rule
    public TestRule watcher = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            LOG.info(String.format("Starting test: %s()...",
                    description.getMethodName()));
        }
    };

    private StackTracesParser setup(String inputString){
        CharStream text = CharStreams.fromString(inputString);
        StackTracesLexer lexer = new StackTracesLexer(text);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        StackTracesParser parser = new StackTracesParser(tokens);
        return parser;
    }

    @Test
    public void testSimpleStackTrace() {
        StackTracesParser parser = setup("org.apache.commons.lang3.SerializationException: ClassNotFoundException " +
                "while reading cloned object data\n" +
                "\tat org.apache.commons.lang3.SerializationUtils.<init>(SerializationUtils.java:99)\n" +
                "\tat org.apache.commons.lang3.SerializationUtilsTest.testPrimitiveTypeClassSerialization" +
                "(SerializationUtilsTest.java:373)\n" +
                "\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\n" +
                "\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)");
        assertThat(parser.stackTraces().content(), hasSize(1));
    }

    @Test
    public void testStackTraceWithInnerClasses() {
        StackTracesParser parser = setup("org.apache.commons.lang3.SerializationException: ClassNotFoundException " +
                "while reading cloned object data\n" +
                "\tat org.apache.commons.lang3.SerializationUtils.clone(SerializationUtils.java:99)\n" +
                "\tat junit.framework.TestResult$1.protect(TestResult.java:122)\n" +
                "\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n" +
                "\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\n" +
                "\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)");
        assertThat(parser.stackTraces().content(), hasSize(1));
    }

    @Test
    public void testStackTraceWithNativeMethod() {
        StackTracesParser parser = setup("org.apache.commons.lang3.SerializationException: ClassNotFoundException " +
                "while reading cloned object data\n" +
                "\tat org.apache.commons.lang3.SerializationUtils.clone(SerializationUtils.java:99)\n" +
                "\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n" +
                "\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\n" +
                "\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)");
        assertThat(parser.stackTraces().content(), hasSize(1));
    }

    @Test
    public void testStackTraceWithUnknownSource() {
        StackTracesParser parser = setup("org.apache.commons.lang3.SerializationException: ClassNotFoundException " +
                "while reading cloned object data\n" +
                "\tat org.apache.commons.lang3.SerializationUtils.clone(SerializationUtils.java:99)\n" +
                "\tat sun.reflect.GeneratedMethodAccessor4.invoke(Unknown Source)\n" +
                "\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\n" +
                "\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)");
        assertThat(parser.stackTraces().content(), hasSize(1));
    }

    @Test
    public void testStackTraceWithCausedBy() {
        StackTracesParser parser = setup("org.apache.commons.lang3.SerializationException: ClassNotFoundException " +
                "while reading cloned object data\n" +
                "\tat org.apache.commons.lang3.SerializationUtils.clone(SerializationUtils.java:99)\n" +
                "\tat sun.reflect.GeneratedMethodAccessor4.invoke(Unknown Source)\n" +
                "\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\n" +
                "\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n" +
                "Caused by: java.lang.ClassNotFoundException: byte\n" +
                "\tat org.apache.tools.ant.AntClassLoader.findClassInComponents(AntClassLoader.java:1365)\n" +
                "\tat java.io.ObjectInputStream.readNonProxyDesc(ObjectInputStream.java:1613)\n" +
                "\tat java.io.ObjectInputStream.readClassDesc(ObjectInputStream.java:1518)\n" +
                "\tat java.io.ObjectInputStream.readClass(ObjectInputStream.java:1484)");
        assertThat(parser.stackTraces().content(), hasSize(1));
    }

    @Test
    public void testMultipleSimpleStackTraces() {
        StackTracesParser parser = setup("org.apache.commons.lang3.SerializationException: ClassNotFoundException " +
                "while reading cloned object data\n" +
                "\tat org.apache.commons.lang3.SerializationUtils.clone(SerializationUtils.java:99)\n" +
                "\tat sun.reflect.GeneratedMethodAccessor4.invoke(Unknown Source)\n" +
                "\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\n" +
                "\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n" +
                "java.lang.ClassNotFoundException: byte\n" +
                "\tat org.apache.tools.ant.AntClassLoader.findClassInComponents(AntClassLoader.java:1365)\n" +
                "\tat java.io.ObjectInputStream.readNonProxyDesc(ObjectInputStream.java:1613)\n" +
                "\tat java.io.ObjectInputStream.readClassDesc(ObjectInputStream.java:1518)\n" +
                "\tat java.io.ObjectInputStream.readClass(ObjectInputStream.java:1484)");
        assertThat(parser.stackTraces().content(), hasSize(2));
    }

    @Test
    public void testInvalidInput() {
        StackTracesParser parser = setup("Lorem ipsum dolor sit amet");
        assertThat(parser.stackTraces().content(), hasSize(5));
        for(StackTracesParser.ContentContext ctx: parser.stackTraces().content()) {
            assertThat(ctx, instanceOf(StackTracesParser.MiscContentContext.class));
        }
    }


}
