package org.github.cradloff.scanutils;

import java.io.*;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;

/** Hilfsklasse für Dateizugriffe */
public class FileAccess {

	static File find(File basefile, String filename) throws IOException {
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
	static Bag<String> readDict(File basefile, String filename) throws IOException {
		Bag<String> dict = new HashBag<>();
		File file = find(basefile, filename);
		while (file != null) {
			Bag<String> dict2 = readDict(file);
			System.out.printf("verwende Wörterbuch %s (%,d Einträge)%n", file.getPath(), dict2.uniqueSet().size());
			dict.addAll(dict2);
			file = find(file.getParentFile().getParentFile(), filename);
		}

		if (dict.isEmpty()) {
			System.out.printf("Warnung: Wörterbuch %s ist leer%n", filename);
		}

		return dict;
	}

	static Bag<String> readDict(File file) throws IOException {
		Bag<String> dict = new HashBag<>();
		NumberFormat nf = NumberFormat.getIntegerInstance();
		readFile(file, line -> {
			String word;
			int count;
			if (line.contains("\t")) {
				String t[] = line.split("\t");
				word = t[0];
				try {
					count = nf.parse(t[1]).intValue();
				} catch (ParseException e) {
					throw new RuntimeException(e);
				}
			} else {
				word = line.trim();
				count = 1;
			}
			dict.add(word, count);
		});

		return dict;
	}

	/**
	 * Liest eine CSV-Datei ein und liefert sie als Map zurück.
	 */
	static Map<String, String> readCSV(File file) throws IOException {
		return readCSV(file, "\\s");
	}
	static Map<String, String> readCSV(File file, String separator) throws IOException {
		Map<String, String> map = new HashMap<>();
		readFile(file, line -> {
					String[] s = line.trim().split(separator, 2);
					map.put(s[0], s[1]);
				});

		return map;
	}

	static Map<String, String> readRechtschreibungCSV(File basefile) throws IOException {
		String filename = "rechtschreibung.csv";
		File file = FileAccess.find(basefile, filename);
		if (file == null) {
			System.out.printf("Datei %s nicht gefunden.%n", filename);
			return Collections.emptyMap();
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

	/**
	 * Liest eine Datei ein. Die Datei enthält Abschnitte, die in eckigen Klammern stehen,
	 * Leerzeilen und Zeilen die mit # beginnen, werden ignoriert. Beispiel:
	 * <pre>
	 * [Abschnitt 1]
	 * Zeile 1
	 * # Kommentar
	 * Zeile 2
	 * [Abschnitt 2]
	 * Zeile 3
	 */
	static Map<String, List<String>> readConfig(String filename) throws IOException {
		try (
				InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
				) {
			if (is == null) {
				throw new FileNotFoundException(filename);
			}

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.defaultCharset()))) {
				Map<String, List<String>> result = new HashMap<>();
				List<String> lines = new ArrayList<>();
				result.put(null, lines);
				Pattern pattern = Pattern.compile("\\[(.*)\\]");
				for (String line = reader.readLine(); line != null; line = reader.readLine()) {
					if (line.trim().isEmpty() || line.startsWith("#")) {

					} else {
						Matcher matcher = pattern.matcher(line);
						if (matcher.matches()) {
							String group = matcher.group(1);
							lines = new ArrayList<>();
							result.put(group, lines);
						} else {
							lines.add(line);
						}
					}
				}

				return result;
			}
		}
	}

	/** Liest die übergebene Datei zeilenweise ein. Leerzeilen werden übersprungen */
	public static List<String> readLines(File file) throws IOException {
		List<String> lines = new ArrayList<>();
		readFile(file, line -> lines.add(line));

		return lines;
	}

	private static void readFile(File file, Consumer<String> c) throws IOException {
		try (FileReader fr = new FileReader(file);
				BufferedReader br = new BufferedReader(fr);) {
			for (String line = br.readLine(); line != null; line = br.readLine()) {
				if (! line.isBlank()) {
					c.accept(line);
				}
			}
		}
	}

	static File basedir(File basefile) throws IOException {
		File dir = basefile.getAbsoluteFile();
		if (dir.isFile()) {
			dir = dir.getParentFile();
		}
		return dir.getCanonicalFile();
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
	public static File checkExists(String filename) {
		File result;
		File f = new File(filename);
		if (f.exists()) {
			result = f;
		} else {
			result = null;
			System.err.printf("Datei %s existiert nicht!%n", filename);
		}

		return result;
	}

	/** Prüft, ob die angegebenen Dateien existieren. Die gefundenen Dateien werden in der Liste zurückgeliefert. */
	public static List<File> checkExists(String[] filenames) {
		List<File> result = new ArrayList<>();
		for (String filename : filenames) {
			File f = checkExists(filename);
			if (f != null) {
				result.add(f);
			}
		}

		return result;
	}

	static void writeDictionary(File file, Bag<String> dictionary) throws IOException {
		try (FileWriter writer = new FileWriter(file);
				PrintWriter out = new PrintWriter(writer)) {
			for (String entry : dictionary.uniqueSet()) {
				out.print(entry);
				out.print("\t");
				out.println(dictionary.getCount(entry));
			}
		}
	}
}
