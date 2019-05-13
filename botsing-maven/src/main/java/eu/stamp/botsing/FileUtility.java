package eu.stamp.botsing;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

public class FileUtility {

	/**
	 * @param file
	 * @return number of rows in the file
	 * @throws IOException
	 */
	public static long getRowNumber(String file) throws IOException {

		try ( Stream<String> lines = Files.lines(Paths.get(file), Charset.defaultCharset()) ) {
			return lines.count();
		}
	}

	/**
	 * Search inside the files of the folder and in subfolders for the regex
	 *
	 * @param folder
	 * @param regex
	 * @return true if the regex is found inside any file
	 * @throws IOException
	 */
	public static boolean search(String folder, String regex, String[] extensions) throws IOException {

		Collection<File> files = FileUtils.listFiles(new File(folder), extensions, true);

        for (File file : files) {

        	if (searchInFile(file, regex)) {
				return true;
			}
        }

		return false;
	}

	protected static boolean searchInFile(File file, String regex) throws IOException {
		boolean result = false;

		LineIterator it = FileUtils.lineIterator(file, "UTF-8");

		try {
			while (it.hasNext()) {
				String line = it.nextLine();
				if (line.matches(regex)) {
					result =  true;
					break;
				}
			}

		} finally {
			LineIterator.closeQuietly(it);
		}

		return result;
	}

	public static void deleteFolder(String testDir) throws IOException {
		FileUtils.deleteDirectory(new File(testDir));
	}
}
