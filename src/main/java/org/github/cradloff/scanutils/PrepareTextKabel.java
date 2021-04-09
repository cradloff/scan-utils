package org.github.cradloff.scanutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.github.cradloff.scanutils.PreProcess.Parameter;

/**
 * Bereitet einen Text von der Kabel-Webseite erstmalig vor. Es werden Anführungszeichen ersetzt und Referenzen eingefügt.
 * Kapitelüberschriften werden in h2/h3-Tags verpackt
 */
public class PrepareTextKabel {
	/** Anzahl der bisher gefundenen Fußnoten */
	private int footnotes = 0;

	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.out.println("Aufruf: PrepareTextKabel <Dateiname(n)>");
			return;
		}

		Parameter params = Parameter.parse(args);
		// keine Dateien gefunden?
		if (params.getInputs().isEmpty()) {
			return;
		}

		// alle Dateien verarbeiten
		for (File input : params.getInputs()) {
			new PrepareTextKabel().prepareText(input);
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
		try (BufferedReader reader = new BufferedReader(in);) {
			String line;
			boolean hasEmptyLine = false;
			while ((line = reader.readLine()) != null) {
				String result = line;
				result = changeQuotes(result);
				result = changeChapter(result);
				result = replaceReferences(result);
				boolean emptyLine = result.isBlank();

				if (! emptyLine || ! hasEmptyLine) {
					out.println(result);
				}

				hasEmptyLine = emptyLine;
			}
		}
	}

	static String changeQuotes(String line) {
		return line.replace('„', '»').replace('“', '«');
	}

	private boolean wasChapter;
	private String changeChapter(String line) {
		String result = line;
		// handelt es sich um eine Kapitel-Überschrift?
		if (line.matches("\\d+\\. Kapitel\\.?")) {
			wasChapter = true;
			result = "<h2>" + line + "</h2>";
		} else {
			if (wasChapter && ! line.isBlank()) {
				result = "<h3>" + line + "</h3>";
			}
			wasChapter = false;
		}

		return result;
	}

	// Fußnoten haben die Form:    ↑ Text Text Text
	private static final Pattern PATTERN_FOOTNOTE = Pattern.compile("\\s*↑ (.*)");
	private String replaceReferences(String input) {
		String result = input;
		// Referenzen auf Anmerkungen haben die Form: Text[1]
		result = result.replaceAll("\\[(\\d)\\]", "<@refnote $1/>");
		Matcher matcher = PATTERN_FOOTNOTE.matcher(result);
		if (matcher.matches()) {
			footnotes++;
			result = matcher.replaceAll(String.format("<@footnote %d \"FILENAME\">$1</@footnote>", footnotes));
		}

		return result;
	}

}
