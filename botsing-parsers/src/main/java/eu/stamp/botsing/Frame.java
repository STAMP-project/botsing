package eu.stamp.botsing;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * This class represents a frame in a stack trace. A frame has a class and method names, and either a class file + line
 * number, or 'Native Method' or 'Unknown Source' indication.
 */
public class Frame {

    public static final int IS_NATIVE = -1;
    public static final int IS_UNKNOWN = -2;

    private String className;
    private String methodName;
    private String fileName;
    private int lineNumber;

    /**
     * Builds a new frame with the given class name and method name and that declared as unknown.
     *
     * @param className the name of the class in which the method is defined
     * @param methodName the name of the method
     */
    public Frame(String className, String methodName) {
        this(className, methodName, null, IS_UNKNOWN);
    }

    /**
     * Builds a new Frame with the given class, method, file and line number. If lineNumber is IS_NATIVE or IS_UNKNOWN,
     * then fileName must be null (and vice versa).
     *
     * @param className The name of the class (should not be null).
     * @param methodName The name of the method (should not be null).
     * @param fileName The name of the file or null if isNative or isUnknown.
     * @param lineNumber The line in the file or IS_NATIVE or IS_UNKNOWN.
     * @throws IllegalArgumentException If line number is IS_NATIVE or IS_UNKNOWN and fileName is not null, or if file
     * name is null and line number is not IS_NATIVE or IS_UNKNOWN.
     */
    public Frame(String className, String methodName, String fileName, int lineNumber) throws IllegalArgumentException {
        checkArgument(!(lineNumber == IS_UNKNOWN || lineNumber == IS_NATIVE) || (fileName == null),
                "If line number " + "is IS_UNKNOWN or IS_NATIVE, then fileName must be null!");
        checkArgument(!(fileName == null) || (lineNumber == IS_UNKNOWN || lineNumber == IS_NATIVE),
                "If fileName is " + "null, then line number should be IS_UNKNOWN or IS_NATIVE!");
        this.className = className;
        this.methodName = methodName;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
    }

    /**
     * Returns the name of the class.
     *
     * @return The name of the class.
     */
    public String getClassName() {
        return className;
    }

    /**
     * Sets the name of the class.
     *
     * @param className should not be null.
     */
    public void setClassName(String className) throws IllegalArgumentException {
        this.className = className;
    }

    /**
     * Returns the name of the method.
     *
     * @return The name of the method.
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Sets the name of the method.
     *
     * @param methodName should not be null.
     */
    public void setMethodName(String methodName) throws IllegalArgumentException {
        this.methodName = methodName;
    }

    /**
     * Returns the name of the file where the method is defined or null if isNative or isUnknown.
     *
     * @return The name of the file where the method is defined or null if isNative or isUnknown.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns the line number or IS_NATIVE if isNative or IS_UNKNOWN if isUnknown.
     *
     * @return The line number or IS_NATIVE if isNative or IS_UNKNOWN if isUnknown.
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Sets the location to the given line in the given file name. If lineNumber is IS_NATIVE or IS_UNKNOWN, then
     * fileName must be null (and vice versa).
     *
     * @param fileName The name of the file.
     * @param lineNumber The line in the file.
     * @throws IllegalArgumentException If line number is IS_NATIVE or IS_UNKNOWN and fileName is not null, or if file
     * name is null and line number is not IS_NATIVE or IS_UNKNOWN
     */
    public void setLocation(String fileName, int lineNumber) throws IllegalArgumentException {
        checkArgument(!(lineNumber == IS_UNKNOWN || lineNumber == IS_NATIVE) || (fileName == null),
                "If line number " + "is IS_UNKNOWN or IS_NATIVE, then fileName must be null!");
        checkArgument(!(fileName == null) || (lineNumber == IS_UNKNOWN || lineNumber == IS_NATIVE),
                "If fileName is " + "null, then line number should be IS_UNKNOWN or IS_NATIVE!");
        this.fileName = fileName;
        this.lineNumber = lineNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, methodName, fileName, lineNumber);
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }
        if(o == null || getClass() != o.getClass()) {
            return false;
        }
        Frame frame = (Frame) o;
        return lineNumber == frame.lineNumber && Objects.equals(className, frame.className) && Objects.equals(methodName, frame.methodName) && Objects.equals(fileName, frame.fileName);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder = builder.append("\tat ").append(className).append('.').append(methodName).append('(');
        if(isNative()) {
            builder = builder.append("Native Method");
        } else if(isUnknown()) {
            builder = builder.append("Unknown Source");
        } else {
            builder = builder.append(fileName).append(':').append(lineNumber);
        }
        builder = builder.append(')');
        return builder.toString();
    }

    /**
     * Indicates if the methods is declared native in the stack trace.
     *
     * @return True if the methods is declared native in the stack trace.
     */
    public boolean isNative() {
        return this.lineNumber == IS_NATIVE;
    }

    /**
     * Indicates if the methods is declared unknown in the stack trace.
     *
     * @return True if the methods is declared unknown in the stack trace.
     */
    public boolean isUnknown() {
        return this.lineNumber == IS_UNKNOWN;
    }
}
