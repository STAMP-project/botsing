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


public class EllipsisFrameTest {

    private static final Logger LOG = LoggerFactory.getLogger(EllipsisFrameTest.class);

    @Rule
    public TestRule watcher = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            LOG.info(String.format("Starting test: %s()...",
                    description.getMethodName()));
        }
    };

    @Test
    public void testNew() {
        int more = 42;
        EllipsisFrame frame = new EllipsisFrame(more);
        assertThat(frame.getClassName(), equalTo(EllipsisFrame.ELLIPSIS_CLASS_NAME));
        assertThat(frame.getMethodName(), equalTo(EllipsisFrame.ELLIPSIS_METHOD_NAME));
        assertThat(frame.getFileName(), nullValue());
        assertThat(frame.getLineNumber(), equalTo(Frame.IS_UNKNOWN));
        assertThat(frame.isNative(), equalTo(false));
        assertThat(frame.isUnknown(), equalTo(true));
        assertThat(frame.howManyMore(), equalTo(more));
    }

    @Test
    public void testImmutable() {
        int more = 42;
        EllipsisFrame frame = new EllipsisFrame(more);
        assertThat(frame.getClassName(), is(EllipsisFrame.ELLIPSIS_CLASS_NAME));
        assertThat(frame.getMethodName(), is(EllipsisFrame.ELLIPSIS_METHOD_NAME));
        assertThat(frame.getFileName(), nullValue());
        assertThat(frame.getLineNumber(), is(Frame.IS_UNKNOWN));
        assertThat(frame.isNative(), is(false));
        assertThat(frame.isUnknown(), is(true));
        assertThat(frame.howManyMore(), equalTo(more));


        frame.setClassName("blabla");
        assertThat(frame.getClassName(), is(EllipsisFrame.ELLIPSIS_CLASS_NAME));
        frame.setMethodName("bla");
        assertThat(frame.getMethodName(), is(EllipsisFrame.ELLIPSIS_METHOD_NAME));
        frame.setLocation("file.java", 24);
        assertThat(frame.getFileName(), nullValue());
        assertThat(frame.getLineNumber(), is(Frame.IS_UNKNOWN));
        assertThat(frame.isNative(), is(false));
        assertThat(frame.isUnknown(), is(true));
        assertThat(frame.howManyMore(), equalTo(more));
    }

    @Test
    public void testEquals() {
        int more = 42;
        EllipsisFrame frame = new EllipsisFrame(more);
        assertThat(frame.equals(frame), is(true));
        assertThat(frame.equals(new EllipsisFrame(more)), is(true));
        assertThat(frame.equals(new EllipsisFrame(24)), is(false));
        assertThat(frame.equals(new Frame("test", "methodtest", "file.java",  24)), is(false));

    }

    @Test
    public void testHashCode() {
        int more = 42;
        EllipsisFrame frame = new EllipsisFrame(more);
        assertThat("Hash codes generated on the same objects should be equal!",
                frame.hashCode(), equalTo(frame.hashCode()));
        assertThat("Hash codes generated on equal objects should be equal!",
                frame.hashCode(), equalTo(new EllipsisFrame(more).hashCode()));
        assertThat("Hash codes generated on non equal objects should be different!",
                frame.hashCode(), not(new EllipsisFrame(24).hashCode()));
    }

    @Test
    public void testToString() {
        int more = 42;
        EllipsisFrame frame = new EllipsisFrame(more);
        assertThat(frame.toString(), not(emptyString()));
    }

}