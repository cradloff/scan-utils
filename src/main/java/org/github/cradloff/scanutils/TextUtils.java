package org.github.cradloff.scanutils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

public class TextUtils {

	public static String satzzeichenErsetzen(String line) {
		String result = line;
		// mehrfache Gedankenstriche zusammenfassen
		result = result.replaceAll("[-—=]{2,}", "—");

		// << und >> in « und » umwandeln
		result = result.replaceAll("<<", "«");
		result = result.replaceAll(">>", "»");

		// gerade Anführungszeichen durch typographische ersetzen
		result = result.replace('\'', '’');

		// mittleren Punkt durch normalen Punkt ersetzen
		result = result.replace('·', '.');

		return result;
	}

	enum State { WHITESPACE, WORD, OTHER }
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
			// Backslash, Apostrophe und Ziffern werden wie Buchstaben behandelt
			else if (Character.isJavaIdentifierPart(ch) || ch == '\\' || ch == '\'' || ch == '’') {
				newState = State.WORD;
			}
			// Bindestriche, außer vor Großbuchstaben, werden ebenfalls wie Buchstaben behandelt
			else if (isDash(ch)
					&& (i == len - 1 && state == State.WORD
					|| i < len - 1 && Character.isLowerCase(line.charAt(i + 1)))) {
				newState = State.WORD;
			} else {
				newState = State.OTHER;
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
	public static Set<String> addUpperCase(Set<String> dict) {
		Set<String> ciDict = new HashSet<>(dict);
		for (String s : dict) {
			if (! s.isEmpty() && Character.isLowerCase(s.charAt(0))) {
				String t = toUpperCase(s);
				ciDict.add(t);
			}
		}
		return ciDict;
	}

	public static String toUpperCase(String word) {
		return Character.toUpperCase(word.charAt(0)) + word.substring(1);
	}

	public static boolean isWord(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (! Character.isAlphabetic(s.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	/** Prüft, ob vor der angegebenen Position ein Wort kommt */
	public static boolean wordBefore(List<String> line, int i) {
		return i > 0 && (isWord(line.get(i - 1)));
	}

	/** Prüft, ob nach der angegebenen Position ein Wort kommt */
	public static boolean wordAfter(List<String> line, int i) {
		return i < line.size() - 1 && isWord(line.get(i + 1));
	}

	public static boolean isSatzzeichen(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (Character.isWhitespace(s.charAt(i))
					|| Character.isAlphabetic(s.charAt(i))) {
				return false;
			}
		}
		return true;
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

	private static final Pattern END_OF_TAG = Pattern.compile("['\"/;]*>.*");
	public static boolean endOfTag(String token) {
		return END_OF_TAG.matcher(token).matches() && ! token.startsWith(">>");
	}

	private static final Pattern START_OF_TAG = Pattern.compile("[^<]*<[/@,.]*");
	public static boolean startOfTag(String token) {
		return START_OF_TAG.matcher(token).matches() && ! token.endsWith("<<");
	}

	/** Erzeugt ein Wörterbuch, in dem alle Einträge rückwärts enthalten sind (also z.B. "riw" statt "wir") */
	public static SortedSet<String> inverse(Set<String> ciDict) {
		SortedSet<String> result = new TreeSet<>();
		for (String entry : ciDict) {
			result.add(reverse(entry));
		}

		return result;
	}

	public static String reverse(String word) {
		return new StringBuilder(word).reverse().toString();
	}

}
