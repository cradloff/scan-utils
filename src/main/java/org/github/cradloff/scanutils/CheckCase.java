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
		if (args.length != 1) {
			System.out.println("Aufruf: CheckCase <Dateiname>");
			return;
		}

		File input = new File(args[0]);
		if (! input.exists()) {
			System.out.println("Datei " + args[0] + " nicht gefunden!");
			return;
		}

		System.out.println("Verarbeite Datei " + args[0]);

		// Wörterbuch einlesen
		Set<String> lower = FileAccess.readDict(input, "kleinschreibung.txt");

		// Datei umbenennen
		String backup = args[0].substring(0, args[0].lastIndexOf('.')) + ".bak";
		input.renameTo(new File(backup));
		try (Reader in = new FileReader(new File(backup));
				Writer out = new FileWriter(input);) {
			int count = new CheckCase().checkCase(in, out, lower);

			System.out.printf("Anzahl ersetzter Wörter: %,d, Zeit: %,dms%n",
					count, (System.currentTimeMillis() - start));
		}
	}

	private int checkCase(Reader in, Writer out, Set<String> lower) throws IOException {
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
			count += toLower(lastLine, s, lower);

			writer.println();
			lastLine = s;
			line = reader.readLine();
		}
		while (line != null);

		return count;
	}

	/** Ersetzt bestimmte Wörter durch ihre Kleinschreibweise, wenn sie nicht am Satzanfang stehen */
	public static int toLower(List<String> lastLine, List<String> line, Set<String> lower) {
		// alle Wörter, die nicht am Zeilenanfang oder nach einem Punkt kommen, durch Kleinschreibweise ersetzen
		int count = 0;
		for (int i = 0; i < line.size(); i++) {
			// das Wort beginnt mit einem Großbuchstaben, ist in der Liste vorhanden?
			String word = line.get(i);
			String lcWord = word.toLowerCase();
			if (Character.isUpperCase(word.charAt(0))
					&& lower.contains(lcWord) && ! satzAnfang(lastLine, line, i)) {
				line.set(i, lcWord);
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
