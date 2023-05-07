package org.github.cradloff.scanutils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
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
	static Future<Result> EMPTY_LINE = new Future<>() {

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
	private Bag<String> ciDict;
	/** Baum-Ansicht des Dictionary */
	private SortedSet<String> treeView;
	private SortedSet<String> invDict;
	private Bag<String> silben;
	public LineProcessor(Parameter params, List<String> line, Map<String, String> map, Bag<String> ciDict, SortedSet<String> invDict, SortedSet<String> treeView, Bag<String> silben) {
		this.params = params;
		this.line = line;
		this.map = map;
		this.ciDict = ciDict;
		this.invDict = invDict;
		this.treeView = treeView;
		this.silben = silben;
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
			// und auch nicht in HTML-Entities (&nbsp; etc.)
			if (token.startsWith("&")) {
				result.print(token);
				continue;
			}

			// Satzzeichen in Wörtern entfernen
			while (TextUtils.isWord(token) && i < line.size() - 1 && line.get(i + 1).matches("[.,»«\"]") && TextUtils.textAfter(line, i + 1)) {
				token += line.get(i + 2);
				PreProcess.remove(line, i + 1, 2);
			}

			// Satzzeichen ersetzen
			token = TextUtils.satzzeichenErsetzen(token);
			// ,, durch » ersetzen
			if (token.matches("[,.]{2}") && TextUtils.textAfter(line, i)) {
				token = "»";
			}

			// Anführungszeichen richtig herum drehen (»Wort« statt «Wort»)
			token = changeQuotes(token, i);

			if (token.isEmpty()) {
				continue;
			}

			// Ausrufezeichen durch 't' ersetzen
			String replacement = ausrufezeichenErsetzen(token, i);

			// Wörter ersetzen
			replacement = process(i, replacement);

			// durch Leerzeichen getrennte Wörter zusammenfassen
			if (TextUtils.isWord(token) && whitespaceAfter(line, i) && TextUtils.textAfter(line, i + 1)
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
		}

		return result;
	}

	private String ausrufezeichenErsetzen(String token, int index) {
		if (ciDict.contains(token) || index + 1 >= line.size()) {
			return token;
		}
		
		String nextToken = line.get(index + 1);
		// alle Wörter ermitteln, die mit dem Token beginnen
		SortedSet<String> subset = treeView.subSet(token, token + "z");
		for (String candidate : subset) {
			String remainder = candidate.substring(token.length());
			// besteht der Rest nur aus Buchstaben, die einem Ausrufezeichen ähneln?
			if (remainder.matches("[tli]+")) {
				// wird das Token von ebenso vielen Ausrufezeichen gefolgt?
				int length = remainder.length();
				if (nextToken.length() >= length
						&& nextToken.substring(0, length).matches("[!]*")) {
					// die Ausrufezeichen entfernen
					nextToken = nextToken.substring(length);
					line.set(index + 1, nextToken);
					return candidate;
				}
			}
		}
		
		return token;
	}

	String changeQuotes(String token, int pos) {
		String result = token;
		// » nach einem Wort oder Satzzeichen aber nicht vor einem Wort ("Hallo»", "Hallo» Peter")
		if (token.contains("»")
				&& (TextUtils.textBefore(line, pos) || TextUtils.satzzeichenBefore(line, pos) || pos > 0 && TextUtils.isWhitespace(line.get(pos - 1)))
				&& ! TextUtils.textAfter(line, pos)) {
			result = token.replace("»", "«");
		}
		// « vor einem Wort ("«Hallo")
		else if ("«".equals(token) && TextUtils.textAfter(line, pos) && ! TextUtils.textBefore(line, pos)) {
			result = "»";
		}
		// » am Zeilenende
		else if (token.contains("»") && pos == line.size() - 1) {
			result = token.replace("»", "«");
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
			// einfach das häufigste passende Wort nehmen
			String inv = TextUtils.reverse(word);
			SortedSet<String> subSet = invDict.subSet(inv, inv + "\uffff");
			int count = 0;
			for (String curr : subSet) {
				String s = TextUtils.reverse(curr);
				if (count < ciDict.getCount(s)) {
					result = s;
					count = ciDict.getCount(s);
				}
			}
		}
		// Zwei Großbuchstaben am Wortbeginn?
		else if (word.length() > 2
				&& Character.isUpperCase(word.charAt(0))
				&& Character.isUpperCase(word.charAt(1))) {
			// jeweils eines der Zeichen löschen und damit versuchen
			Set<String> candidates = new HashSet<>();
			addIfModified(index, word.charAt(0) + word.substring(2).toLowerCase(), candidates);
			addIfModified(index, word.charAt(1) + word.substring(2).toLowerCase(), candidates);
			// zusätzlich mit dem zweiten Buchstaben in klein
			addIfModified(index, word.charAt(0) + word.substring(1).toLowerCase(), candidates);
			String candidate = bestMatch(token, candidates);
			if (! candidate.equals(word)) {
				result = candidate;
			}
		} else if (word.length() > 1) {
			// gängige Vertauschungen durchführen
			int level = Math.min(params.getLevel(), word.length() - 1);
			String candidate = replaceCharacters(token, level);
			if (! candidate.equals(word)) {
				result = candidate;
			}
		}

		return result;
	}

	private void addIfModified(int index, String word, Set<String> candidates) {
		String candidate = process(index, word);
		if (! word.equals(candidate)) {
			candidates.add(candidate);
		}
	}

	/** Ersetzt vertauschte s/f, v/r/o, etc. */
	public String replaceCharacters(String input, int threshold) {
		// an allen Positionen die Zeichen vertauschen und prüfen, ob sie im Wörterbuch enthalten sind
		// der Anfangsbuchstabe wird sowohl in Groß- als auch in Kleinschreibweise gesucht
		String[] variants = caseVariants(input);
		Set<String> candidates = new LinkedHashSet<>();
		int newThreshold = replaceCharacters(variants[0], treeView, candidates, 0, threshold) - 1;
		replaceCharacters(variants[1], treeView, candidates, 0, newThreshold);
		// den Kandidaten mit den wenigsten Unterschieden zum Original heraussuchen
		String result = bestMatch(input, candidates);
		// war das übergebene Wort groß geschrieben, muss auch das Ergebnis groß sein
		if (Character.isUpperCase(input.charAt(0))
				&& ! Character.isUpperCase(result.charAt(0))) {
			return TextUtils.toUpperCase(result);
		}

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
			replacements.addAll(Arrays.asList("", "ö", "ä", "ü", "ß", "Ö", "Ä", "Ü", "ch", "ck", "ff", "ll", "ss", "tt", "tz"));
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

	private String bestMatch(String input, Set<String> candidates) {
		if (candidates.isEmpty()) {
			return input;
		}

		// es gewinnt der Kandidat, der ein besseres Verhältnis von der Häufigkeit zur
		// Anzahl der Abweichungen hat. Dabei geht die 10er Potenz der Häufigkeit mit in die
		// Berechnung ein, sodass ein Wort mit 100 Vorkommen zwei zusätzliche Abweichungen
		// im Vergleich zu einem Wort mit nur einem Vorkommen haben darf
		// Damit sehr kurze und häufige Wörter (der, die, und, ...) nicht längere Wörter
		// überlagern, wird das Ganze auf einen Unterschied je zwei Buchstaben der Wortlänge
		// begrenzt (bei "der", "die" etc. beispielsweise auf eins)
		Iterator<String> it = candidates.iterator();
		String result = it.next();
		if (candidates.size() > 1) {
			int distance = calculateDistance(input, result);
			while (it.hasNext()) {
				String candidate = it.next();
				int distance2 = calculateDistance(input, candidate);
				// bei Wörtern, die sich nur in der Groß-/Kleinschreibung unterscheiden,
				// wird das Wort mit der Original-Schreibweise bevorzugt, unabhängig von der Häufigkeit
				if (result.equalsIgnoreCase(candidate)) {
					if (candidate.regionMatches(0, input, 0, 1)) {
						result = candidate;
					}
				} else if (distance2 < distance) {
					result = candidate;
					distance = distance2;
				} else if (distance2 == distance) {
					// häufigere Wörter bevorzugen
					int frequency1 = ciDict.getCount(result);
					int frequency2 = ciDict.getCount(candidate);
					if (frequency2 > frequency1) {
						result = candidate;
					}
				}
			}
		}

		return result;
	}

	private int calculateDistance(String original, String candidate) {
		int distance = LevenshteinDistance.compare(original, candidate);
		int bonus = (int) Math.log10(ciDict.getCount(candidate));
		bonus = Math.min(candidate.length() / 3, bonus);

		return distance - bonus;
	}

	private static boolean whitespaceAfter(List<String> line, int i) {
		return i < line.size() - 1 && TextUtils.isWhitespace(line.get(i + 1));
	}
}