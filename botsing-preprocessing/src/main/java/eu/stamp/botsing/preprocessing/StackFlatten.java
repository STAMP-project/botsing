package eu.stamp.botsing.preprocessing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StackFlatten implements STProcessor {
	private static final String MORE = " more";
	static final String CAUSED_BY_PREFIX = "Caused by: ";

	private static StackFlatten instance = new StackFlatten();
	public static StackFlatten get() {
		return instance;
	}

	private StackFlatten() {
	}

	@Override
	public List<String> preprocess(List<String> lines) {
		if (lines.size() < 2) {
			return lines;
		}
		List<List<String>> chuncks = splitLines(lines, CAUSED_BY_PREFIX);
		return flatten(chuncks);
	}

	/*
	 * cleans the stack trace chunck removing the caused by prefix and the "...
	 * X more" line
	 */
	List<String> cleanNestedChunk(List<String> chunk) throws IndexOutOfBoundsException {
		// removes the caused by prefix from the first line
		int size = chunk.size();
		chunk.set(0, chunk.get(0).replace(CAUSED_BY_PREFIX, ""));
		if (chunk.get(size - 1).endsWith(MORE)) {
			chunk.remove(size - 1);
		}
		return chunk;
	}

	String findCauseMethod(List<String> chunk) throws IndexOutOfBoundsException {
		String lastFrame = chunk.get(chunk.size() - 1);
		int splitPoint = lastFrame.indexOf('(');
		return lastFrame.substring(0, splitPoint);
	}

	/*
	 * searches a list and returns the index of the first line starting with a
	 * given prefix
	 */
	int findElementWithPrefix(List<String> lines, String prefix) {
		String target = lines.stream().filter(l -> l.startsWith(prefix)).findFirst().orElse(null);
		return target != null ? lines.indexOf(target) : -1;
	}

	/*
	 * Takes a stack trace splitted at every chained exception and produces a
	 * flat stack trace with the root cause at the top
	 */
	List<String> flatten(List<List<String>> splittedTrace) {
		//case of empty trace
		if (splittedTrace.isEmpty()) {
			return Collections.emptyList();
		}
		List<String> orderedList = new ArrayList<>();
		orderedList.addAll(splittedTrace.get(0));
		for (List<String> chunk : splittedTrace) {
			if (chunk.isEmpty() || !chunk.get(0).startsWith(CAUSED_BY_PREFIX)) {
				continue;
			}
			chunk = cleanNestedChunk(chunk);
			String method = findCauseMethod(chunk);

			// look for first occurrence of method in main_chunck
			int targetIndex = findElementWithPrefix(orderedList, method);

			// remove all frames until the one containing the target method
			orderedList.subList(0, targetIndex + 1).clear();
			orderedList.addAll(0, chunk);
		}
		return orderedList;
	}

	/*
	 * Divides a list of strings into chunks, based on a prefix. If the first
	 * line starts with the prefix, the first chunk is empty
	 */
	List<List<String>> splitLines(List<String> lines, String prefix) {
		List<List<String>> subSets = new ArrayList<List<String>>();
		List<String> chunk = new ArrayList<>();
		for (String line : lines) {
			if (line.startsWith(prefix)) {
				subSets.add(chunk);
				chunk = new ArrayList<>();
			}
			chunk.add(line);
		}
		subSets.add(chunk);
		return subSets;
	}

}
