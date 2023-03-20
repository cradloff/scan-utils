package org.github.cradloff.scanutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bereitet einen Text von der Kabel-Webseite erstmalig vor. Die Seite wird heruntergeladen und gespeichert.
 * Es werden Anführungszeichen ersetzt und Referenzen eingefügt.
 * Kapitelüberschriften werden in h2/h3-Tags verpackt
 */
public class ImportKabel {
	static class Parameter {
		private int level = 6;
		private String input;

		public static Parameter parse(String[] args) {
			Parameter param = new Parameter();
			for (String arg : args) {
				if (arg.startsWith("-")) {
					param.level = Integer.parseInt(arg.substring(1));
				} else {
					param.input = arg;
				}
			}

			return param;
		}

		public int getLevel() {
			return level;
		}

		public String getInput() {
			return input;
		}
	}

	private static final int BUFFER_SIZE = 1024;

	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.out.println("Aufruf: ImportKabel <URL>");
			return;
		}

		Parameter params = Parameter.parse(args);
		// keine URL gefunden?
		if (params.getInput() == null) {
			return;
		}

		// Dokument herunterladen
		File input = readDocument(params.getInput());

		// Datei verarbeiten
		if (input != null) {
			new ImportKabel().prepareText(input);
		}
	}

	private static File readDocument(String fileUrl) throws IOException {
		File output;
		URL url = new URL(fileUrl);
		URLConnection httpConn = url.openConnection();

		// always check HTTP response code first
		if (httpConn instanceof HttpURLConnection && ((HttpURLConnection) httpConn).getResponseCode() != HttpURLConnection.HTTP_OK) {
			output = null;
			System.out.println("No file to download. Server replied HTTP code: " + ((HttpURLConnection) httpConn).getResponseCode());
		} else {
			output = new File("text.md");

			try (
					// opens input stream from the HTTP connection
					InputStream inputStream = httpConn.getInputStream();
					// opens an output stream to save into file
					FileOutputStream outputStream = new FileOutputStream(output);) {

				int bytesRead = -1;
				byte[] buffer = new byte[BUFFER_SIZE];
				while ((bytesRead = inputStream.read(buffer)) != -1) {
					outputStream.write(buffer, 0, bytesRead);
				}
			}

			System.out.println("File downloaded");
		}
		if (httpConn instanceof HttpURLConnection) {
			((HttpURLConnection) httpConn).disconnect();
		}

		return output;
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
			// Text bis zum Beginn des eigentlichen Inhalts überlesen
			skipPreText(reader);

			while ((line = reader.readLine()) != null) {
				boolean processed =
						// eine Überschrift?
						processHeading(line, out)
						// oder eine Fußnote?
						|| processFootnote(line, out)
						// oder normale Text-Zeile?
						|| processLine(line, out);
				if (processed) {
					out.println();
				}
			}
		}
	}
	
	private static final Pattern HEADING = Pattern.compile("<(h[1-3])( class=\"[^\"]*\")?>(.*)</h[1-3]>");
	private boolean processHeading(String line, PrintWriter out) {
		Matcher matcher = HEADING.matcher(line);
		if (matcher.matches()) {
			String tag = matcher.group(1);
			String text = stripTags(matcher.group(3));
			out.printf("<%s>%s</%s>%n", tag, text, tag);
			
			return true;
		}
		
		return false;
	}
	
	private static final Pattern PATTERN_FOOTNOTE = Pattern.compile("<li class=\"rtejustify\"><a href=\"#R(\\d+)\" name=\"A\\d+\" id=\"A\\d+\">↑</a>(.*)</li>");
	private boolean processFootnote(String line, PrintWriter out) {
		Matcher matcher = PATTERN_FOOTNOTE.matcher(line);
		if (matcher.matches()) {
			String result = matcher.group(2).trim();
			result = changeQuotes(result);
			out.printf("<@footnote %s \"FILENAME\">%s</@footnote>%n", matcher.group(1), result);
			
			return true;
		}
		
		return false;
	}

	private static final Pattern PARAGRAPH = Pattern.compile("(<p class=\"([^\"]*)\">)?(.*?)(</p>)?");
	private boolean processLine(String line, PrintWriter out) {
		Matcher matcher = PARAGRAPH.matcher(line);
		if (matcher.matches()) {
			String clazz  = matcher.group(2);
			String result = matcher.group(3);
			result = changeQuotes(result);
			result = replaceReferences(result);
			result = replaceFormat(result, clazz);
			result = escapeDigits(result);
			result = nonBreakingSpaces(result);
			boolean emptyLine = result.isBlank();

			if (! emptyLine) {
				out.println(result);
			}
			
			return true;
		}
		
		return false;
	}

	static String stripTags(String text) {
		return text.replaceAll("<[^<>]*>", "");
	}

	static final String BEGIN_OF_TEXT = "<div class=\"content\">";
	private void skipPreText(BufferedReader reader) throws IOException {
		String line;
		while ((line = reader.readLine()) != null) {
			if (BEGIN_OF_TEXT.equals(line.trim())) {
				break;
			}
		}
	}

	static String changeQuotes(String line) {
		return line.replace('„', '»').replace('“', '«').replace('–', '—');
	}
	private static final Pattern PATTERN_REFERENCE = Pattern.compile("<sup><a href=\"#A\\d+\" name=\"R\\d+\" id=\"R\\d+\">\\[(\\d+)\\]</a></sup>");
	static String replaceReferences(String input) {
		String result = input;
		Matcher matcher = PATTERN_REFERENCE.matcher(result);
		result = matcher.replaceAll("<@refnote $1/>");

		return result;
	}

	static String replaceFormat(String line, String clazz) {
		String result = line;
		boolean paragraph = false;
		// Links entfernen
		result = result.replaceAll("<a href=\"[^\"]*\">([^<>]*)</a>", "$1");
		// keine class-Angabe
		if (clazz == null) {
			// kein Prefix
		}
		// wörtliche Rede
		else if ("rteindent2 rtejustify".equals(clazz)) {
			result = "> " + result;
		} else if ("rteindent3 rtejustify".equals(clazz)) {
			result = "> > " + result;
		} else if ("rteindent4 rtejustify".equals(clazz)) {
			result = "> > > " + result;
		} else if (clazz.contains("rtecenter")) {
			paragraph = true;
			result = "<p class=\"centered\">" + result + "</p>";
		} else if (clazz.endsWith("rteright")) {
			paragraph = true;
			result = "<p class=\"right\">" + result + "</p>";
		}
		// Emphasis
		if (paragraph) {
			result = result.replaceAll("<span style=\"letter-spacing:2px\">([^<>]*)</span>", "<em>$1</em>");
		} else {
			result = result.replaceAll("<span style=\"letter-spacing:2px\">([^<>]*)</span>", "*$1*");
		}

		return result;
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
