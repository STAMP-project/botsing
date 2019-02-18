package eu.stamp.botsing;

import java.util.Objects;

/**
 * An Ellipsis frame with the given number of more frames ellipsed. The objects always have the ELLIPSIS_CLASS_NAME
 * value as class name and ELLIPSIS_METHOD_NAME as method name. The fileName is always null and the frame is always
 * unknown.
 */
public final class EllipsisFrame extends Frame {

    public static final String ELLIPSIS_CLASS_NAME = "ellipsis";
    public static final String ELLIPSIS_METHOD_NAME = "frame";

    private int more;

    /**
     * Builds a new ellipsis frame with ELLIPSIS_CLASS_NAME as class name and ELLIPSIS_METHOD_NAME as method name. The
     * fileName is null and the frame is unknown.
     *
     * @param more The number of frames ellipsed. This value should be higher than 0.
     */
    public EllipsisFrame(int more) {
        super(ELLIPSIS_CLASS_NAME, ELLIPSIS_METHOD_NAME, null, IS_UNKNOWN);
        this.more = more;
    }

    @Override
    public String getClassName() {
        return ELLIPSIS_CLASS_NAME;
    }

    @Override
    public String getMethodName() {
        return ELLIPSIS_METHOD_NAME;
    }

    @Override
    public String getFileName() {
        return null;
    }

    @Override
    public int getLineNumber() {
        return IS_UNKNOWN;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), more);
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }
        if(o == null || getClass() != o.getClass()) {
            return false;
        }
        if(!super.equals(o)) {
            return false;
        }
        EllipsisFrame that = (EllipsisFrame) o;
        return more == that.more;
    }

    @Override
    public String toString() {
        return "... " + more + " more";
    }

    @Override
    public boolean isNative() {
        return false;
    }

    @Override
    public boolean isUnknown() {
        return true;
    }

    /**
     * Returns the number of frames ellipsed.
     *
     * @return The number of ellipsed frames.
     */
    public int howManyMore() {
        return this.more;
    }
}
