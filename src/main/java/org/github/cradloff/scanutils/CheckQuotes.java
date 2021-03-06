package org.github.cradloff.scanutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Prüft die öffnenden und schließenden Quotes in einem Text.
 */
public class CheckQuotes {
	public static void main(String... args) throws IOException {
		long start = System.currentTimeMillis();
		if (args.length < 1) {
			System.out.println("Aufruf: CheckQuotes <Dateiname(n)>");

			return;
		}

		List<File> inputs = FileAccess.checkExists(args);
		// keine Dateien gefunden?
		if (inputs.isEmpty()) {
			return;
		}

		// Dateien prüfen
		int errors = 0;
		File basedir = FileAccess.basedir(inputs.get(0));
		File checkquote = new File(basedir, "checkquotes.log");
		System.out.printf("Schreibe fehlerhafte Anführungszeichen nach %s%n", checkquote);
		try (Writer fw = new FileWriter(checkquote);
				PrintWriter out = new PrintWriter(fw)) {
			for (File input : inputs) {
				errors += checkQuotes(input, out);
			}
		}

		System.out.printf("Anzahl fehlerhafter Anführungszeichen: %,d, Zeit: %,dms%n",
				errors, (System.currentTimeMillis() - start));
	}

	private static int checkQuotes(File input, PrintWriter out) throws IOException {
		out.println(input.getName());
		int errors = 0;

		try (Reader in = new FileReader(input);) {
			errors = checkQuotes(in, out);
		}
		out.println();

		return errors;
	}

	static int checkQuotes(Reader in, PrintWriter out) throws IOException {
		String line;
		int errors = 0;
		int lineNo = 0;
		List<List<String>> paragraph = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(in);) {
			while ((line = reader.readLine()) != null) {
				lineNo++;
				if (line.trim().isEmpty()) {
					errors += checkQuotes(paragraph, out, lineNo - paragraph.size());
					paragraph.clear();
				} else {
					paragraph.add(TextUtils.split(line));
				}
			}
		}

		errors += checkQuotes(paragraph, out, lineNo - paragraph.size());
		return errors;
	}

	private static int checkQuotes(List<List<String>> paragraph, PrintWriter out, int line) {
		int errors = 0;
		int quotes = 0;
		int i = 0;
		for (List<String> currLine : paragraph) {
			i++;
			for (String token : currLine) {
				// sowohl öffnendes als auch schließendes Anführungszeichen?
				if (hasStartQuote(token) && hasEndQuote(token)) {
					logError(out, line + i, line + i, 0);
					errors++;
				} else if (hasStartQuote(token)) {
					quotes++;
				} else if (hasEndQuote(token)) {
					quotes--;
				}
			}
		}

		if (quotes != 0) {
			logError(out, line, line + paragraph.size() - 1, quotes);
			errors += Math.abs(quotes);
		}

		return errors;
	}

	private static boolean hasEndQuote(String token) {
		return token.contains("«") || token.contains("“");
	}

	private static boolean hasStartQuote(String token) {
		return token.contains("»") || token.contains("„");
	}

	private static void logError(PrintWriter out, int lineStart, int lineEnd, int quotes) {
		out.printf("Fehler in Zeile %,d - %,d: %,d%n", lineStart, lineEnd, quotes);
	}
}
