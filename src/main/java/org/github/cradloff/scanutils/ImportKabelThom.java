package org.github.cradloff.scanutils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.Charset;

/**
 * Bereitet einen Text von Thom als Dokument erstmalig vor.
 * Es werden Anführungszeichen ersetzt und Referenzen eingefügt.
 * Kapitelüberschriften werden in h2/h3-Tags verpackt.
 * Der Zeichensatz wird konvertiert und Zeilen umgebrochen.
 */
public class ImportKabelThom {
	static class Parameter {
		private int level = 6;
		private String input;

		public int getLevel() {
			return level;
		}

		public String getInput() {
			return input;
		}
	}

	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.out.println("Aufruf: ImportKabelThom <Datei(en)>");
			return;
		}

		PreProcess.Parameter params = PreProcess.Parameter.parse(args);
		// keine URL gefunden?
		if (params.getInputs().isEmpty()) {
			return;
		}

		// Dateien verarbeiten
		for (File input : params.getInputs()) {
			new ImportKabelThom().prepareText(input);
		}
	}

	private void prepareText(File input) throws IOException {
		long start = System.currentTimeMillis();
		System.out.println("Verarbeite Datei " + input.getPath());

		// Datei umbenennen
		File backup = FileAccess.roll(input);
		try (Reader in = new FileReader(backup, Charset.forName("windows-1252"));
				PrintWriter out = new PrintWriter(input);) {
			prepareText(in, out);

			System.out.printf("Zeit: %,dms%n", System.currentTimeMillis() - start);
		}
	}

	void prepareText(Reader in, PrintWriter out) throws IOException {
		LineReader reader = new LineReader(in);
		String prevLine = "n/a";
		while (reader.readLine()) {
			String line = String.join("", reader.current());
			// Absätze beginnen mit 5 Leerzeichen, wenn die Folgezeile(n) ohne Leerzeichen startet,
			// beide Zeilen zusammenfassen
			for (String nextLine = String.join("", reader.next());
					line.startsWith("     ") && ! nextLine.isBlank() && nextLine.charAt(0) != ' ';
					nextLine = String.join("", reader.next())) {
				reader.skip(1);
				line = line + ' ' + nextLine;
			}
			
			String processed = processLine(line, prevLine, out);
			if (! processed.isEmpty()) {
				out.println();
				prevLine = processed;
			}
		}
	}

	static String processLine(String line, String previousLine, PrintWriter out) {
		String result = line;
		result = changeQuotes(result);
		result = changeSpecial(result);
		result = handleChapter(result);
		result = handleSubChapter(previousLine, result);
		result = escapeDigits(result);
		result = nonBreakingSpaces(result);
		result = replaceUmlaut(result);

		if (! result.isBlank()) {
			// zentrierte Zeilen beginnen mit mehr als 5 Leerzeichen
			boolean centered = false;
			if (result.startsWith("       ")) {
				centered = true;
				out.print("<p class='centered'>");
			}
			// nach ca. 50 Zeichen umbrechen
			result = result.trim();
			int idx = result.indexOf(' ', 45);
			int start = 0;
			while (idx > 0) {
				out.println(result.substring(start, idx));
				start = idx + 1;
				idx = result.indexOf(' ', start + 50);
			}
			out.print(result.substring(start));
			if (centered) {
				out.println("</p>");
			} else {
				out.println();
			}
		}
		
		return result;
	}

	private static String handleChapter(String line) {
		return line.replaceAll("^\\s*(\\d+)[.]\\s*Kapitel[.]$", "<h2>$1. Kapitel.</h2>");
	}

	private static String handleSubChapter(String previousLine, String line) {
		if (previousLine.startsWith("<h2>") && ! line.isBlank()) {
			return "<h3>" + line.trim() + "</h3>";
		}
		return line;
	}

	static String changeQuotes(String line) {
		return line.replace('„', '»').replace('“', '«').replace('–', '—');
	}

	static String changeSpecial(String line) {
		return line
				.replace(". . .", "…")
				.replace(". .", "…")
				.replace("...", "…")
				.replace("..", "…")
				.replaceAll("([\\wßöäü])…", "$1 …")
				.replace("» …", "»…")
				.replace(" - ", " — ")
				.replace("\"", "’")
				.replace("'", "’")
				.replace("´", "’")
				.replace("---", "—")
				.replace("--", "—");
	}
	
	static String escapeDigits(String line) {
		// ersetze "1. April" durch "1\. April"
		return line.replaceFirst("^([0-9]+)\\.", "$1\\\\.");
	}

	static String nonBreakingSpaces(String line) {
		String result = line;
		result = result.replace("G. m. b. H.", "G.&nbsp;m.&nbsp;b.&nbsp;H.");
		result = result.replaceAll("(\\d) (\\d{3})", "$1&nbsp;$2");

		return result;
	}

	static String replaceUmlaut(String result) {
		return result.replace("Ue", "Ü")
				.replace("Ae", "Ä")
				.replace("Oe", "Ö");
	}

}
