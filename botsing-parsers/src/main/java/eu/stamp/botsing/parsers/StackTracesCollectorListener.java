package eu.stamp.botsing.parsers;

import eu.stamp.botsing.EllipsisFrame;
import eu.stamp.botsing.Frame;
import eu.stamp.botsing.CrashStackTrace;
import org.antlr.v4.runtime.misc.Interval;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects the stack traces in a given abstract syntax tree. To be used with a ParseTreeWalker object.
 *
 * @see org.antlr.v4.runtime.tree.ParseTreeWalker
 */
public class StackTracesCollectorListener extends StackTracesParserBaseListener {

    private List<CrashStackTrace> stackTraces;

    private CrashStackTrace current = null;

    private String message = null;
    private String className = null;
    private String methodName = null;
    private String fileName = null;
    private int lineNumber = 0;

    /**
     * Creates a new stack trace collector.
     */
    public StackTracesCollectorListener() {
        stackTraces = new ArrayList<>();
    }

    @Override
    public void enterStackTraces(StackTracesParser.StackTracesContext ctx) {
        stackTraces.clear();
    }

    @Override
    public void enterRootStackTrace(StackTracesParser.RootStackTraceContext ctx) {
        current = new CrashStackTrace();
        stackTraces.add(current);
    }

    @Override
    public void exitAtLine(StackTracesParser.AtLineContext ctx) {
        String className = this.className;
        String methodName = this.methodName;
        Frame frame = new Frame(className, methodName);
        frame.setLocation(fileName, lineNumber);
        current.addFrame(frame);
    }

    @Override
    public void exitEllipsisLine(StackTracesParser.EllipsisLineContext ctx) {
        int more = Integer.parseInt(ctx.NUMBER().getText());
        Frame frame = new EllipsisFrame(more);
        current.addFrame(frame);
    }

    @Override
    public void enterCausedByLine(StackTracesParser.CausedByLineContext ctx) {
        CrashStackTrace cause = new CrashStackTrace();
        current.setCause(cause);
        current = cause;
    }

    @Override
    public void exitMessageLine(StackTracesParser.MessageLineContext ctx) {
        current.setExceptionClass(ctx.qualifiedClass().getText());
        current.setErrorMessage(message);
        message = null;
    }

    @Override
    public void enterQualifiedClass(StackTracesParser.QualifiedClassContext ctx) {
        className = ctx.getText();
    }

    @Override
    public void enterFileLocation(StackTracesParser.FileLocationContext ctx) {
        fileName = ctx.fileName().getText();
        lineNumber = Integer.parseInt(ctx.NUMBER().getText());
    }

    @Override
    public void enterIsNative(StackTracesParser.IsNativeContext ctx) {
        fileName = null;
        lineNumber = Frame.IS_NATIVE;
    }

    @Override
    public void enterIsUnknown(StackTracesParser.IsUnknownContext ctx) {
        fileName = null;
        lineNumber = Frame.IS_UNKNOWN;
    }

    @Override
    public void enterConstructor(StackTracesParser.ConstructorContext ctx) {
        methodName = ctx.getText();
    }

    @Override
    public void enterMethodName(StackTracesParser.MethodNameContext ctx) {
        methodName = ctx.getText();
    }

    @Override
    public void enterMessage(StackTracesParser.MessageContext ctx) {
        int start = ctx.start.getStartIndex();
        int stop = ctx.stop.getStopIndex();
        Interval interval = new Interval(start, stop);
        message = ctx.start.getInputStream().getText(interval);
    }

    /**
     * Returns the collected stack traces. The list is cleared each time the collector is used by a walker.
     *
     * @return The collected stack traces.
     */
    public List<CrashStackTrace> getStackTraces() {
        return stackTraces;
    }
}
