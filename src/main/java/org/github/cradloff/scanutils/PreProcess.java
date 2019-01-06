package org.github.cradloff.scanutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Ersetzt falsch geschriebene Worte durch die korrekte Version.
 * Die Klasse benötigt ein Wörterbuch (german.dic) und eine Datei mit Rechtschreib-Korrekturen (rechtschreibung.csv)
 * im Verzeichnis der Eingabedatei oder darüber.
 * Das Wörterbuch kann z.B. von hier bezogen werden: https://sourceforge.net/projects/germandict/
 * Die Rechtschreib-Korrekturen müssen jeweils den Suchbegriff und die Ersetzung in einer Zeile, durch Tab getrennt, enthalten.
 */
public class PreProcess {
	/** leere Liste als Markierung für das Dateiende */
	private static final List<String> EOF = new ArrayList<>();
	/** Zeile mit einem Pagebreak */
	private static final List<String> PAGEBREAK = TextUtils.split("<@pagebreak/>");

	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.out.println("Aufruf: PreProcess <Dateiname(n)>");
			return;
		}

		List<File> inputs = FileAccess.checkExists(args);
		// keine Dateien gefunden?
		if (inputs.isEmpty()) {
			return;
		}

		// CSV-Datei mit Rechtschreib-Fehlern einlesen
		File basedir = FileAccess.basedir(inputs.get(0));
		Map<String, String> map = FileAccess.readRechtschreibungCSV(basedir);
		// Wörterbuch einlesen
		Set<String> dict = FileAccess.readDict(basedir, "german.dic");

		// alle Dateien verarbeiten
		File logfile = new File(basedir, "changes.log");
		try (PrintWriter log = new PrintWriter(logfile)) {
			for (File input : inputs) {
				preProcess(input, log, map, dict);
			}
		}
	}

	private static void preProcess(File input, PrintWriter log, Map<String, String> map, Set<String> dict)
			throws IOException, FileNotFoundException {
		long start = System.currentTimeMillis();
		System.out.println("Verarbeite Datei " + input.getPath());

		// Datei umbenennen
		File backup = FileAccess.roll(input);
		try (Reader in = new FileReader(backup);
				Writer out = new FileWriter(input);
				) {
			int count = new PreProcess().preProcess(in, out, log, map, dict);

			System.out.printf("Anzahl ersetzter Wörter: %,d, Zeit: %,dms%n",
					count, (System.currentTimeMillis() - start));
		}
	}

	private List<String> nextLine(BufferedReader reader) throws IOException {
		String line = reader.readLine();
		List<String> tokens;
		if (line == null) {
			tokens = EOF;
		} else {
			// Satzzeichen ersetzen
			line = TextUtils.satzzeichenErsetzen(line);
			// Punkte und Anführungszeichen in Wörtern entfernen
			line = line.replaceAll("(\\p{IsAlphabetic})[.»«](\\p{javaLowerCase})", "$1$2");
			// Zeile in Token zerlegen
			tokens = TextUtils.split(line);
		}

		return tokens;
	}

	public int preProcess(Reader in, Writer out, PrintWriter log, Map<String, String> map, Set<String> dict) throws IOException {
		// klein geschriebene Wörter auch in Groß-Schreibweise hinzufügen
		Set<String> ciDict = TextUtils.addUpperCase(dict);
		BufferedReader reader = new BufferedReader(in);
		PrintWriter writer = new PrintWriter(out);
		List<String> line = nextLine(reader);
		List<String> nextLine;
		int count = 0;
		do {
			nextLine = nextLine(reader);
			// ggf. Pagebreak nach unten verschieben
			if (line.equals(PAGEBREAK) && nextLine.isEmpty() && nextLine != EOF) {
				line = nextLine;
				nextLine = PAGEBREAK;
			}
			// 7er etc. ersetzen
			count += replaceSeven(line);
			// Brüche ersetzen
			count += replaceFraction(line);

			for (int i = 0; i < line.size(); i++) {
				String token = line.get(i);
				String replacement;
				// Worttrennung am Zeilenende?
				if (i == line.size() - 1 && TextUtils.endsWithDash(token)
						&& ! nextLine.isEmpty() && Character.isAlphabetic(nextLine.get(0).charAt(0))) {
					// Wörter zusammenfügen
					token += nextLine.remove(0);
					// Satzzeichen in die aktuelle Zeile übernehmen
					while (! nextLine.isEmpty() && TextUtils.isSatzzeichen(nextLine.get(0))) {
						line.add(nextLine.remove(0));
					}
					// Leerzeichen am Zeilenanfang entfernen.
					while (! nextLine.isEmpty() && " ".equals(nextLine.get(0))) {
						nextLine.remove(0);
					}
					// ist die Folge-Zeile jetzt leer?
					if (nextLine.isEmpty()) {
						// Zeile überspringen
						nextLine = nextLine(reader);
					}
				}

				// Anführungszeichen richtig herum drehen (»Wort« statt «Wort»)
				if ("»".equals(token) && i > 0
						&& (TextUtils.isWord(line.get(i - 1)) || TextUtils.isSatzzeichen(line.get(i - 1)))) {
					token = "«";
				} else if ("«".equals(token) && i < line.size() - 1 && TextUtils.isWord(line.get(i + 1))) {
					token = "»";
				}

				replacement = process(token, map, ciDict); map.get(token);
				if (replacement.equals(token)) {
					writer.print(token);
				} else {
					count++;
					writer.print(replacement);
					// einfache Entfernungen von Bindestrichen nicht protokollieren
					if (! replacement.equals(TextUtils.removeDashes(token))) {
						log.printf("%s\t%s%n", token, replacement);
					}
				}
			}

			writer.println();
			line = nextLine;
		}
		while (line != EOF);

		return count;
	}

	private String process(String token, Map<String, String> map, Set<String> ciDict) {
		String result = token;
		// ggf. Bindestriche entfernen, außer am Wortende
		String word = TextUtils.removeDashes(token);
		// Satzzeichen?
		if (" ".equals(token) || TextUtils.isSatzzeichen(token)) {
			// nichts zu tun
		}
		// Korrektur vorhanden?
		else if (map.containsKey(token)) {
			result = map.get(token);
		}
		// Wort ohne Bindestriche in Korrektur-Map?
		else if (map.containsKey(word)) {
			result = map.get(word);
		}
		// Wort ohne Bindestriche im Wörterbuch?
		else if (ciDict.contains(word)) {
			result = word;
		}
		// ist das Wort fälschlicherweise klein geschrieben?
		else if (Character.isLowerCase(word.charAt(0))
				&& ciDict.contains(TextUtils.toUpperCase(word))) {
			result = TextUtils.toUpperCase(word);
		} else {
			// überflüssige Buchstaben entfernen
			String candidate = removeSil(word, ciDict);

			// nicht gefunden? mit den Rechtschreib-Ersetzungen nochmal prüfen
			if (candidate.equals(word)) {
				candidate = removeSil(word, map.keySet());
				if (map.containsKey(candidate)) {
					candidate = map.get(candidate);
				}
			}

			// jetzt gefunden?
			if (! candidate.equals(word)) {
				result = candidate;
				// wenn das Original mit 'i' oder 'l' geendet hat, kommt wahrscheinlich ein Ausrufezeichen
				if ((word.endsWith("i") || word.endsWith("l"))
						&& ! (candidate.endsWith("i") || candidate.endsWith("l"))) {
					result += "!";
				}
			} else {
				// gängige Vertauschungen durchführen
				candidate = replaceCharacters(word, ciDict);
				if (! candidate.equals(word)) {
					result = candidate;
				}
			}
		}

		return result;
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

	/** Ersetzt vertauschte s/f, v/r/o, etc. */
	public static String replaceCharacters(String input, Set<String> dict) {
		// an allen Positionen die Zeichen vertauschen und prüfen, ob sie im Wörterbuch enthalten sind
		List<String> candidates = new ArrayList<>();
		replaceCharacters(input, dict, candidates, input.length() - 1, 5);
		String result = bestMatch(input, candidates);

		return result;
	}

	// Map mit typischen Vertauschungen
	private static final TreeMap<String, List<String>> SIMILAR_CHARS;
	static {
		TreeMap<String, List<String>> sc = new TreeMap<>();
		try {
			Map<String, List<String>> similar = FileAccess.readConfig("similar_chars.cfg");
			for (String s : similar.get("all")) {
				addAll(sc, s.split("\\s"));
			}
			for (String s : similar.get("first")) {
				addFirst(sc, s.split("\\s"));
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		SIMILAR_CHARS = sc;
	}
	private static void addAll(Map<String, List<String>> sc, String... entries) {
		for (int i = 0; i < entries.length; i++) {
			List<String> replacement = sc.get(entries[i]);
			if (replacement == null) {
				replacement = new ArrayList<>();
				sc.put(entries[i], replacement);
			}
			for (int j = 0; j < entries.length; j++) {
				if (i != j) {
					replacement.add(entries[j]);
				}
			}
		}
	}
	private static void addFirst(Map<String, List<String>> sc, String... entries) {
		List<String> values = Arrays.asList(entries);
		values = values.subList(1, values.size());
		if (sc.containsKey(entries[0])) {
			sc.get(entries[0]).addAll(values);
		} else {
			sc.put(entries[0], values);
		}
	}
	private static void replaceCharacters(String input, Set<String> dict, Collection<String> result, int end, int threshold) {
		for (int i = end; i >= 0; i--) {
			// erste passende Stelle suchen
			String tail = input.substring(i);
			Map<String, List<String>> map = SIMILAR_CHARS.subMap(tail.substring(0, 1), true, tail, true);
			for (Entry<String, List<String>> entry : map.entrySet()) {
				String chars = entry.getKey();
				if (tail.startsWith(chars)) {
					// durch alle anderen Zeichen ersetzen
					for (String replacement : entry.getValue()) {
						String candidate = input.substring(0, i) + replacement + input.substring(i + chars.length());
						if (dict.contains(candidate)) {
							result.add(candidate);
						}
						// weitere Kandidaten erzeugen
						if (i > 0 && threshold > 1) {
							replaceCharacters(candidate, dict, result, i - 1, threshold - 1);
						}
					}
				}
			}
		}
	}

	/** Entfernt überflüssige 's', 'i' und 'l' aus Wörtern. */
	public static String removeSil(String input, Set<String> dict) {
		// an allen Positionen die Zeichen entfernen und prüfen, ob sie im Wörterbuch enthalten sind
		List<String> candidates = new ArrayList<>();
		removeSil(input, dict, candidates, input.length() - 1);
		String result = bestMatch(input, candidates);

		// 's' am Wortende ignorieren
		if (input.endsWith("s") && ! result.endsWith("s")) {
			result += "s";
		}

		return result;
	}

	private static void removeSil(String input, Set<String> dict, Collection<String> result, int end) {
		for (int i = end; i >= 0; i--) {
			char ch = input.charAt(i);
			if (ch == 's' || ch == 'i' || ch == 'l') {
				StringBuilder sb = new StringBuilder(input);
				String candidate = sb.deleteCharAt(i).toString();
				if (dict.contains(candidate)) {
					result.add(candidate);
				}
				removeSil(candidate, dict, result, i - 1);
			}
		}
	}

	private static String bestMatch(String input, List<String> candidates) {
		if (candidates.isEmpty()) {
			return input;
		}

		String result = candidates.get(0);
		if (candidates.size() > 1) {
			int distance = LevenshteinDistance.compare(input, result);
			for (int i = 1; i < candidates.size(); i++) {
				String candidate = candidates.get(i);
				int distance2 = LevenshteinDistance.compare(input, candidate);
				if (distance2 < distance) {
					result = candidate;
					distance = distance2;
				}
			}
		}

		return result;
	}
}
