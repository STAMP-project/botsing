package eu.stamp.botsing.preprocessing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class StackFlatten implements STProcessor {
	static final String MORE = " more";
	protected static final String CAUSED_BY_PREFIX = "Caused by: ";

	private static StackFlatten instance = new StackFlatten();
	public static StackFlatten get() {
		return instance;
	}

	private StackFlatten() {
	}

	@Override
	public List<String> preprocess(List<String> lines, String regexp) {
		if (lines.size() < 2) {
			return lines;
		}
		List<List<String>> chuncks = splitLines(lines, CAUSED_BY_PREFIX);
		return flatten(chuncks, regexp);
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

	/*
	 * Takes a stack trace splitted at every chained exception and produces a
	 * flat stack trace with the root cause at the top
	 */
	List<String> flatten(List<List<String>> splittedTrace, String regexp) {

		//reverse order to get the deep nested stack trace
		Collections.reverse(splittedTrace);
		Pattern pattern = Pattern.compile(regexp);

		for (List<String> chunk : splittedTrace) {
			cleanNestedChunk(chunk);
			for (String line: chunk){
				if (pattern.matcher(line).find()){
					return chunk;
				}
			}
		}
		return Collections.emptyList();
	}

	/*
	 * Divides a list of strings into chunks, based on a prefix. If the first
	 * line starts with the prefix, the first chunk is empty
	 */
	List<List<String>> splitLines(List<String> lines, String prefix) {
		//ArrayList - insertion order
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
