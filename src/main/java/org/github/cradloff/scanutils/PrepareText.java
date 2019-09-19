package org.github.cradloff.scanutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.List;

import org.github.cradloff.scanutils.PreProcess.Parameter;

/**
 * Bereitet einen Text erstmalig vor. Es werden zahlreiche Ersetzungen vorgenommen und Schmierzeichen entfernt.
 */
public class PrepareText {

	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.out.println("Aufruf: PrepareText <Dateiname(n)>");
			return;
		}

		Parameter params = Parameter.parse(args);
		// keine Dateien gefunden?
		if (params.getInputs().isEmpty()) {
			return;
		}

		// alle Dateien verarbeiten
		for (File input : params.getInputs()) {
			prepareText(input);
		}
	}

	private static void prepareText(File input) throws IOException {
		long start = System.currentTimeMillis();
		System.out.println("Verarbeite Datei " + input.getPath());

		// Datei umbenennen
		File backup = FileAccess.roll(input);
		try (Reader in = new FileReader(backup);
				PrintWriter out = new PrintWriter(input);
				) {
			prepareText(in, out);

			System.out.printf("Zeit: %,dms%n", (System.currentTimeMillis() - start));
		}
	}

	static void prepareText(Reader in, PrintWriter out) throws IOException {
		try (BufferedReader reader = new BufferedReader(in);) {
			String line;
			boolean hasEmptyLine = false;
			while ((line = reader.readLine()) != null) {
				String result = removeLitter(line);
				result = changeQuotes(result);
				result = changeDash(result);
				result = TextUtils.satzzeichenErsetzen(result);
				result = changeSpecial(result);
				boolean emptyLine = result.isBlank();

				if (! emptyLine || ! hasEmptyLine) {
					out.println(result);
				}

				hasEmptyLine = emptyLine;
			}
		}
	}

	static String removeLitter(String line) {
		// so lange wiederholen, bis sich nichts mehr ändert
		boolean changed;
		String result = line;
		do {
			// Schmierzeichen vorne entfernen
			String s = result.replaceAll("^[|/\\\\,.;:_‘’© ]+", "");
			// folgende Zeichen nur entfernen, wenn sie von einem Leerzeichen gefolgt werden
			s = s.replaceAll("^[ij] ", "");

			// Schmierzeichen hinten entfernen
			s = s.replaceAll("[|/\\\\_‘’© ]+$", "");
			s = s.replaceAll(" [ijy,.;:]$", "");

			changed = ! s.equals(result);
			result = s;
		} while (changed);

		return result;
	}

	private static final String QUOTE_CHARS = "\"®*„“”";
	static String changeQuotes(String line) {
		// Zeile in Token zerlegen
		List<String> tokens = TextUtils.split(line);
		for (int i = 0; i < tokens.size(); i++) {
			String token = tokens.get(i);
			CharSequence lastChar = token.substring(token.length() - 1, token.length());
			if (QUOTE_CHARS.contains(lastChar)
					&& TextUtils.wordBefore(tokens, i) && ! TextUtils.wordAfter(tokens, i)) {
				tokens.set(i, token.substring(0, token.length() - 1) + "«");
			} else if (QUOTE_CHARS.contains(token)
					&& TextUtils.wordAfter(tokens, i) && ! TextUtils.wordBefore(tokens, i)) {
				tokens.set(i, "»");
			}
		}

		return String.join("", tokens);
	}

	static String changeDash(String line) {
		return line.replaceAll("[»=]$", "-");
	}

	public static String changeSpecial(String line) {
		String result = line.replace("<", "ch");
		result = result.replace(">", "ck");
		result = result.replace("s{", "sch");
		result = result.replace("{", "sch");

		return result;
	}

}
