package eu.stamp.botsing;

import java.util.List;

public interface STProcessor {

	List<String> preprocess(List<String> lines, String regexp);

}
