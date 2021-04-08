package org.github.cradloff.scanutils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
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

import org.apache.commons.collections4.Bag;
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
	private SortedSet<String> invDict;
	private Set<String> silben;
	private Bag<String> occurences;
	public LineProcessor(Parameter params, List<String> line, Map<String, String> map, SortedSet<String> ciDict, SortedSet<String> invDict, Set<String> silben, Bag<String> occurences) {
		this.params = params;
		this.line = line;
		this.map = map;
		this.ciDict = ciDict;
		this.invDict = invDict;
		this.silben = silben;
		this.occurences = occurences;
	}

	@Override
	public Result call() {
		LineProcessor.Result result = new Result();
		boolean tag = false;
		for (int i = 0; i < line.size(); i++) {
			String token = line.get(i);

			// Leerzeichen ausgeben und mit dem nächsten Token weitermachen
			if (" ".equals(token)) {
				result.print(token);
				continue;
			}

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
			while (TextUtils.isWord(token) && i < line.size() - 1 && line.get(i + 1).matches("[.,»«\"]") && TextUtils.wordAfter(line, i + 1)) {
				token += line.get(i + 2);
				PreProcess.remove(line, i + 1, 2);
			}

			// Satzzeichen ersetzen
			token = TextUtils.satzzeichenErsetzen(token);
			// ,, durch » ersetzen
			if (token.matches("[,.]{2}") && TextUtils.wordAfter(line, i)) {
				token = "»";
			}

			// Anführungszeichen richtig herum drehen (»Wort« statt «Wort»)
			if (token.contains("»") && TextUtils.wordBefore(line, i) && ! TextUtils.wordAfter(line, i)) {
				token = token.replace("»", "«");
			} else if ("«".equals(token) && TextUtils.wordAfter(line, i) && ! TextUtils.wordBefore(line, i)) {
				token = "»";
			} else if (token.contains("»") && i == line.size() - 1) {
				token = token.replace("»", "«");
			}

			// Wörter ersetzen
			String replacement = process(i, token);

			// durch Leerzeichen getrennte Wörter zusammenfassen
			if (TextUtils.isWord(token) && whitespaceAfter(line, i) && TextUtils.wordAfter(line, i + 1)
					&& replacement.equals(token) && ! ciDict.contains(replacement) && ! ciDict.contains(line.get(i + 2))) {
				String word = token + line.get(i + 2);
				replacement = process(i, word);
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

			occurences.add(replacement);
		}

		return result;
	}

	private String process(int index, String token) {
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
		}
		// am Zeilenanfang prüfen ob es Wort gibt, das mit dem Token endet
		else if (index == 0 && word.length() > 2 && hasPrefix(invDict, TextUtils.reverse(word))) {
			// einfach das kürzeste passende Wort nehmen
			String inv = TextUtils.reverse(word);
			SortedSet<String> subSet = invDict.subSet(inv, inv + "\uffff");
			String shortest = subSet.first();
			for (String curr : subSet) {
				if (curr.length() < shortest.length()) {
					shortest = curr;
				}
			}
			result = TextUtils.reverse(shortest);
		} else if (word.length() > 1) {
			// gängige Vertauschungen durchführen
			int level = Math.min(params.getLevel(), word.length() - 1);
			String candidate = replaceCharacters(token, ciDict, level);
			if (! candidate.equals(word)) {
				result = candidate;
			}
			// Zwei Großbuchstaben am Wortbeginn?
			else if (word.length() > 2
					&& Character.isUpperCase(word.charAt(0))
					&& Character.isUpperCase(word.charAt(1))) {
				// jeweils eines der Zeichen löschen und damit versuchen
				List<String> candidates = new ArrayList<>();
				candidates.add(process(index, word.substring(1)));
				candidates.add(process(index, word.charAt(0) + word.substring(2)));
				candidate = bestMatch(token, candidates);
				if (! candidate.equals(word)) {
					result = candidate;
				}
			}
		}

		return result;
	}

	/** Ersetzt vertauschte s/f, v/r/o, etc. */
	public String replaceCharacters(String input, SortedSet<String> dict, int threshold) {
		// an allen Positionen die Zeichen vertauschen und prüfen, ob sie im Wörterbuch enthalten sind
		// der Anfangsbuchstabe wird sowohl in Groß- als auch in Kleinschreibweise gesucht
		String[] variants = caseVariants(input);
		List<String> candidates = new ArrayList<>();
		int newThreshold = replaceCharacters(variants[0], dict, candidates, 0, threshold) - 1;
		replaceCharacters(variants[1], dict, candidates, 0, newThreshold);
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
	// allgemeine Ersetzungen für unbekannte Zeichen
	private static final List<String> REPLACEMENTS;
	// Map mit bekannten Zeichen
	private static final Set<String> KNOWN_CHARS;
	static {
		try {
			// allgemeine Ersetzungen für Bindestriche und unbekannte Zeichen
			List<String> replacements = new ArrayList<>();
			replacements.addAll(Arrays.asList("", "ö", "ä", "ü", "ß", "Oe", "Ae", "Ue", "ch", "ck", "ff", "ll", "ss", "tt", "tz"));
			for (char ch = 'a'; ch <= 'z'; ch++) {
				replacements.add(Character.toString(ch));
				replacements.add(Character.toString(ch).toUpperCase());
			}
			REPLACEMENTS = replacements;

			TreeMap<String, List<String>> sc = new TreeMap<>();
			Map<String, List<String>> similar = FileAccess.readConfig("similar_chars.cfg");
			for (String s : similar.get("all")) {
				addAll(sc, s.split("\\s"));
			}
			for (String s : similar.get("first")) {
				addFirst(sc, s.split("\\s"));
			}
			// ein Bindestrich kann jeder beliebige Buchstabe oder auch nichts sein
			sc.put("-", replacements);
			SIMILAR_CHARS = sc;

			// Liste mit den bereits bekannten Zeichen aufbauen
			Set<String> knownChars = new HashSet<>();
			" .,;:…-—’()!?*_»«<>\\\\/=@öäüÖÄÜß0123456789".chars().forEach(i -> knownChars.add(String.valueOf((char) i)));
			for (char c = 'a'; c <= 'z'; c++) {
				knownChars.add(String.valueOf(c));
				knownChars.add(String.valueOf(c).toUpperCase());
			}
			similar.values()
				.stream()
				.forEach(list -> list.stream()
						.forEach(line -> Arrays.asList(line.split("\t")).stream()
							.filter(s -> s.length() == 1)
							.forEach(s -> knownChars.add(s))));
			KNOWN_CHARS = knownChars;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
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
	private static int replaceCharacters(String input, SortedSet<String> dict, Collection<String> result, int start, int threshold) {
		// sind wir schon am Ende angelangt?
		if (start == input.length()) {
			return threshold;
		}

		int newThreshold = threshold;
		// zuerst mit dem unveränderten Zeichen weitersuchen
		String head = input.substring(0, start);
		// mit dem Teil des Dictionary weiterarbeiten, dass mit dem Prefix anfängt
		SortedSet<String> subSet = dict.subSet(head, head + "\uffff");
		if (hasPrefix(subSet, head)) {
			newThreshold = replaceCharacters(input, subSet, result, start + 1, newThreshold);

			// dann mit allen möglichen Ersetzungen
			String tail = input.substring(start);
			String currCh = tail.substring(0, 1);
			// unbekanntes Zeichen?
			if (! KNOWN_CHARS.contains(currCh)) {
				// durch allgemeine Zeichen ersetzen
				String suffix = tail.substring(1);
				newThreshold = replaceCharacters(result, start, newThreshold, head, subSet, REPLACEMENTS, suffix);
			} else {
				Map<String, List<String>> map = SIMILAR_CHARS.subMap(currCh, true, tail, true);
				for (Entry<String, List<String>> entry : map.entrySet()) {
					String chars = entry.getKey();
					if (tail.startsWith(chars)) {
						String suffix = tail.substring(chars.length());
						// durch alle anderen Zeichen ersetzen
						newThreshold = replaceCharacters(result, start, newThreshold, head, subSet, entry.getValue(), suffix);
					}
				}
			}
		}

		return newThreshold;
	}

	private static int replaceCharacters(Collection<String> result, int start, int threshold, String head,
			SortedSet<String> subSet, List<String> replacements, String suffix) {
		int newThreshold = threshold;
		for (String replacement : replacements) {
			String candidate = head + replacement + suffix;
			if (subSet.contains(candidate)) {
				result.add(candidate);
				// wenn wir einen Kandidaten gefunden haben, macht es keinen Sinn,
				// weiter nach Wörtern mit mehr Unterschieden zu suchen
				newThreshold = 1;
			}

			// weitere Kandidaten erzeugen
			if (hasPrefix(subSet, head + replacement) && newThreshold > 1) {
				int x = replaceCharacters(candidate, subSet, result, start + replacement.length(), newThreshold - 1);
				newThreshold = Math.min(x + 1, newThreshold);
			}
		}
		return newThreshold;
	}

	/** gibt es im Wörterbuch einen Eintrag, der mit dem angegebenen Eintrag beginnt? */
	private static boolean hasPrefix(SortedSet<String> dict, String prefix) {
		SortedSet<String> subSet = dict.tailSet(prefix);
		return ! subSet.isEmpty() && subSet.first().startsWith(prefix);
	}

	private String bestMatch(String input, List<String> candidates) {
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
				} else if (distance2 == distance) {
					// häufigere Wörter bevorzugen
					int frequency1 = occurences.getCount(result);
					int frequency2 = occurences.getCount(candidate);
					if (frequency2 > frequency1) {
						result = candidate;
					}
				}
			}
		}

		return result;
	}

	private static boolean whitespaceAfter(List<String> line, int i) {
		return i < line.size() - 1 && TextUtils.isWhitespace(line.get(i + 1));
	}
}