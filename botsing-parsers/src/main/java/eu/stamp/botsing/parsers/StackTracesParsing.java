package eu.stamp.botsing.parsers;

import eu.stamp.botsing.CrashStackTrace;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.List;

/**
 * Utility class to parse stack traces.
 */
public class StackTracesParsing {

    /**
     * Parses the given input and returns a list of stack traces found in that input.
     * @param input The input to parse.
     * @return A list with the stack traces parsed from the given input.
     */
    public static List<CrashStackTrace> parseStackTraces(String input){
        CharStream text = CharStreams.fromString(input);
        StackTracesLexer lexer = new StackTracesLexer(text);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        StackTracesParser parser = new StackTracesParser(tokens);
        StackTracesParser.StackTracesContext tree = parser.stackTraces();
        // Walk the three to build the stack traces
        StackTracesCollectorListener listener = new StackTracesCollectorListener();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(listener, tree);
        // Return the built list
        return listener.getStackTraces();
    }




}
