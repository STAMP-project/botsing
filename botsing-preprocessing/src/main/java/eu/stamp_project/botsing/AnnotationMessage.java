package eu.stamp_project.botsing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class AnnotationMessage implements STProcessor {

	private final static String JAVA = "java";
	private final static String DOT = ".";
	private final static String SLASH = "/";

	private final static String PREFIX_PATH_CLASSES = "/java/";
	private final static String AT = "at";
	private final static String ANNOTATION = "@";
	private final static String COLON = ":";

	private static HashMap<String, HashMap<Integer, String>> annotations = new HashMap<>();
	private static AnnotationMessage instance = new AnnotationMessage();

	public static AnnotationMessage get(String source) {
		
		annotations = getAnnotations(source);
		return instance;
	}

	private AnnotationMessage() {
	}

	@Override
	public List<String> preprocess(List<String> lines) {

		List<String> removedAnnotation = new ArrayList<String>();

		for (String f : lines) {
			String ff = f.trim();
			if (!ff.startsWith(AT)) {
				continue;
			}
			String fullMethodName = ff.substring(AT.length() + 1, ff.indexOf('('));
			String fullClassName = fullMethodName.substring(0, fullMethodName.lastIndexOf(DOT));

			if (ff.contains(COLON)) {// read the line if contains the ":"
				String line = ff.substring(ff.indexOf(COLON) + 1, ff.indexOf(')'));
				if (line.matches("\\d+")) { // if it's a number
					if (annotations.get(fullClassName) != null && annotations.get(fullClassName).get(new Integer(line)) != null) {
						removedAnnotation.add(f);
					}
				}
			}
		}

		lines.removeAll(removedAnnotation);
		return lines;
	}

	private static boolean isAnnotation(String line) {
		if (line != null) {
			return line.trim().startsWith(ANNOTATION);
		} else
			return false;
	}

	@SuppressWarnings("resource")
	private static HashMap<String, HashMap<Integer, String>> getAnnotations(String zipFile) {
		
		HashMap<String, HashMap<Integer, String>> ann = new HashMap<>();
		
		try {
			ZipFile zf = new ZipFile(zipFile);
			@SuppressWarnings("unchecked")
			Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) zf.entries();
			String filejava = null;

			while (entries.hasMoreElements()) {
				ZipEntry ze = (ZipEntry) entries.nextElement();

				filejava = ze.getName();
				long size = ze.getSize();

				if (size > 0 && filejava.endsWith(DOT + JAVA)) {

					BufferedReader br = new BufferedReader(new InputStreamReader(zf.getInputStream(ze)));
					String line;
					int lineNumber = 0;
					HashMap<Integer, String> l = new HashMap<>();
					while ((line = br.readLine()) != null) {
						++lineNumber;
						if (isAnnotation(line)) {
							l.put(lineNumber, line.trim());
						}
					}
					if (l.size() > 0) {// formatting of class full name
						String keypath = filejava.substring(
								filejava.indexOf(PREFIX_PATH_CLASSES) + PREFIX_PATH_CLASSES.length(),
								filejava.indexOf(DOT + JAVA));
						ann.put(keypath.replaceAll(SLASH, DOT), l);
					}

					br.close();
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return ann;
	}

}
