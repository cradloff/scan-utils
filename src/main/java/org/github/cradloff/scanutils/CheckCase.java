package org.github.cradloff.scanutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
		Set<String> dict = FileAccess.readDict(basedir, "german.dic");
		dict = removeAmbigous(dict);

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

	/** Entfernt Wörter, die sowohl in Groß- als auch in Kleinschreibung vorhanden sind */
	static Set<String> removeAmbigous(Set<String> dict) {
		// Wörter mit uneindeutiger Groß-/Kleinschreibung aus dem Ergebnis entfernen
		Set<String> result = new HashSet<>(dict);
		for (String word : dict) {
			if (Character.isLowerCase(word.charAt(0))) {
				String ucWord = TextUtils.toUpperCase(word);
				if (dict.contains(ucWord)) {
					result.remove(word);
					result.remove(ucWord);
				}
			} else {
				String lcWord = word.toLowerCase();
				if (dict.contains(lcWord)) {
					result.remove(word);
					result.remove(lcWord);
				}
			}
		}

		return result;
	}

	int checkCase(Reader in, Writer out, Set<String> dict) throws IOException {
		BufferedReader reader = new BufferedReader(in);
		PrintWriter writer = new PrintWriter(out);
		String line = reader.readLine();
		List<String> lastLine = Collections.emptyList();
		int count = 0;
		do {
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
		boolean tag = false;
		for (int i = 0; i < line.size(); i++) {
			String word = line.get(i);
			// Tags überspringen
			if (tag) {
				if (TextUtils.endOfTag(word)) {
					tag = false;
				}
			} else if (TextUtils.startOfTag(word)) {
				tag = true;
			} else {
				String lcWord = word.toLowerCase();
				String ucWord = TextUtils.toUpperCase(word);
				// das Wort beginnt mit einem Großbuchstaben, ist im Wörterbuch nur klein vorhanden?
				if (Character.isUpperCase(word.charAt(0))
						&& ! dict.contains(ucWord)
						&& dict.contains(lcWord)
						&& satzanfang(lastLine, line, i) == Satzanfang.NEIN) {
					line.set(i, lcWord);
					count++;
				}
				// das Wort beginnt mit einem Kleinbuchstaben und ist im Wörterbuch nur groß vorhanden?
				else if (Character.isLowerCase(word.charAt(0))
						&& (dict.contains(ucWord)
								&& ! dict.contains(lcWord)
								// oder wir befinden uns am Satzanfang
								|| satzanfang(lastLine, line, i) == Satzanfang.JA)) {
					line.set(i, ucWord);
					count++;
				}
			}
		}

		return count;
	}

	public enum Satzanfang {
		JA, NEIN, WEISS_NICHT
	}
	/** Map mit eindeutigen Hinweisen auf Satzanfänge */
	private static final Map<String, Satzanfang> HINTS;
	static {
		HINTS = new HashMap<>();
		HINTS.put(".", Satzanfang.JA);
		HINTS.put(".«", Satzanfang.JA);
		HINTS.put("«.", Satzanfang.JA);
		HINTS.put("!", Satzanfang.JA);
		HINTS.put("?", Satzanfang.JA);
		HINTS.put("?!", Satzanfang.JA);
		HINTS.put("—!", Satzanfang.JA);
		HINTS.put(",", Satzanfang.NEIN);
		HINTS.put(",«", Satzanfang.NEIN);
		HINTS.put(";", Satzanfang.NEIN);
		// nach einer wörtlichen Rede geht manchmal ein Satz weiter
		HINTS.put("!«", Satzanfang.WEISS_NICHT);
		HINTS.put("?«", Satzanfang.WEISS_NICHT);
		HINTS.put("?!«", Satzanfang.WEISS_NICHT);
		HINTS.put("—«", Satzanfang.WEISS_NICHT);
		HINTS.put(":", Satzanfang.WEISS_NICHT);
	}
	/** Prüft, ob das übergebene Wort am Satzanfang steht */
	static Satzanfang satzanfang(List<String> lastLine, List<String> line, int i) {
		Satzanfang satzanfang = satzanfang(line, i);
		// in der aktuellen Zeile nichts gefunden?
		if (satzanfang == null) {
			// ist die vorherige Zeile leer?
			if (lastLine.isEmpty()) {
				satzanfang = Satzanfang.JA;
			} else {
				satzanfang = satzanfang(lastLine, lastLine.size());
			}
		}

		return satzanfang;
	}

	private static Satzanfang satzanfang(List<String> line, int i) {
		boolean tag = false;
		Satzanfang satzanfang = null;
		for (int j = i - 1; j >= 0 && satzanfang == null; j--) {
			String token = line.get(j);
			// Tags überspringen
			if (tag) {
				if (TextUtils.startOfTag(token)) {
					tag = false;
				}
			} else if (TextUtils.endOfTag(token)) {
				tag = true;
			} else if (TextUtils.isWord(token)) {
				satzanfang = Satzanfang.NEIN;
			} else {
				satzanfang = HINTS.get(token);
			}
		}
		return satzanfang;
	}
}
