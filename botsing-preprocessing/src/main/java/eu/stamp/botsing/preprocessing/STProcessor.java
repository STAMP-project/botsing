package eu.stamp.botsing.preprocessing;

import java.util.List;

public interface STProcessor {

	List<String> preprocess(List<String> lines, String regexp);

}
