package org.github.cradloff.scanutils;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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

		Map<String, String> replacements;
		File replacementsFile = FileAccess.find(params.getInputs().get(0), "replace_once.txt");
		if (replacementsFile == null) {
			replacements = new HashMap<>();
		} else {
			replacements = FileAccess.readCSV(replacementsFile);
		}

		// alle Dateien verarbeiten
		for (File input : params.getInputs()) {
			prepareText(input, replacements);
		}
	}

	private static void prepareText(File input, Map<String, String> replacements) throws IOException {
		long start = System.currentTimeMillis();
		System.out.println("Verarbeite Datei " + input.getPath());

		// Datei umbenennen
		File backup = FileAccess.roll(input);
		try (Reader in = new FileReader(backup);
				PrintWriter out = new PrintWriter(input);
				) {
			prepareText(in, out, replacements);

			System.out.printf("Zeit: %,dms%n", (System.currentTimeMillis() - start));
		}
	}

	static class TextReader implements AutoCloseable {
		private BufferedReader reader;
		private String nextLine;

		public TextReader(BufferedReader reader) throws IOException {
			this.reader = reader;
			this.nextLine = reader.readLine();
		}

		public String readLine() throws IOException {
			String line = nextLine;
			nextLine = reader.readLine();
			return line;
		}

		public String peek() {
			return nextLine;
		}

		@Override
		public void close() throws IOException {
			reader.close();
		}
	}

	static void prepareText(Reader in, PrintWriter out, Map<String, String> replacements) throws IOException {
		try (TextReader reader = new TextReader(new BufferedReader(in));) {
			String line;
			String previousLine = "";
			boolean hasEmptyLine = false;
			while ((line = reader.readLine()) != null) {
				String result = line;
				result = changeSatzzeichen(result);
				result = handleChapter(result);
				result = removeLitter(result);
				result = changeDash(result);
				result = TextUtils.satzzeichenErsetzen(result);
				result = changeQuotes(result);
				result = changeSpecial(result);
				result = replaceOnce(result, replacements);
				result = handleSubChapter(previousLine, result, reader.peek());
				result = escapeDigits(result);
				result = nonBreakingSpaces(result);
				boolean emptyLine = result.isBlank();

				if (! emptyLine || ! hasEmptyLine) {
					out.println(result);
				}

				hasEmptyLine = emptyLine;
				if (! emptyLine) {
					previousLine = result;
				}
			}
		}
	}

	static String changeSatzzeichen(String line) {
		String result = line;
		// Punkte und … durch … ersetzen
		result = result.replaceAll("…\\s?\\.", "…");
		result = result.replaceAll("\\.\\s?…", "…");
		// drei oder mehr Punkte/Kommas werden durch … ersetzt
		result = result.replaceAll("[.,…]\\s?[.,…](\\s?[.,…])+", "…");
		// doppelte Punkte durch … ersetzen
		result = result.replaceAll("\\.\\s?\\.", "…");
		result = result.replace("……", "…");
		// einzelne Punkte nach Leerzeichen ersetzen
		result = result.replace(" . ", " … ");
		if (result.startsWith(". ")) {
			result = result.replaceAll("^\\. ", "… ");
		}
		if (result.endsWith(" .")) {
			result = result.replaceAll(" \\.$", " …");
		}

		// zwischen Punkten und Wörtern Leerzeichen einfügen
		result = result.replaceAll("(\\w)…", "$1 …");
		result = result.replaceAll("…(\\w)", "… $1");
		// doppelte Kommas durch Quote ersetzen
		result = result.replace(",,", "\"");

		return result;
	}

	static String removeLitter(String line) {
		// so lange wiederholen, bis sich nichts mehr ändert
		boolean changed;
		String result = line;

		// Schmierzeichen an beliebigen Stellen entfernen
		result = result.replaceAll("[#_%]", "");

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

	private static final Pattern QUOTES = Pattern.compile("[\"®*„“”]+");
	static String changeQuotes(String line) {
		// Zeile in Token zerlegen
		List<String> tokens = TextUtils.split(line);
		for (int i = 0; i < tokens.size(); i++) {
			String token = tokens.get(i);
			Matcher matcher = QUOTES.matcher(token);
			if (matcher.find()) {
				String replacement;
				if (TextUtils.textBefore(tokens, i)
						// Mischung aus Satzzeichen und Quote, z.B.: ?!"
						|| TextUtils.isWord(token.substring(0, matcher.start()))) {
					replacement = "«";
				}
				// Quote am Zeilenende
				else if (i == tokens.size() - 1) {
					// ggf. vorangegangenes Leerzeichen entfernen
					if (i > 0 && TextUtils.isWhitespace(tokens.get(i - 1))
							// nicht bei ..."
							&& ! token.startsWith("…")) {
						tokens.set(i - 1, "");
					}
					replacement = "«";
				}
				// Sonderfall: Satzzeichen gefolgt von Leerzeichen und Quote (z.B.: hier! ”)
				else if (i >= 2
						&& TextUtils.isWhitespace(tokens.get(i - 1))
						&& (TextUtils.textBefore(tokens, i - 1)
								|| TextUtils.isSatzzeichen(tokens.get(i - 2)))
						&& (i == tokens.size() - 1
								|| TextUtils.isWhitespace(tokens.get(i + 1)) )) {
					replacement = "«";
				} else {
					replacement = "»";
				}
				tokens.set(i, matcher.replaceAll(replacement));
			}
		}

		return String.join("", tokens);
	}

	static String changeDash(String line) {
		// Trennzeichen am Zeilenende
		String result = line.replaceAll("[»=]$", "-");
		// am Wortende
		result = result.replaceAll("(\\w)=", "$1-");
		// Gleichheitszeichen im Text
		result = result.replaceAll("^-?=[-“]* ", "— ");
		result = result.replaceAll(" -?=[-“]* ", " — ");
		result = result.replaceAll(" -?=[-“]* ", " — ");
		result = result.replaceAll(" -?=[-“]*$", " —");

		return result;
	}

	private static Pattern HEADING_PATTERN = Pattern.compile("<h\\d>.*</h\\d>");
	public static String changeSpecial(String line) {
		// Pagebreaks und Überschriften ignorieren
		if (line.startsWith("<@pagebreak")
				|| HEADING_PATTERN.matcher(line).matches()) {
			return line;
		}

		String result = line;
		result = result.replace("\\", "s")

				.replace("s{<", "sch")
				.replace("s{", "sch")
				.replace("{<", "sch")
				.replace("{h", "sch")
				.replace("{", "sch")

				.replace("}", "st")
				.replace('ſ', 's')

				.replace('|', '!')
				.replace('[', '!')

				.replace("c<", "ch")
				.replace("<h", "ch")
				.replace("<", "ch")

				.replace("c>", "ck")
				.replace("d>", "ck")
				.replace(">k", "ck")
				.replace(">", "ck");

		return result;
	}

	public static String replaceOnce(String input, Map<String, String> replacements) {
		String result = input;
		for (Entry<String, String> entry : replacements.entrySet()) {
			try {
				result = result.replaceAll(entry.getKey(), entry.getValue());
			} catch (PatternSyntaxException e) {
				throw new RuntimeException("Error replacing pattern " + entry.getKey() + " with " + entry.getValue(), e);
			}
		}

		return result;
	}

	private static String handleChapter(String line) {
		return line.replaceAll("^[^\\p{Alnum}]*(\\d)[.,:]? [KAFNPRTL][aägp][povy][ijlft][tflji][eacor][lfjst]\\s*.*$", "<h2>$1. Kapitel.</h2>");
	}

	private static String handleSubChapter(String previousLine, String line, String nextLine) {
		if (previousLine.startsWith("<h2>")
				&& ! line.isBlank()
				// Folge-Zeile ist leer oder die Zeile ist kürzer als 40 Zeichen
				&& ("".equals(nextLine)
						|| line.length() < 40)) {
			return line.replaceAll("^(.*?)[.,]?$", "<h3>$1.</h3>");
		}
		return line;
	}

	private static String escapeDigits(String line) {
		// ersetze "1. April" durch "1\. April"
		return line.replaceFirst("^([0-9]+)\\.", "$1\\\\.");
	}

	private static String nonBreakingSpaces(String line) {
		String result = line;
		result = result.replace("G. m. b. H.", "G.&nbsp;m.&nbsp;b.&nbsp;H.");
		result = result.replaceAll("(\\d) (\\d{3})", "$1&nbsp;$2");

		return result;
	}

}
