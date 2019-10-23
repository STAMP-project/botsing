package eu.stamp.botsing.commons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This class contains Utility methods to handle class paths.
 */
public class ClassPaths {

    private static final Logger LOG = LoggerFactory.getLogger(ClassPaths.class);

    /**
     * Converts a given custom Botsing classpath to a standard classpath, compatible with EvoSuite (i.e., containing
     * only paths to jar files or class files in a proper package directory hierarchy). The format is a list of folder
     * and jar file paths, separated by the local path separator character (see {@link java.io.File}.pathSeparator).
     *
     * @param classPath The custom classpath to process.
     * @return A list of classpath entries, valid for EvoSuite.
     */
    public static List<String> getClassPathEntries(String classPath) {
        List<String> entries = new ArrayList<>();
        // If the classpath is empty, return an empty list
        if(classPath.isEmpty()){
            return entries;
        }
        // Else process entries
        for(String rawEntry : classPath.split(File.pathSeparator)) {
            File file = new File(rawEntry);
            // Check that the entry exists, otherwise skip it
            if(file.exists()) {
                // If the entry is a folder, check for '.jar' files
                if(file.isDirectory()) {
                    // Add jar files to the classpath entries
                    for(File jarFile : file.listFiles((File f) -> f.isFile() && f.getName().endsWith(".jar"))) {
                        entries.add(jarFile.getAbsolutePath());
                    }
                }
                // Add the entry itself
                entries.add(file.getAbsolutePath());
            } else {
                LOG.warn("ClassPath entry {} not found, will skip it.", rawEntry);
            }
        }
        return entries;
    }

}