package eu.stamp_project.botsing;

import java.util.List;

public class ErrorMessage implements STProcessor {
	private static ErrorMessage instance = new ErrorMessage();

	public static ErrorMessage get() {
		return instance;
	}

	private ErrorMessage() {
	}

	@Override
	public List<String> preprocess(List<String> lines) {
		if (lines.size() < 1) {
			return lines;
		}
		String head = lines.get(0);
		// remove thread info
		if (head.startsWith("Exception in thread")) {
			head = head.replaceAll("Exception in thread \".*\" ", "");
		}
		if (head.contains(":")) {
			head = head.replaceAll("\\:.*", "");
		}
		lines.set(0, head);
		return lines;
	}

}
