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
				String result = line;
				// doppelte Kommas durch Quote ersetzen
				result = result.replace(",,", "\"");
				result = removeLitter(result);
				result = changeDash(result);
				result = changeQuotes(result);
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
			String s = result.replaceAll("^[|/,‚.;:_‘’©>' ]+", "");
			// folgende Zeichen nur entfernen, wenn sie von einem Leerzeichen gefolgt werden
			s = s.replaceAll("^[\\\\“”\",¿(){}a-zA-Z0-9] ", "");

			// Schmierzeichen hinten entfernen
			s = s.replaceAll("[|/\\\\_‘’© ]+$", "");
			s = s.replaceAll(" [,.;:'()a-zA-Z0-9]$", "");

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
			if (token.length() > 1) {
				String lastChar = token.substring(token.length() - 1, token.length());
				if (QUOTE_CHARS.contains(lastChar)) {
					tokens.set(i, token.substring(0, token.length() - 1) + "«");
				} else if (QUOTE_CHARS.contains(token.substring(0, 1))) {
					tokens.set(i, "»" + token.substring(1));
				}
			} else if (QUOTE_CHARS.contains(token)) {
				if (TextUtils.wordBefore(tokens, i)) {
					tokens.set(i, "«");
				} else {
					tokens.set(i, "»");
				}
			}
		}

		return String.join("", tokens);
	}

	static String changeDash(String line) {
		// Trennzeichen am Zeilenende
		String result = line.replaceAll("[»=]$", "-");
		// Gleichheitszeichen im Text
		result = result.replace(" = ", " — ");

		return result;
	}

	public static String changeSpecial(String line) {
		// Pagebreaks ignorieren
		if ("<@pagebreak/>".equals(line)) {
			return line;
		}

		String result = line;
		result = result.replace("\\", "s");

		result = result.replace("s{<", "sch");
		result = result.replace("s{", "sch");
		result = result.replace("{<", "sch");
		result = result.replace("{", "sch");

		result = result.replace("}", "st");

		result = result.replace("c<", "ch");
		result = result.replace("<h", "ch");
		result = result.replace("<", "ch");

		result = result.replace("c>", "ck");
		result = result.replace("d>", "ck");
		result = result.replace(">k", "ck");
		result = result.replace(">", "ck");

		return result;
	}

}
