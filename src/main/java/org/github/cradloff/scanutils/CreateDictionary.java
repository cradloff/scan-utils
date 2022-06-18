package org.github.cradloff.scanutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.TreeBag;

/**
 * Liest in einem Verzeichnis alle .md-Dateien ein und extrahiert daraus die Wörter für ein Wörterbuch.
 */
public class CreateDictionary {
	public static void main(String... args) throws IOException {
		long start = System.currentTimeMillis();
		if (args.length > 1) {
			System.out.println("Aufruf: CreateDictionary <Verzeichnis>");

			return;
		}

		String dirName = args.length == 0 ? "." : args[0];
		File baseDir = new File(dirName);
		if (! baseDir.exists()) {
			System.out.printf("Verzeichnis %s ist nicht vorhanden!%n", dirName);

			return;
		}
		if (! baseDir.isDirectory()) {
			System.out.printf("%s ist kein Verzeichnis!%n", dirName);

			return;
		}

		// nach Markdown-Dateien suchen
		Bag<String> dictionary = new TreeBag<>(new CaseInsensitiveComparator());
		addWords(baseDir, dictionary);

		File file = new File(baseDir, "german.dic");
		FileAccess.writeDictionary(file, dictionary);
		System.out.printf("Wörterbuch %s erstellt (%,d Wörter, %,d ms)%n", file.getPath(), dictionary.uniqueSet().size(),
				(System.currentTimeMillis() - start));
	}

	static void addWords(File baseDir, Bag<String> dictionary) throws IOException {
		// erst nach Dateien suchen
		File[] files = baseDir.listFiles(
				(dir, name) -> name.endsWith(".md"));

		for (File file : files) {
			readWords(file, dictionary);
		}

		// weitere Unterverzeichnisse suchen
		File[] directories = baseDir.listFiles(
				path -> path.isDirectory());

		for (File directory : directories) {
			addWords(directory, dictionary);
		}
	}

	static void readWords(File file, Collection<String> words) throws IOException {
		try (FileReader fr = new FileReader(file);
				BufferedReader reader = new BufferedReader(fr);) {
			String line = reader.readLine();
			do {
				// Zeile in Token zerlegen
				List<String> tokens = TextUtils.split(line);
				// nur Token, die mit einem Buchstaben beginnen, hinzufügen
				for (String s : tokens) {
					if (Character.isAlphabetic(s.charAt(0))) {
						// ggf. ein non-breaking-space am Ende entfernen (z.B. 'dann&nbsp;...')
						if (s.endsWith("&nbsp")) {
							s = s.substring(0, s.length() - 5);
						}
						words.add(s);
					}
				}

				line = reader.readLine();
			}
			while (line != null);
		}
	}

	private enum State { WHITESPACE, WORD, OTHER }
	public static List<String> split(String line) {
		StringBuilder sb = new StringBuilder();
		List<String> result = new ArrayList<>();
		State state = State.WHITESPACE;
		for (int i = 0; i < line.length(); i++) {
			char ch = line.charAt(i);
			State newState;
			if (Character.isWhitespace(ch)) {
				newState = State.WHITESPACE;
			}
			else if (Character.isLetter(ch)) {
				newState = State.WORD;
			} else {
				newState = State.OTHER;
			}
			if (state != newState && i > 0) {
				if (state == State.WORD) {
					result.add(sb.toString());
				}
				sb.setLength(0);
			}
			sb.append(ch);
			state = newState;
		}
		if (sb.length() > 0) {
			if (state == State.WORD) {
				result.add(sb.toString());
			}
		}

		return result;
	}
}
