package org.github.cradloff.scanutils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.github.cradloff.scanutils.PreProcess.Parameter;

public class LineProcessor implements Callable<LineProcessor.Result> {
	static Future<Result> EMPTY_LINE = new Future<Result>() {

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return false;
		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public boolean isDone() {
			return false;
		}

		@Override
		public Result get() throws InterruptedException, ExecutionException {
			return Result.EMPTY_RESULT;
		}

		@Override
		public Result get(long timeout, TimeUnit unit)
				throws InterruptedException, ExecutionException, TimeoutException {
			return Result.EMPTY_RESULT;
		}
	};

	static class Result {
		static final Result EMPTY_RESULT = new Result();

		private int count;
		private StringBuilder line = new StringBuilder(80);
		private List<String> log = new ArrayList<>();

		public int count() {
			return count;
		}

		public StringBuilder line() {
			return line;
		}

		public void print(String token) {
			line.append(token);
		}

		public void changed() {
			count++;
		}

		public void log(String token, String replacement) {
			log.add(token + "\t" + replacement);
		}

		public List<String> log() {
			return log;
		}
	}

	private Parameter params;
	private List<String> line;
	private Map<String, String> map;
	private SortedSet<String> ciDict;
	private Set<String> silben;
	public LineProcessor(Parameter params, List<String> line, Map<String, String> map, SortedSet<String> ciDict, Set<String> silben) {
		this.params = params;
		this.line = line;
		this.map = map;
		this.ciDict = ciDict;
		this.silben = silben;
	}

	@Override
	public Result call() {
		LineProcessor.Result result = new Result();
		boolean tag = false;
		for (int i = 0; i < line.size(); i++) {
			String token = line.get(i);

			// in Tags keine Ersetzungen durchführen
			if (TextUtils.startOfTag(token)) {
				tag = true;
			} else if (TextUtils.endOfTag(token)) {
				tag = false;
			} else if (tag) {
				result.print(token);
				continue;
			}

			// Satzzeichen in Wörtern entfernen
			while (TextUtils.isWord(token) && i < line.size() - 1 && line.get(i + 1).matches("[.,»«\"]") && wordAfter(line, i + 1)) {
				token += line.get(i + 2);
				PreProcess.remove(line, i + 1, 2);
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
			String replacement = process(token);

			// durch Leerzeichen getrennte Wörter zusammenfassen
			if (TextUtils.isWord(token) && whitespaceAfter(line, i) && wordAfter(line, i + 1)
					&& replacement.equals(token) && ! ciDict.contains(replacement) && ! ciDict.contains(line.get(i + 2))) {
				String word = token + line.get(i + 2);
				replacement = process(word);
				// kein Erfolg?
				if (replacement.equals(word) && ! ciDict.contains(replacement)) {
					replacement = token;
				} else {
					// bei Erfolg die nachfolgenden Token löschen
					token += line.get(i + 1) + line.get(i + 2);
					PreProcess.remove(line, i + 1, 2);
				}
			}

			if (replacement.equals(token)) {
				// nichts gefunden?
				if (! ciDict.contains(token)) {
					// ggf. zusammengeschriebene Wörter wieder trennen
					// jedes Teil-Wort muss mindestens zwei Zeichen haben
					for (int j = 1; j < token.length() -1; j++) {
						String prefix = token.substring(0, j);
						String suffix = token.substring(j);
						if (ciDict.contains(prefix) && ciDict.contains(suffix)
								// keine einzelnen Silben abtrennen
								&& ! silben.contains(prefix) && ! silben.contains(suffix)) {
							result.changed();
							result.print(prefix);
							result.print(" ");
							replacement = suffix;
							break;
						}
					}
				}
				result.print(replacement);
			} else {
				result.changed();
				result.print(replacement);
				// einfache Entfernungen von Bindestrichen nicht protokollieren
				if (! replacement.equals(TextUtils.removeDashes(token))) {
					result.log(token, replacement);
				}
			}
		}

		return result;
	}

	private String process(String token) {
		String result = token;
		// ggf. Bindestriche entfernen, außer am Wortende
		String word = TextUtils.removeDashes(token);
		// im Wörterbuch vorhanden?
		if (ciDict.contains(token)) {
			// nichts zu tun
		}
		// Korrektur vorhanden?
		else if (map.containsKey(token)) {
			result = map.get(token);
		}
		// Satzzeichen?
		else if (" ".equals(token) || TextUtils.isSatzzeichen(token)) {
			// nichts zu tun
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

	/** Ersetzt vertauschte s/f, v/r/o, etc. */
	public static String replaceCharacters(String input, Set<String> dict, int threshold) {
		// an allen Positionen die Zeichen vertauschen und prüfen, ob sie im Wörterbuch enthalten sind
		// der Anfangsbuchstabe wird sowohl in Groß- als auch in Kleinschreibweise gesucht
		String[] variants = caseVariants(input);
		List<String> candidates = new ArrayList<>();
		threshold = replaceCharacters(variants[0], dict, candidates, input.length() - 1, threshold) - 1;
		replaceCharacters(variants[1], dict, candidates, input.length() - 1, threshold);
		// den Kandidaten mit den wenigsten Unterschieden zum Original heraussuchen
		String result = bestMatch(input, candidates);

		return result;
	}

	private static String[] caseVariants(String input) {
		String lower = input.toLowerCase();
		String upper = TextUtils.toUpperCase(lower);
		String variants[] = new String[2];
		// zuerst wird in der Original-Schreibweise gesucht
		if (Character.isLowerCase(input.charAt(0))) {
			variants[0] = lower;
			variants[1] = upper;
		} else {
			variants[0] = upper;
			variants[1] = lower;
		}

		return variants;
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
	private static int replaceCharacters(String input, Set<String> dict, Collection<String> result, int end, int threshold) {
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
							// wenn wir einen Kandidaten gefunden haben, macht es keinen Sinn,
							// nach Wörtern mit mehr Unterschieden zu suchen
							threshold = 1;
						}
						// weitere Kandidaten erzeugen
						if (i > 0 && threshold > 1) {
							threshold = replaceCharacters(candidate, dict, result, i - 1, threshold - 1);
						}
					}
				}
			}
		}

		return threshold + 1;
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

	private static boolean wordBefore(List<String> line, int i) {
		return i > 0 && (TextUtils.isWord(line.get(i - 1)));
	}

	private static boolean wordAfter(List<String> line, int i) {
		return i < line.size() - 1 && TextUtils.isWord(line.get(i + 1));
	}

	private static boolean whitespaceAfter(List<String> line, int i) {
		return i < line.size() - 1 && TextUtils.isWhitespace(line.get(i + 1));
	}
}