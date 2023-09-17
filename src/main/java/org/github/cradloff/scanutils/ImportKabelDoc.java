package org.github.cradloff.scanutils;

import java.io.*;

/**
 * Bereitet einen Text von der Kabel-Webseite als Dokument (Word/Writer) erstmalig vor.
 * Es werden Anführungszeichen ersetzt und Referenzen eingefügt.
 * Kapitelüberschriften werden in h2/h3-Tags verpackt
 */
public class ImportKabelDoc {
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
			System.out.println("Aufruf: ImportKabelDoc <Datei(en)>");
			return;
		}

		PreProcess.Parameter params = PreProcess.Parameter.parse(args);
		// keine URL gefunden?
		if (params.getInputs().isEmpty()) {
			return;
		}

		// Dateien verarbeiten
		for (File input : params.getInputs()) {
			new ImportKabelDoc().prepareText(input);
		}
	}

	private void prepareText(File input) throws IOException {
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

	void prepareText(Reader in, PrintWriter out) throws IOException {
		LineReader reader = new LineReader(in);
		String prevLine = "n/a";
		while (reader.readLine()) {
			String line = String.join("", reader.current());
			String processed = processLine(line, prevLine, out);
			out.println();
			prevLine = processed;
		}
	}

	static String processLine(String line, String previousLine, PrintWriter out) {
		String result = line;
		result = changeQuotes(result);
		result = handleChapter(result);
		result = handleSubChapter(previousLine, result);
		result = escapeDigits(result);
		result = nonBreakingSpaces(result);

		if (! result.isBlank()) {
			out.println(result);
		}
		
		return result;
	}

	private static String handleChapter(String line) {
		return line.replaceAll("^(\\d+)[.] Kapitel[.]$", "<h2>$1. Kapitel.</h2>");
	}

	private static String handleSubChapter(String previousLine, String line) {
		if (previousLine.startsWith("<h2>")) {
			return "<h3>" + line + "</h3>";
		}
		return line;
	}

	static String changeQuotes(String line) {
		return line.replace('„', '»').replace('“', '«').replace('–', '—');
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

}
