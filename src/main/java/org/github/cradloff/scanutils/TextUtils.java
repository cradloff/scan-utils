package org.github.cradloff.scanutils;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;

public class TextUtils {

	public static String satzzeichenErsetzen(String line) {
		String result = line;
		// mehrfache Gedankenstriche zusammenfassen
		result = result.replaceAll("[-—=+]{2,}", "—");

		// << und >> in « und » umwandeln
		result = result.replaceAll("<<", "«");
		result = result.replaceAll(">>", "»");

		// gerade Anführungszeichen durch typographische ersetzen
		result = result.replace('\'', '’');

		// Apostroph unten durch Komma ersetzen
		result = result.replace('‚', ',');

		// mittleren Punkt durch normalen Punkt ersetzen
		result = result.replace('·', '.');

		// drei (oder mehr) Punkte durch … ersetzen
		result = result.replaceAll("[.,]{3,}", "…");
		result = result.replaceAll("\\.*[…]\\.*", "…");
		result = result.replace(".-.", "…");
		result = result.replace(",.", "…");

		return result;
	}

	enum State { WHITESPACE, WORD, TAG, PUNCTUATION }
	private static final String PUNCTUATION_CHARS = ".…,;:-—!?()[]{}»«„“”\"*_";
	public static List<String> split(CharSequence line) {
		StringBuilder sb = new StringBuilder();
		List<String> result = new ArrayList<>();
		State state = State.WHITESPACE;
		final int len = line.length();
		for (int i = 0; i < len; i++) {
			char ch = line.charAt(i);
			State newState;
			if (Character.isWhitespace(ch)) {
				newState = State.WHITESPACE;
			}
			// Bindestriche, außer vor Großbuchstaben, werden ebenfalls wie Buchstaben behandelt
			else if (isDash(ch)
					&& (i == len - 1 && state == State.WORD
					|| i < len - 1 && Character.isLowerCase(line.charAt(i + 1)))) {
				newState = State.WORD;
			} else if (PUNCTUATION_CHARS.indexOf(ch) >= 0) {
				newState = State.PUNCTUATION;
			} else if (ch == '<' || ch == '/' || ch == '@' || ch == '>') {
				newState = State.TAG;
			} else {
				newState = State.WORD;
			}
			if (state != newState && i > 0) {
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

	/** Fügt dem Wörterbuch alle klein geschriebenen Wörter auch in Groß-Schreibweise hinzu */
	public static Bag<String> addUpperCase(Bag<String> dict) {
		Bag<String> ciDict = new HashBag<>(dict);
		for (String s : dict) {
			if (! s.isEmpty() && Character.isLowerCase(s.charAt(0))) {
				String t = toUpperCase(s);
				if (! ciDict.contains(t)) {
					ciDict.add(t);
				}
			}
		}
		return ciDict;
	}

	public static String toUpperCase(String word) {
		char firstChar = word.charAt(0);
		String remainder = word.substring(1);
		// nach einem Apostroph gehts groß weiter (z.B. "O’Hara")
		if (remainder.startsWith("’") && remainder.length() > 1) {
			return Character.toUpperCase(firstChar) + "’" + toUpperCase(remainder.substring(1));
		}
		return Character.toUpperCase(firstChar) + remainder;
	}

	public static boolean isWord(String s) {
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			if (! Character.isAlphabetic(ch) && ch != '’') {
				return false;
			}
		}
		return ! s.isEmpty();
	}

	public static boolean isAlphaNumeric(String s) {
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			if (! Character.isAlphabetic(ch) && ! Character.isDigit(ch) && ch != '’') {
				return false;
			}
		}
		return ! s.isEmpty();
	}

	/** Prüft, ob vor der angegebenen Position ein Wort oder eine Zahl kommt */
	public static boolean textBefore(List<String> line, int i) {
		return i > 0 && (isAlphaNumeric(line.get(i - 1)));
	}

	/** Prüft, ob nach der angegebenen Position ein Wort oder eine Zahl kommt */
	public static boolean textAfter(List<String> line, int i) {
		return i < line.size() - 1 && isAlphaNumeric(line.get(i + 1));
	}

	public static boolean isSatzzeichen(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (Character.isWhitespace(s.charAt(i))
					|| Character.isAlphabetic(s.charAt(i))) {
				return false;
			}
		}
		return ! s.isEmpty();
	}

	/** Prüft, ob vor der angegebenen Position ein Wort kommt */
	public static boolean satzzeichenBefore(List<String> line, int i) {
		return i > 0 && (isSatzzeichen(line.get(i - 1)));
	}

	public static boolean isDash(char ch) {
		return ch == '-' || ch == '—' || ch == '=';
	}

	public static boolean isWhitespace(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (! Character.isWhitespace(s.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	public static boolean endsWithDash(String s) {
		return s.length() > 1
				&& isDash(s.charAt(s.length() - 1))
				&& s.charAt(s.length() - 2) != '\\';
	}

	public static String removeDashes(String word) {
		StringBuilder sb = new StringBuilder(word.length());
		char last = ' ';
		for (int i = 0; i < word.length(); i++) {
			char ch = word.charAt(i);
			// alle Bindestriche außer am Wortende und nach einem Backslash entfernen
			if (isDash(ch) && i < word.length() - 1 && last != '\\') {

			} else {
				sb.append(ch);
			}
			last = ch;
		}

		return sb.toString();
	}

	public static boolean endsWith(List<String> line, String... tokens) {
		return regionMatches(line, line.size() - tokens.length, tokens);
	}

	public static boolean startsWith(List<String> line, String... tokens) {
		return regionMatches(line, 0, tokens);
	}

	public static boolean regionMatches(List<String> line, int pos, String... tokens) {
		boolean equal = true;
		if (pos < 0 || tokens.length + pos > line.size()) {
			equal = false;
		} else {
			for (int i = 0; i < tokens.length; i++) {
				if (! tokens[i].equals(line.get(i + pos))) {
					equal = false;
				}
			}
		}

		return equal;
	}

	private static final Pattern END_OF_TAG = Pattern.compile("['\"/;]*>.*");
	public static boolean endOfTag(String token) {
		return END_OF_TAG.matcher(token).matches() && ! token.startsWith(">>");
	}

	private static final Pattern START_OF_TAG = Pattern.compile("[^<]*<[/@,.]*");
	public static boolean startOfTag(String token) {
		return START_OF_TAG.matcher(token).matches() && ! token.endsWith("<<");
	}

	/** Erzeugt ein Wörterbuch, in dem alle Einträge rückwärts enthalten sind (also z.B. "riw" statt "wir") */
	public static SortedSet<String> inverse(Bag<String> ciDict) {
		SortedSet<String> result = new TreeSet<>();
		for (String entry : ciDict) {
			result.add(reverse(entry));
		}

		return result;
	}

	public static String reverse(String word) {
		return new StringBuilder(word).reverse().toString();
	}

	/** Prüft, ob in der Zeile ab dem angegebenen Index die Pattern zutreffen */
	public static boolean matches(List<String> line, int i, Pattern ... patterns) {
		if (line.size() - i < patterns.length || i < 0) {
			return false;
		}

		for (int j = 0; j < patterns.length; j++) {
			if (! patterns[j].matcher(line.get(i + j)).matches()) {
				return false;
			}
		}

		return true;
	}

	/** Stimmt die Zeile an der aktuellen Position mit dem Pattern überein, wird es durch die Zeichenkette ersetzt
	 * @return */
	public static int replace(List<String> line, int pos, Pattern[] pattern, String replacement) {
		int count = 0;
		if (matches(line, pos, pattern)) {
			for (int i = 1; i < pattern.length; i++) {
				line.remove(pos);
			}
			line.set(pos, replacement);
			count++;
		}

		return count;
	}

}
