package eu.stamp.botsing;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

public class FileUtility {

	/**
	 * Search inside the files of the folder for the regex
	 *
	 * @param folder
	 * @param regex
	 * @return true if the regex is found inside any file
	 * @throws IOException
	 */
	public static boolean search(String folder, String regex) throws IOException {

		File fold = new File(folder);

		for (File f : fold.listFiles()) {

			if (searchInFile(f, regex)) {
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
}
