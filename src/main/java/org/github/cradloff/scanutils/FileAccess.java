package org.github.cradloff.scanutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Hilfsklasse für Dateizugriffe */
public class FileAccess {

	static File find(File basefile, String filename) throws FileNotFoundException {
		if (basefile == null) {
			return null;
		}

		// Die Datei rekursiv nach oben suchen
		File dir = basedir(basefile);
		File file = new File(dir, filename);
		if (file.exists()) {
			return file;
		}

		return find(dir.getParentFile(), filename);
	}

	/**
	 * Liest das Wörterbuch relativ zur angegebenen Datei ein.
	 */
	static Set<String> readDict(File basefile, String filename) throws IOException {
		Set<String> dict = new HashSet<>();
		File file = find(basefile, filename);
		while (file != null) {
			Set<String> dict2 = new HashSet<>();
			try (FileReader fr = new FileReader(file);
					BufferedReader br = new BufferedReader(fr);) {
				for (String line = br.readLine(); line != null; line = br.readLine()) {
					dict2.add(line.trim());
				}
			}
			System.out.printf("verwende Wörterbuch %s (%,d Einträge)%n", file.getPath(), dict2.size());
			dict.addAll(dict2);
			file = find(file.getParentFile().getParentFile(), filename);
		}

		if (dict.isEmpty()) {
			throw new FileNotFoundException(filename);
		}

		return dict;
	}

	/**
	 * Liest eine CSV-Datei ein und liefert sie als Map zurück.
	 */
	static Map<String, String> readCSV(File file) throws IOException {
		Map<String, String> map = new HashMap<>();
		try (FileReader fr = new FileReader(file);
				BufferedReader br = new BufferedReader(fr);) {
			for (String line = br.readLine(); line != null; line = br.readLine()) {
				line = line.trim();
				if (! line.isEmpty()) {
					String[] s = line.split("\\s", 2);
					map.put(s[0], s[1]);
				}
			}
		}

		return map;
	}

	static Map<String, String> readRechtschreibungCSV(File basefile) throws IOException {
		String filename = "rechtschreibung.csv";
		File file = FileAccess.find(basefile, filename);
		if (file == null) {
			throw new FileNotFoundException(filename);
		}
		Map<String, String> rechtschreibung = new HashMap<>();
		while (file != null) {
			Map<String, String> map = FileAccess.readCSV(file);
			rechtschreibung.putAll(map);
			System.out.printf("verwende Rechtschreibung %s (%,d Einträge)%n", file.getPath(), map.size());
			file = FileAccess.find(file.getParentFile().getParentFile(), filename);
		}

		return rechtschreibung;
	}

	static File basedir(File basefile) {
		File dir = basefile.getAbsoluteFile();
		if (dir.isFile()) {
			dir = dir.getParentFile();
		}
		return dir;
	}

	/**
	 * Benennt die Datei um und gibt ihr ein numerisches Suffix. Dabei wird "Datei" umbenannt in "Datei.1".
	 * Wenn "Datei.1" schon existiert, wird diese zuvor in "Datei.2" umbenannt usw. Existiert bereits "Datei.9",
	 * so wird diese gelöscht.
	 * @param datei Umzubenennende Datei
	 * @return neuer Dateiname
	 */
	static File roll(File datei) {
		return roll(datei, datei, 1);
	}

	private static File roll(File base, File curr, int i) {
		File dateiNeu = new File(base.getAbsolutePath() + "." + i);
		if (dateiNeu.exists()) {
			if (i == 9) {
				dateiNeu.delete();
			} else {
				roll(base, dateiNeu, i + 1);
			}
		}

		curr.renameTo(dateiNeu);
		return dateiNeu;
	}

	/** Prüft, ob die angegebenen Dateien existieren. Die gefundenen Dateien werden in der Liste zurückgeliefert. */
	public static List<File> checkExists(String[] filenames) {
		List<File> result = new ArrayList<>();
		for (String filename : filenames) {
			File f = new File(filename);
			if (f.exists()) {
				result.add(f);
			} else {
				System.err.printf("Datei %s existiert nicht!%n", filename);
			}
		}

		return result;
	}
}
