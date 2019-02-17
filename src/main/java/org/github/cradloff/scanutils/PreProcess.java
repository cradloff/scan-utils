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
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Ersetzt falsch geschriebene Worte durch die korrekte Version.
 * Die Klasse benötigt ein Wörterbuch (german.dic) und eine Datei mit Rechtschreib-Korrekturen (rechtschreibung.csv)
 * im Verzeichnis der Eingabedatei oder darüber.
 * Das Wörterbuch kann z.B. von hier bezogen werden: https://sourceforge.net/projects/germandict/
 * Die Rechtschreib-Korrekturen müssen jeweils den Suchbegriff und die Ersetzung in einer Zeile, durch Tab getrennt, enthalten.
 */
public class PreProcess {
	static class Parameter {
		private int level = 4;
		private List<File> inputs = new ArrayList<>();

		public static Parameter parse(String[] args) {
			Parameter param = new Parameter();
			for (String arg : args) {
				if (arg.startsWith("-")) {
					param.level = Integer.parseInt(arg.substring(1));
				} else {
					File input = FileAccess.checkExists(arg);
					if (input != null) {
						param.inputs.add(input);
					}
				}
			}

			return param;
		}

		public int getLevel() {
			return level;
		}

		public List<File> getInputs() {
			return inputs;
		}
	}

	/** leere Liste als Markierung für das Dateiende */
	private static final List<String> EOF = new ArrayList<>();
	/** Zeile mit einem Pagebreak */
	private static final List<String> PAGEBREAK = TextUtils.split("<@pagebreak/>");
	private Parameter params;

	public PreProcess(Parameter params) {
		this.params = params;
	}

	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.out.println("Aufruf: PreProcess [Optionen] <Dateiname(n)>");
			return;
		}

		Parameter params = Parameter.parse(args);
		// keine Dateien gefunden?
		if (params.getInputs().isEmpty()) {
			return;
		}

		// CSV-Datei mit Rechtschreib-Fehlern einlesen
		File basedir = FileAccess.basedir(params.getInputs().get(0));
		Map<String, String> map = FileAccess.readRechtschreibungCSV(basedir);
		// Wörterbuch einlesen
		Set<String> dict = FileAccess.readDict(basedir, "german.dic");
		Set<String> silben = FileAccess.readDict(basedir, "silben.dic");

		// alle Dateien verarbeiten
		File logfile = new File(basedir, "changes.log");
		try (PrintWriter log = new PrintWriter(logfile)) {
			for (File input : params.getInputs()) {
				preProcess(input, params, log, map, dict, silben);
			}
		}
	}

	private static void preProcess(File input, Parameter params, PrintWriter log, Map<String, String> map, Set<String> dict, Set<String> silben)
			throws IOException, FileNotFoundException {
		long start = System.currentTimeMillis();
		System.out.println("Verarbeite Datei " + input.getPath());

		// Datei umbenennen
		File backup = FileAccess.roll(input);
		try (Reader in = new FileReader(backup);
				Writer out = new FileWriter(input);
				) {
			int count = new PreProcess(params).preProcess(in, out, log, map, dict, silben);

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
			// Zeile in Token zerlegen
			tokens = TextUtils.split(line);
		}

		return tokens;
	}

	public int preProcess(Reader in, Writer out, PrintWriter log, Map<String, String> map, Set<String> dict, Set<String> silben) throws IOException {
		// klein geschriebene Wörter auch in Groß-Schreibweise hinzufügen
		SortedSet<String> ciDict = new TreeSet<>(TextUtils.addUpperCase(dict));
		BufferedReader reader = new BufferedReader(in);
		PrintWriter writer = new PrintWriter(out);
		List<String> line = nextLine(reader);
		List<String> prevLine = Arrays.asList("BOF");
		List<String> nextLine;
		int count = 0;
		do {
			nextLine = nextLine(reader);

			// ggf. Pagebreak nach unten verschieben
			if (line.equals(PAGEBREAK) && ! prevLine.isEmpty() && nextLine.isEmpty() && nextLine != EOF) {
				line = nextLine;
				nextLine = PAGEBREAK;
			}

			// Leerzeilen überspringen
			if (line.isEmpty()) {
				writer.println();
				prevLine = line;
				line = nextLine;
				continue;
			}

			// 7er etc. ersetzen
			count += replaceSeven(line);
			// Brüche ersetzen
			count += replaceFraction(line);
			// Worttrennung am Zeilenende zusammenfassen
			if (mergeLinebreak(line, nextLine)) {
				// ist die Folge-Zeile jetzt leer?
				if (nextLine.isEmpty()) {
					// Zeile überspringen
					nextLine = nextLine(reader);
				}
			}

			boolean tag = false;
			for (int i = 0; i < line.size(); i++) {
				String token = line.get(i);

				// in Tags keine Ersetzungen durchführen
				if (startOfTag(token)) {
					tag = true;
				} else if (endOfTag(token)) {
					tag = false;
				} else if (tag) {
					writer.print(token);
					continue;
				}

				// Satzzeichen in Wörtern entfernen
				while (TextUtils.isWord(token) && i < line.size() - 1 && line.get(i + 1).matches("[.,»«\"]") && wordAfter(line, i + 1)) {
					token += line.get(i + 2);
					remove(line, i + 1, 2);
				}

				// Satzzeichen ersetzen
				token = TextUtils.satzzeichenErsetzen(token);
				// ,, durch » ersetzen
				if (token.matches("[,.]{2}") && wordAfter(line, i)) {
					token = "»";
				}

				// Anführungszeichen richtig herum drehen (»Wort« statt «Wort»)
				if (token.endsWith("»") && wordBefore(line, i) && ! wordAfter(line, i)) {
					token = token.replace("»", "«");
				} else if ("«".equals(token) && wordAfter(line, i) && ! wordBefore(line, i)) {
					token = "»";
				}

				// Wörter ersetzen
				String replacement = process(token, map, ciDict, silben);

				// durch Leerzeichen getrennte Wörter zusammenfassen
				if (TextUtils.isWord(token) && whitespaceAfter(line, i) && wordAfter(line, i + 1)
						&& replacement.equals(token) && ! ciDict.contains(replacement) && ! ciDict.contains(line.get(i + 2))) {
					String word = token + line.get(i + 2);
					replacement = process(word, map, ciDict, silben);
					// kein Erfolg?
					if (replacement.equals(word) && ! ciDict.contains(replacement)) {
						replacement = token;
					} else {
						// bei Erfolg die nachfolgenden Token löschen
						token += line.get(i + 1) + line.get(i + 2);
						remove(line, i + 1, 2);
					}
				}

				if (replacement.equals(token)) {
					// nichts gefunden?
					if (! ciDict.contains(token)) {
						// ggf. zusammengeschriebene Wörter wieder trennen
						// jedes Teil-Wort muss mindestene zwei Zeichen haben
						for (int j = 1; j < token.length() -1; j++) {
							String prefix = token.substring(0, j);
							String suffix = token.substring(j);
							if (ciDict.contains(prefix) && ciDict.contains(suffix)) {
								count++;
								writer.print(prefix);
								writer.print(" ");
								replacement = suffix;
								break;
							}
						}
					}
					writer.print(replacement);
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
			prevLine = line;
			line = nextLine;
		}
		while (line != EOF);

		return count;
	}

	private boolean mergeLinebreak(List<String> line, List<String> nextLine) {
		boolean merged = false;
		// Worttrennung am Zeilenende?
		String token = line.get(line.size() - 1);
		if (TextUtils.endsWithDash(token) && Character.isAlphabetic(token.codePointAt(0))
				&& ! nextLine.isEmpty() && Character.isAlphabetic(nextLine.get(0).charAt(0))) {
			// Wörter zusammenfügen
			token += nextLine.remove(0);
			line.set(line.size() - 1, token);
			// Satzzeichen in die aktuelle Zeile übernehmen
			while (! nextLine.isEmpty() && TextUtils.isSatzzeichen(nextLine.get(0))) {
				line.add(nextLine.remove(0));
			}
			// Leerzeichen am Zeilenanfang entfernen.
			while (! nextLine.isEmpty() && " ".equals(nextLine.get(0))) {
				nextLine.remove(0);
			}
			merged = true;
		}

		return merged;
	}

	private String process(String token, Map<String, String> map, Set<String> ciDict, Set<String> silben) {
		String result = token;
		// ggf. Bindestriche entfernen, außer am Wortende
		String word = TextUtils.removeDashes(token);
		// Satzzeichen oder im Wörterbuch vorhanden?
		if (" ".equals(token) || TextUtils.isSatzzeichen(token) || ciDict.contains(token)) {
			// nichts zu tun
		}
		// Korrektur vorhanden?
		else if (map.containsKey(token)) {
			result = map.get(token);
		}
		// keine Ersetzung von Silben
		else if (silben.contains(word)) {
			result = word;
		}
		// Wort ohne Bindestriche in Korrektur-Map?
		else if (map.containsKey(word)) {
			result = map.get(word);
		}
		// Wort ohne Bindestriche im Wörterbuch?
		else if (ciDict.contains(word)) {
			result = word;
		}
		// endet das Wort auf i, l, t, 1 und ist der Rest im Wörterbuch?
		else if ((word.endsWith("i") || word.endsWith("l") || word.endsWith("t") || word.endsWith("1"))
				&& ciDict.contains(word.substring(0, word.length() - 1))) {
			// dann das letzte Zeichen durch ein Ausrufezeichen ersetzen
			result = word.substring(0, word.length() - 1) + "!";
		}
		// ist das Wort fälschlicherweise klein geschrieben?
		else if (Character.isLowerCase(word.charAt(0))
				&& ciDict.contains(TextUtils.toUpperCase(word))) {
			// nicht, wenn das Wort mit Bindestrich beginnt
			if (! token.startsWith("-")) {
				result = TextUtils.toUpperCase(word);
			}
		}
		// oder passt die Groß-/Kleinschreibung nicht (z.B. "eS")?
		else if (! ciDict.contains(word) && ciDict.contains(word.toLowerCase())) {
			result = word.toLowerCase();
		} else if (! ciDict.contains(word) && ciDict.contains(TextUtils.toUpperCase(word.toLowerCase()))) {
			result = TextUtils.toUpperCase(word.toLowerCase());
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
				candidate = replaceCharacters(word, ciDict, params.getLevel());
				if (! candidate.equals(word)) {
					result = candidate;
				}
			}
		}

		return result;
	}

	private static final Pattern SEVEN = Pattern.compile(".*\\D[72]$");
	private static final Pattern SEVEN_PLUS = Pattern.compile(".*\\D[72][ilt1]$");
	static int replaceSeven(List<String> line) {
		int count = 0;
		String nextWord = "";
		boolean tag = false;
		for (int i = line.size() - 1; i >= 0; i--) {
			// '7' am Wortende durch '?' ersetzen
			String word = line.get(i);
			if (endOfTag(word)) {
				tag = true;
			} else if (startOfTag(word)) {
				tag = false;
			} else if (tag) {
				continue;
			}

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
					&& (nextWord.equals("i") || nextWord.equals("l")
							|| nextWord.equals("t") || nextWord.equals("1"))) {
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
						remove(words, i, 2);
						count++;
					}
				}
			}
		}
		return count;
	}

	private static void remove(List<?> values, int index, int count) {
		for (int i = 0; i < count; i++) {
			values.remove(index);
		}
	}

	/** Ersetzt vertauschte s/f, v/r/o, etc. */
	public static String replaceCharacters(String input, Set<String> dict, int threshold) {
		// an allen Positionen die Zeichen vertauschen und prüfen, ob sie im Wörterbuch enthalten sind
		List<String> candidates = new ArrayList<>();
		replaceCharacters(input, dict, candidates, input.length() - 1, threshold);
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
			sc.put(entries[0], new ArrayList<>(values));
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

	/** Entfernt überflüssige 's', 'i', 'l' und 'x' aus Wörtern. */
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
			if (ch == 's' || ch == 'i' || ch == 'l' || ch == 'x') {
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

	private boolean wordBefore(List<String> line, int i) {
		return i > 0 && (TextUtils.isWord(line.get(i - 1)));
	}

	private boolean wordAfter(List<String> line, int i) {
		return i < line.size() - 1 && TextUtils.isWord(line.get(i + 1));
	}

	private boolean whitespaceAfter(List<String> line, int i) {
		return i < line.size() - 1 && TextUtils.isWhitespace(line.get(i + 1));
	}

	private static boolean endOfTag(String token) {
		return ">".equals(token) || "/>".equals(token);
	}

	private static boolean startOfTag(String token) {
		return "<".equals(token) || "</".equals(token) || "<@".equals(token) || "</@".equals(token);
	}
}
