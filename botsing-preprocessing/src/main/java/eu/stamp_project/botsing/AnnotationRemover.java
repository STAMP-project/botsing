package eu.stamp_project.botsing;

import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

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
		
		return null;
	}

	
}