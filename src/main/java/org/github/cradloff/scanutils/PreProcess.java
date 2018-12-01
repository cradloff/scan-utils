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
import java.util.Collection;
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
		Map<String, String> map = FileAccess.readRechtschreibungCSV(input);
		// Wörterbuch einlesen
		Set<String> dict = FileAccess.readDict(input, "german.dic");

		// Datei umbenennen
		File backup = FileAccess.roll(input);
		try (Reader in = new FileReader(backup);
				Writer out = new FileWriter(input);) {
			int count = new PreProcess().preProcess(in, out, map, dict);

			System.out.printf("Anzahl ersetzter Wörter: %,d, Zeit: %,dms%n",
					count, (System.currentTimeMillis() - start));
		}
	}

	public int preProcess(Reader in, Writer out, Map<String, String> map, Set<String> dict) throws IOException {
		// klein geschriebene Wörter auch in Groß-Schreibweise hinzufügen
		Set<String> ciDict = new HashSet<>(dict);
		for (String s : dict) {
			if (Character.isLowerCase(s.charAt(0))) {
				String t = Character.toUpperCase(s.charAt(0)) + s.substring(1);
				ciDict.add(t);
			}
		}
		BufferedReader reader = new BufferedReader(in);
		PrintWriter writer = new PrintWriter(out);
		String line = reader.readLine();
		int count = 0;
		boolean split = false; // Worttrennung in vorheriger Zeile?
		do {
			// Satzzeichen ersetzen
			line = TextUtils.satzzeichenErsetzen(line);
			// Punkte und Anführungszeichen in Wörtern entfernen
			line = line.replaceAll("(\\p{IsAlphabetic})[.»«](\\p{javaLowerCase})", "$1$2");
			// Zeile in Token zerlegen
			List<String> s = TextUtils.split(line);
			// 7er etc. ersetzen
			count += replaceSeven(s);
			// Brüche ersetzen
			count += replaceFraction(s);

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
				else if (ciDict.contains(word)) {
					writer.print(word);
				}
				// vor und nach Worttrennung am Zeilenende keine weitere Sonderbehandlung
				else if (i == 0 && split
						|| i == s.size() - 1 && endWithDash(t)) {
					writer.print(t);
				} else {
					// überflüssige Buchstaben entfernen
					String candidate = removeSil(word, ciDict);
					// nicht gefunden?
					if (candidate.equals(word)) {
						writer.print(t);
					} else {
						count++;
						writer.print(candidate);
						// wenn das Original mit 'i' oder 'l' geendet hat, kommt wahrscheinlich ein Ausrufezeichen
						if ((word.endsWith("i") || word.endsWith("l"))
								&& ! (candidate.endsWith("i") || candidate.endsWith("l"))) {
							writer.print('!');
						}
					}
				}
			}
			split = endWithDash(line);

			writer.println();
			line = reader.readLine();
		}
		while (line != null);

		return count;
	}

	private static final Pattern SEVEN = Pattern.compile(".*\\D7$");
	private static final Pattern SEVEN_PLUS = Pattern.compile(".*\\D7[il]$");
	static int replaceSeven(List<String> line) {
		int count = 0;
		String nextWord = "";
		for (int i = line.size() - 1; i >= 0; i--) {
			// '7' am Wortende durch '?' ersetzen
			String word = line.get(i);
			if (SEVEN_PLUS.matcher(word).matches()) {
				line.set(i, word.substring(0, word.length() - 2));
				line.add(i + 1, "?!");
				count++;
			} else if (SEVEN.matcher(word).matches()) {
				line.set(i, word.substring(0, word.length() - 1));
				line.add(i + 1, "?");
				count++;
			}
			// ? gefolgt von i oder l
			else if (word.equals("?")
					&& (nextWord.equals("i")
							|| nextWord.equals("l"))) {
				line.remove(i + 1);
				line.set(i, "?!");
				count++;
			}
			nextWord = word;
		}

		return count;
	}

	static String removeDashes(String word) {
		StringBuilder sb = new StringBuilder(word.length());
		char last = ' ';
		for (int i = 0; i < word.length(); i++) {
			char ch = word.charAt(i);
			if (isDash(ch) && i < word.length() - 1 && last != '\\') {
				;
			} else {
				sb.append(ch);
			}
			last = ch;
		}

		return sb.toString();
	}

	private static boolean endWithDash(String s) {
		return s.length() > 0 && isDash(s.charAt(s.length() - 1));
	}

	private static boolean isDash(char ch) {
		return ch == '-' || ch == '—';
	}

	// Map für Brüche: (Zähler, Nenner -> Bruch)
	private static final Map<String, Map<String, String>> FRACTIONS;
	static {
		FRACTIONS = new HashMap<>();
		Map<String, String> map = new HashMap<>();
		map.put("2", "½");
		map.put("3", "⅓");
		map.put("4", "¼");
		map.put("5", "⅕");
		map.put("6", "⅙");
		map.put("7", "⅐");
		map.put("8", "⅛");
		map.put("9", "⅑");
		map.put("10", "⅒");
		FRACTIONS.put("1", map);
		map = new HashMap<>();
		map.put("3", "⅔");
		map.put("5", "⅖");
		FRACTIONS.put("2", map);
		map = new HashMap<>();
		map.put("4", "¾");
		map.put("5", "⅗");
		map.put("8", "⅜");
		FRACTIONS.put("3", map);
		map = new HashMap<>();
		map.put("5", "⅘");
		FRACTIONS.put("4", map);
		map = new HashMap<>();
		map.put("6", "⅚");
		map.put("8", "⅝");
		FRACTIONS.put("5", map);
		map = new HashMap<>();
		map.put("8", "⅞");
		FRACTIONS.put("7", map);
	}
	static int replaceFraction(List<String> words) {
		int count = 0;
		for (int i = words.size() - 2; i > 0; i--) {
			if (words.get(i).equals("/")) {
				// Wörter vor und nach dem Slash in der Map suchen
				String zähler = words.get(i - 1);
				Map<String, String> map = FRACTIONS.get(zähler);
				if (map != null) {
					String nenner = words.get(i + 1);
					String bruch = map.get(nenner);
					if (bruch != null) {
						// Zeichenketten durch Bruch ersetzen
						words.set(i - 1, bruch);
						words.remove(i + 1);
						words.remove(i);
						count++;
					}
				}
			}
		}
		return count;
	}

	/** Entfernt überflüssige 's', 'i' und 'l' aus Wörtern. */
	public static String removeSil(String input, Set<String> dict) {
		// Wörter, die bereits im Wörterbuch enthalten sind, nicht bearbeiten
		if (dict.contains(input) || input.isEmpty()) {
			return input;
		}

		// sonst an allen Positionen die Zeichen entfernen und prüfen, ob sie im Wörterbuch enthalten sind
		String result = "";
		Collection<String> candidates = new ArrayList<>();
		removeSil(input, candidates, input.length() - 1);
		for (String candidate : candidates) {
			if (candidate.length() > result.length()
					&& dict.contains(candidate)) {
				result = candidate;
			}
		}

		return result.isEmpty() ? input : result;
	}

	private static void removeSil(String input, Collection<String> result, int end) {
		for (int i = end; i >= 0; i--) {
			char ch = input.charAt(i);
			if (ch == 's' || ch == 'i' || ch == 'l') {
				StringBuilder sb = new StringBuilder(input);
				String candidate = sb.deleteCharAt(i).toString();
				result.add(candidate);
				removeSil(candidate, result, i - 1);
			}
		}
	}

}
