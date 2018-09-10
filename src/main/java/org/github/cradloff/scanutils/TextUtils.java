package org.github.cradloff.scanutils;

import java.util.ArrayList;
import java.util.List;

public class TextUtils {

	public static String satzzeichenErsetzen(String line) {
		String result = line;
		// mehrfache Gedankenstriche zusammenfassen
		result = result.replaceAll("[-—]{2,}", "—");

		// << und >> in « und » umwandeln
		result = result.replaceAll("<<", "«");
		result = result.replaceAll(">>", "»");
		// ,, durch » ersetzen
		result = result.replaceAll("[,.]{2}(\\p{Alnum})", "»$1");

		// gerade Anführungszeichen durch typographische ersetzen
		result = result.replace('\'', '’');

		// mittleren Punkt durch normalen Punkt ersetzen
		result = result.replace('·', '.');

		return result;
	}

	enum State { WHITESPACE, WORD, OTHER }
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
