package org.github.cradloff.scanutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Ersetzt falsch geschriebene Worte durch die korrekte Version.
 * Die Klasse benötigt ein Wörterbuch (german.dic) und eine Datei mit Rechtschreib-Korrekturen (rechtschreibung.csv)
 * im Verzeichnis der Eingabedatei oder darüber.
 * Das Wörterbuch kann z.B. von hier bezogen werden: https://sourceforge.net/projects/germandict/
 * Die Rechtschreib-Korrekturen müssen jeweils den Suchbegriff und die Ersetzung in einer Zeile, durch Tab getrennt, enthalten.
 */
public class PreProcess
{
	public static void main(String[] args) throws IOException {
		long start = System.currentTimeMillis();
		if (args.length != 1) {
			System.out.println("Aufruf: PreProcess <Dateiname>");
			return;
		}

		File input = new File(args[0]);
		if (! input.exists()) {
			System.out.println("Datei " + args[0] + " nicht gefunden!");
			return;
		}

		System.out.println("Verarbeite Datei " + args[0]);

		// CSV-Datei mit Rechtschreib-Fehlern einlesen
		Map<String, String> map = readCSV(input);
		// Wörterbuch einlesen
		Set<String> dict = FileAccess.readDict(input, "german.dic");

		// Datei umbenennen
		String backup = args[0].substring(0, args[0].lastIndexOf('.')) + ".bak";
		input.renameTo(new File(backup));
		try (Reader in = new FileReader(new File(backup));
				Writer out = new FileWriter(input);) {
			int count = new PreProcess().preProcess(in, out, map, dict);

			System.out.printf("Anzahl ersetzter Wörter: %,d, Zeit: %,dms%n",
					count, (System.currentTimeMillis() - start));
		}
	}

	private static Map<String, String> readCSV(File basefile) throws IOException {
		Map<String, String> map = new HashMap<>();
		File file = FileAccess.find(basefile, "rechtschreibung.csv");
		try (FileReader fr = new FileReader(file);
				BufferedReader br = new BufferedReader(fr);) {
			for (String line = br.readLine(); line != null; line = br.readLine()) {
				line = line.trim();
				if (! line.isEmpty()) {
					String[] s = line.split("\t");
					map.put(s[0], s[1]);
				}
			}
		}
		System.out.printf("verwende Rechtschreibung %s (%,d Einträge)%n", file.getPath(), map.size());

		return map;
	}

	public int preProcess(Reader in, Writer out, Map<String, String> map, Set<String> dict) throws IOException {
		BufferedReader reader = new BufferedReader(in);
		PrintWriter writer = new PrintWriter(out);
		String line = reader.readLine();
		List<String> lastLine = Collections.emptyList();
		int count = 0;
		do {
			// Zeile in Token zerlegen
			List<String> s = split(line);
			// 7er etc. ersetzen
			s = replaceSeven(s);
			// Großschreibung durch Kleinschreibung ersetzen
			s = toLower(lastLine, s);

			for (int i = 0; i < s.size(); i++) {
				String t = s.get(i);
				// ggf. Bindestriche entfernen, außer am Wortende
				String word = removeDashes(t);
				// Original-Token in Map?
				if (map.containsKey(t)) {
					count++;
					writer.print(map.get(t));
				}
				// Wort ohne Bindestriche in Map?
				else if (map.containsKey(word)) {
					count++;
					writer.print(map.get(word));
				}
				// Wort ohne Bindestriche im Wörterbuch?
				else if (dict.contains(word)) {
					writer.print(word);
				} else {
					writer.print(t);
				}
			}

			writer.println();
			lastLine = s;
			line = reader.readLine();
		}
		while (line != null);

		return count;
	}

	private static final Pattern SEVEN = Pattern.compile(".*\\D7$");
	private static final Pattern SEVEN_PLUS = Pattern.compile(".*\\D7[il]$");
	static List<String> replaceSeven(List<String> input) {
		List<String> result = new ArrayList<>();
		for (String s : input) {
			// '7' am Wortende durch '?' ersetzen
			if (SEVEN_PLUS.matcher(s).matches()) {
				result.add(s.substring(0, s.length() - 2));
				result.add("?!");
			} else if (SEVEN.matcher(s).matches()) {
				result.add(s.substring(0, s.length() - 1));
				result.add("?");
			} else {
				result.add(s);
			}
		}

		return result;
	}

	/** Wörter für toLower */
	private static final Set<String> LOWER = new HashSet<>(Arrays.asList(
			"Ein", "Zwei", "Drei", "Vier", "Fünf", "Sechs", "Sieben", "Acht", "Neun", "Zehn",
			"Und", "Uns", "Unsere", "Von", "Vor", "Voran", "Wir", "Zu", "Zur", "Zusammen"));
	/** Ersetzt bestimmte Wörter durch ihnre Kleinschreibweise, wenn sie nicht am Satzanfang stehen */
	public static List<String> toLower(List<String> lastLine, List<String> line) {
		// alle Wörter, die nicht am Zeilenanfang oder nach einem Punkt kommen, durch Kleinschreibweise ersetzen
		for (int i = 0; i < line.size(); i++) {
			// das Wort beginnt mit einem Großbuchstaben, ist in der Liste vorhanden?
			String word = line.get(i);
			if (Character.isUpperCase(word.charAt(0))
					&& LOWER.contains(word) && ! satzAnfang(lastLine, line, i)) {
				String lcWord = word.toLowerCase();
				line.set(i, lcWord);
//				System.out.println("Ersetze " + word + " durch " + lcWord + "; " + String.join("", line));
			}
		}
		return line;
	}

	/** Satzzeichen, die einen Satz beenden ('>' beendet ein Tag) */
	private static final Set<String> SATZZEICHEN = new HashSet<>(Arrays.asList(".", "!", "?", ":", ">"));
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

	static String removeDashes(String word) {
		StringBuilder sb = new StringBuilder(word.length());
		char last = ' ';
		for (int i = 0; i < word.length(); i++) {
			char ch = word.charAt(i);
			if (ch == '-' && i < word.length() - 1 && last != '\\') {
				;
			} else {
				sb.append(ch);
			}
			last = ch;
		}

		return sb.toString();
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
			// Backslash, Bindestrich und Ziffern werden wie Buchstabe behandelt
			else if (Character.isLetter(ch) || ch == '-' || ch == '\\' || Character.isDigit(ch)) {
				newState = State.WORD;
			} else {
				newState = State.OTHER;
			}
			if ((state != newState || newState == State.OTHER) && i > 0) {
				result.add(sb.toString());
				sb.setLength(0);
			}
			sb.append(ch);
			state = newState;
		}
		if (sb.length() > 0) {
			result.add(sb.toString());
		}

		return result;
	}
}
