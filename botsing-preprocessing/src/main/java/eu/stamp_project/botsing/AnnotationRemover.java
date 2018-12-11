package eu.stamp_project.botsing;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

public class AnnotationRemover implements STProcessor {
	ClassReader reader;
	private static AnnotationRemover instance = new AnnotationRemover();

	public static AnnotationRemover get() {
		return instance;
	}

	private AnnotationRemover() {

	}

	@Override
	public List<String> preprocess(List<String> lines) {

		Set<String> classNames = new HashSet<>();
		// find classes to inspect
		for (String f : lines) {
			String ff = f.trim();
			if (!ff.startsWith("at")) {
				continue;
			}
			String fullMethodName = ff.substring("at ".length(), ff.indexOf('('));
			String fullClassName = fullMethodName.substring(0, fullMethodName.lastIndexOf('.'));
			classNames.add(fullClassName);
		}
		// maps the class names with the lines belonging to annotations
		Map<String, Set<Integer>> classes = new HashMap<>();
		for (String clazz : classes.keySet()) {
			classes.put(clazz, getAnnotationLines(clazz));
		}
		List<String> newLines = new ArrayList<>();
		// filter out matching frames
		classes.forEach((clazz, set) -> {
			set.forEach(ln -> { 
				lines.removeIf(l -> l.matches("\\s*at\\s"+clazz+".*\\:"+l+"\\)"));
			});
		});
		return newLines;
	}

	private Set<Integer> getAnnotationLines(String clazz) {
		// TODO load classpath
		InputStream in = AnnotationRemover.class.getResourceAsStream("classpath" + clazz + ".class");
		try {
			reader = new ClassReader(in);
		} catch (IOException e) {
			e.printStackTrace();
			// be conservative
			return new HashSet<>();
		}
		AnnotationScanner classVisitor = new AnnotationScanner(Opcodes.ASM4);
		reader.accept(classVisitor, 0);
		return classVisitor.getLines();
	}

}