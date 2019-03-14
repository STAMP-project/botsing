package eu.stamp.botsing;

import java.util.List;

public class ErrorMessage implements STProcessor {
	private final static String EXCEPTION_PREFIX = "Exception in thread";
	
	private static ErrorMessage instance = new ErrorMessage();

	public static ErrorMessage get() {
		return instance;
	}

	private ErrorMessage() {
	}

	@Override
	public List<String> preprocess(List<String> lines, String regexp) {
		if (lines.size() < 1) {
			return lines;
		}
		String head = lines.get(0);
		// remove thread info 
		if (head.startsWith(EXCEPTION_PREFIX)) {
			head = head.replaceAll(EXCEPTION_PREFIX + " \".*\" ", "");
		}
		if (head.contains(":")) {
			head = head.replaceAll("\\:.*", "");
		}
		lines.set(0, head);
		return lines;
	}

}
