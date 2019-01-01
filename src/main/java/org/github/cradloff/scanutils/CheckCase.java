package org.github.cradloff.scanutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Korrigiert die Groß-/Kleinschreibung */
public class CheckCase {
	public static void main(String[] args) throws IOException {
		long start = System.currentTimeMillis();
		if (args.length < 1) {
			System.out.println("Aufruf: CheckCase <Dateiname(n)>");
			return;
		}

		List<File> inputs = FileAccess.checkExists(args);
		// keine Dateien gefunden?
		if (inputs.isEmpty()) {
			return;
		}

		// Wörterbuch einlesen
		File basedir = FileAccess.basedir(inputs.get(0));
		Set<String> dict = FileAccess.readDict(basedir, "kleinschreibung.txt");

		for (File input : inputs) {
			System.out.println("Verarbeite Datei " + input.getPath());
			// Datei umbenennen
			File backup = FileAccess.roll(input);
			try (Reader in = new FileReader(backup);
					Writer out = new FileWriter(input);) {
				int count = new CheckCase().checkCase(in, out, dict);

				System.out.printf("Anzahl ersetzter Wörter: %,d, Zeit: %,dms%n",
						count, (System.currentTimeMillis() - start));
			}
		}
	}

	int checkCase(Reader in, Writer out, Set<String> dict) throws IOException {
		BufferedReader reader = new BufferedReader(in);
		PrintWriter writer = new PrintWriter(out);
		String line = reader.readLine();
		List<String> lastLine = Collections.emptyList();
		int count = 0;
		do {
			// Satzzeichen ersetzen
			line = TextUtils.satzzeichenErsetzen(line);
			// Zeile in Token zerlegen
			List<String> s = TextUtils.split(line);
			// Großschreibung durch Kleinschreibung ersetzen
			count += fixCase(lastLine, s, dict);
			for (int i = 0; i < s.size(); i++) {
				writer.write(s.get(i));
			}

			writer.println();
			lastLine = s;
			line = reader.readLine();
		}
		while (line != null);

		return count;
	}

	/** Ersetzt bestimmte Wörter durch ihre Groß-/Kleinschreibweise, wenn sie nicht am Satzanfang stehen */
	public static int fixCase(List<String> lastLine, List<String> line, Set<String> dict) {
		// alle Wörter, die nicht am Zeilenanfang oder nach einem Punkt kommen, durch Kleinschreibweise ersetzen
		int count = 0;
		for (int i = 0; i < line.size(); i++) {
			String word = line.get(i);
			String lcWord = word.toLowerCase();
			String ucWord = Character.toUpperCase(word.charAt(0)) + word.substring(1);
			// das Wort beginnt mit einem Großbuchstaben, ist im Wörterbuch klein vorhanden?
			if (Character.isUpperCase(word.charAt(0))
					&& dict.contains(lcWord) && ! satzAnfang(lastLine, line, i)) {
				line.set(i, lcWord);
				count++;
			}
			// das Wort beginnt mit einem Kleinbuchstaben und ist im Wörterbuch groß vorhanden?
			else if (Character.isLowerCase(word.charAt(0))
					&& dict.contains(ucWord)) {
				line.set(i, ucWord);
				count++;
			}
		}

		return count;
	}

	/** Satzzeichen, die einen Satz beenden ('>' beendet ein Tag) */
	private static final Set<String> SATZZEICHEN = new HashSet<>(Arrays.asList(".", "!", "?", ":", "»", "«", ">"));
	/** Prüft, ob das übergebene Wort am Satzanfang steht */
	static boolean satzAnfang(List<String> lastLine, List<String> line, int i) {
		boolean satzAnfang = false;
		boolean wortVorhanden = false;
		for (int pos = i - 1; pos >= 0 && ! satzAnfang && ! wortVorhanden; pos--) {
			String word = line.get(pos);
			if (SATZZEICHEN.contains(word)) {
				satzAnfang = true;
			} else if (Character.isLetter(word.charAt(0))) {
				wortVorhanden = true;
			}
		}

		// weder Punkt noch Wort gefunden? -> vorhergehende Zeile analysieren
		if (! satzAnfang && ! wortVorhanden) {
			// Leerzeile?
			if (lastLine.isEmpty()) {
				satzAnfang = true;
			} else {
				for (int pos = lastLine.size() - 1; pos >= 0 && ! satzAnfang && ! wortVorhanden; pos--) {
					String word = lastLine.get(pos);
					if (SATZZEICHEN.contains(word)) {
						satzAnfang = true;
					} else if (Character.isLetter(word.charAt(0))) {
						wortVorhanden = true;
					}
				}
			}
		}

		// immer noch nichts gefunden?
		if (! wortVorhanden) {
			satzAnfang = true;
		}

		return satzAnfang;
	}
}
