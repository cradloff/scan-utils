package org.github.cradloff.scanutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Liest in einem Verzeichnis alle .md-Dateien ein und extrahiert daraus die Wörter für ein Wörterbuch.
 */
public class CreateDictionary {
	public static void main(String... args) throws IOException {
		long start = System.currentTimeMillis();
		if (args.length != 1) {
			System.out.println("Aufruf: CreateDictionary <Verzeichnis>");

			return;
		}

		File baseDir = new File(args[0]);
		if (! baseDir.exists()) {
			System.out.printf("Verzeichnis %s ist nicht vorhanden!%n", args[0]);

			return;
		}
		if (! baseDir.isDirectory()) {
			System.out.printf("%s ist kein Verzeichnis!%n", args[0]);

			return;
		}

		// nach Markdown-Dateien suchen
		Set<String> dictionary = new TreeSet<>();
		addWords(baseDir, dictionary);

		File file = new File(baseDir, "german.dic");
		writeDictionary(file, dictionary);
		System.out.printf("Wörterbuch %s erstellt (%,d Wörter, %,d ms)%n", file.getPath(), dictionary.size(),
				(System.currentTimeMillis() - start));
	}

	private static void writeDictionary(File file, Set<String> dictionary) throws IOException {
		try (FileWriter writer = new FileWriter(file);
				PrintWriter out = new PrintWriter(writer)) {
			for (String entry : dictionary) {
				out.println(entry);
			}
		}
	}

	private static void addWords(File baseDir, Set<String> dictionary) throws IOException {
		// erst nach Dateien suchen
		File[] files = baseDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".md");
			}
		});

		for (File file : files) {
			readWords(file, dictionary);
		}

		// weitere Unterverzeichnisse suchen
		File[] directories = baseDir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});

		for (File directory : directories) {
			addWords(directory, dictionary);
		}
	}

	static void readWords(File file, Collection<String> words) throws IOException {
//		System.out.println("Lese " + file.getAbsolutePath());
		try (FileReader fr = new FileReader(file);
				BufferedReader reader = new BufferedReader(fr);) {
			String line = reader.readLine();
			do {
				// Zeile in Token zerlegen
				List<String> s = split(line);
				words.addAll(s);

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
