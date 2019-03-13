package eu.stamp.botsing;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * This class represents a Java stack trace. Each stack trace has an exception type, an error message and a list of
 * frames. Each stack trace may optinally have a cause (i.e., another stack trace with the encapsulated exception). The
 * last frame of a stack trace is either a regular frame or an ellipsis indicating that the remainder of the stack trace
 * has been stripped.
 */
public class StackTrace implements Iterable<Frame> {

    private String exceptionClass;
    private String errorMessage;
    private List<Frame> frames;
    private StackTrace cause = null;

    /**
     * Creates a new empty stack trace.
     */
    public StackTrace() {
        this(null, null);
    }

    /**
     * Creates a new stack trace with the given exception class and error message.
     *
     * @param exceptionClass The exception class name.
     * @param errorMessage The error message for this stack trace.
     */
    public StackTrace(String exceptionClass, String errorMessage) {
        this(exceptionClass, errorMessage, null);
    }

    /**
     * Creates a new stack trace that has the given cause and with the given exception class and error message.
     *
     * @param exceptionClass The exception class name.
     * @param errorMessage The error message for this stack trace. May be null.
     * @param cause The cause of this stack trace. May be null.
     */
    public StackTrace(String exceptionClass, String errorMessage, StackTrace cause) {
        this.exceptionClass = exceptionClass;
        this.errorMessage = errorMessage;
        this.frames = new ArrayList<>();
        this.cause = cause;
    }

    /**
     * Returns the exception class.
     *
     * @return The exception class
     */
    public String getExceptionClass() {
        return exceptionClass;
    }

    /**
     * Sets the exception class. Exceptions class should not be null.
     *
     * @param exceptionClass The new  exception class.
     */
    public void setExceptionClass(String exceptionClass) {
        this.exceptionClass = exceptionClass;
    }

    /**
     * Returns the error message (if any).
     *
     * @return The error message.
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the error message.
     *
     * @param errorMessage The new error message.
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Returns the cause of this stack trace (if any).
     *
     * @return The cause of this stack trace.
     */
    public StackTrace getCause() {
        return cause;
    }

    /**
     * Sets the cause of this stack trace.
     *
     * @param stackTrace The new cause of this stack trace. There should be no cycle in the stack trace causes.
     */
    public void setCause(StackTrace stackTrace) {
        this.cause = stackTrace;
    }

    @Override
    public Iterator<Frame> iterator() {
        return frames.iterator();
    }

    /**
     * Removes the frame at the given level. The level should be between 1 and highestFrameLevel (incl.).
     *
     * @param level The level of the frame to remove.
     * @return The removed frame.
     */
    public Frame removeFrame(int level) {
        return frames.remove(level - 1);
    }

    /**
     * Append the given frame to the end of the stack trace. If this frame is an EllipsisFrame, no more frames can be
     * added afterwards.
     *
     * @param frame The frame to add. Should not be null.
     * @throws IllegalArgumentException If the last frame of the stack trace is an EllipsisFrame or if the given frame
     * is null.
     */
    public void addFrame(Frame frame) throws IllegalArgumentException {
        checkArgument(!(highestFrameLevel() > 0) || !(getFrame(highestFrameLevel()) instanceof EllipsisFrame),
                "Highest frame is an ellipse, no more frames can be added to this stack trace!");
        checkArgument(frame != null, "Given frame may not be null!");
        frames.add(frame);
    }

    /**
     * Returns the highest frame level. Frames are indexed from 1 to maxLevel.
     *
     * @return The highest frame level.
     */
    public int highestFrameLevel() {
        return frames.size();
    }

    /**
     * Returns the frame at the given level. The level should be between 1 and highestFrameLevel (incl.).
     *
     * @param level The level of the frame.
     * @return The frame at the given level.
     */
    public Frame getFrame(int level) throws IllegalArgumentException {
        checkArgument(level > 0 && level <= highestFrameLevel(), "Level should be between 1 and %s (incl.)!",
                frames.size());
        return frames.get(level - 1);
    }

    @Override
    public int hashCode() {
        return Objects.hash(exceptionClass, errorMessage, frames, cause);
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }
        if(o == null || getClass() != o.getClass()) {
            return false;
        }
        StackTrace stackTrace = (StackTrace) o;
        return Objects.equals(exceptionClass, stackTrace.exceptionClass) && Objects.equals(errorMessage,
                stackTrace.errorMessage) && Objects.equals(frames, stackTrace.frames) && Objects.equals(cause,
                stackTrace.cause);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder = builder.append(exceptionClass).append(':').append(errorMessage).append('\n');
        for(Frame f : this) {
            builder = builder.append(f.toString()).append('\n');
        }
        if(cause != null) {
            builder = builder.append("Caused by: ").append(cause.toString());
        }
        return builder.toString();
    }
}