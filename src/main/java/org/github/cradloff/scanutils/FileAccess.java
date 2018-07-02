package org.github.cradloff.scanutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/** Hilfsklasse für Dateizugriffe */
public class FileAccess {

	static File find(File dir, String filename) throws FileNotFoundException {
		if (dir == null) {
			throw new FileNotFoundException(filename);
		}
	
		// Die Datei rekursiv nach oben suchen
		File file = new File(dir, filename);
		if (file.exists()) {
			return file;
		}
	
		return find(dir.getParentFile(), filename);
	}

	static Set<String> readDict(File dir, String filename) throws IOException {
		Set<String> dict = new HashSet<>();
		File file = find(dir, filename);
		try (FileReader fr = new FileReader(file);
				BufferedReader br = new BufferedReader(fr);) {
			for (String line = br.readLine(); line != null; line = br.readLine()) {
				dict.add(line);
			}
		}
		System.out.printf("verwende Wörterbuch %s (%,d Einträge)%n", file.getPath(), dict.size());
	
		return dict;
	}

}
