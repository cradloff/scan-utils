package org.github.cradloff.scanutils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
	
	private Parameter params;
	PrintWriter out;
	private String filename;
	private Map<String, String> references = new HashMap<>();
	private int anzahlKapitel = 0;

	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.out.println("Aufruf: ImportKabel <URL>");
			return;
		}

		new ImportKabel().doImport(args);
	}

	private void doImport(String[] args) throws IOException {
		params = Parameter.parse(args);
		// keine URL gefunden?
		String url = params.getInput();
		if (url == null) {
			return;
		}

		// ggf. Dokument herunterladen
		File input;
		if (isUrl(url)) {
			input = readDocument(new URL(url), "text.md");
		} else {
			// sonst direkt Datei bearbeiten
			input = new File(url);
			if (! input.exists()) {
				System.out.println("Datei nicht gefunden: " + url);
				return;
			}
		}

		// Datei verarbeiten
		if (input != null) {
			prepareText(input);
		}
	}

	private boolean isUrl(String url) {
		return url.startsWith("http:") || url.startsWith("https:");
	}

	private static File readDocument(URL url, String filename) throws IOException {
		File output;
		URLConnection httpConn = url.openConnection();

		// always check HTTP response code first
		if (httpConn instanceof HttpURLConnection && ((HttpURLConnection) httpConn).getResponseCode() != HttpURLConnection.HTTP_OK) {
			output = null;
			System.out.println("No file to download. Server replied HTTP code: " + ((HttpURLConnection) httpConn).getResponseCode());
		} else {
			output = new File(filename);

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

			System.out.printf("Datei %s heruntergeladen%n", filename);
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
		try (Reader in = new FileReader(backup);) {
			prepareText(in);

			System.out.printf("Zeit: %,dms%n", (System.currentTimeMillis() - start));
		}
	}

	private static final String[] START_KAPITEL = TextUtils.split("<h1 class=\"rtecenter\"><").toArray(new String[0]);
	private static final Pattern KAPITEL = Pattern.compile("\\s*<h1 class=\"rtecenter\">.*<span style=\"font-size:28px\">(<strong>)?(.*)(</strong>)?</span>.*</h1>");
	private static final String[] START_COVER = TextUtils.split("<p class=\"rtecenter\"><img alt=").toArray(new String[0]);
	private static final String ANMERKUNGEN = "<p class=\"rtejustify\"><strong>Anmerkungen:</strong></p>";
	void prepareText(Reader in) throws IOException {
		LineReader reader = new LineReader(in);
		// Text bis zum Beginn des eigentlichen Inhalts überlesen
		skipPreText(reader);

		// kein Inhalt gefunden?
		if (! reader.hasNext()) {
			System.out.println("Kein Inhalt gefunden!");
			return;
		}
		
		boolean leerzeile = false;
		while (reader.readLine()) {
			String line = String.join("", reader.current());
			switchFile(line);
			
			// mehrere Leerzeilen zusammenfassen
			if (isBlank(line)) {
				if (leerzeile) {
					continue;
				}
				leerzeile = true;
				line = "";
			}
			
			boolean processed =
					// eine Überschrift?
					processHeading(line)
					// oder eine Fußnote?
					|| processFootnote(line)
					// oder normale Text-Zeile?
					|| processLine(reader, line);
			if (processed) {
				assert out != null;
				out.println();
			}
		}
		
		close();
	}

	private boolean isBlank(String line) {
		if ("<p>".equals(line) || " </p>".equals(line) || "<p> </p>".equals(line) || line.isBlank()) {
			return true;
		}

		return false;
	}

	private void switchFile(String line) throws IOException {
		Matcher matcher = KAPITEL.matcher(line);
		if (matcher.matches()) {
			String kapitel = matcher.group(2);
			if (! kapitel.endsWith(".")) {
				kapitel += ".";
			}
			anzahlKapitel++;
			openFile(String.format("%02d_%smd", anzahlKapitel, kapitel));
		} else if (ANMERKUNGEN.equals(line)) {
			openFile("99_footnotes.md");
		}
	}
	
	void close() {
		if (out != null) {
			System.out.printf("Datei %s exportiert%n", filename);
			out.close();
			out = null;
		}
	}
	
	void openFile(String newFilename) throws IOException {
		close();
		this.filename = newFilename;
		this.out = new PrintWriter(newFilename);
	}
	
	private static final Pattern HEADING = Pattern.compile("<(h[1-3])( class=\"[^\"]*\")?>(.*)</h[1-3]>");
	boolean processHeading(String line) {
		Matcher matcher = HEADING.matcher(line);
		if (matcher.matches() && out != null) {
			String tag = matcher.group(1);
			String text = stripTags(matcher.group(3));
			text = changeQuotes(text);
			out.printf("<%s>%s</%s>%n", tag, text, tag);
			
			return true;
		}
		
		return false;
	}
	
	private static final Pattern PATTERN_FOOTNOTE = Pattern.compile("<li class=\"rtejustify\"><a href=\"#R(\\d+)\" name=\"A\\d+\" id=\"A\\d+\">↑</a>(.*)</li>");
	boolean processFootnote(String line) {
		Matcher matcher = PATTERN_FOOTNOTE.matcher(line);
		if (matcher.matches()) {
			String result = matcher.group(2).trim();
			result = changeQuotes(result);
			String ref = matcher.group(1);
			out.printf("<@footnote %s \"%s\">%s</@footnote>%n", ref, filenameForReference(ref), result);
			
			return true;
		}
		
		return false;
	}

	String filenameForReference(String ref) {
		return references.get(ref);
	}

	private static final Pattern PARAGRAPH = Pattern.compile("(<p class=\"([^\"]*)\">)?(.*?)(</p>)?", Pattern.DOTALL);
	private static final Pattern SPLIT_LINE = Pattern.compile("<p class=\"([^\"]*)\">?(.*?)<br\\s*/>");
	private static final String[] BR1 = { "<", "br", " ", "/>" };
	private static final String[] BR2 = { "<", "br", "/>" };
	boolean processLine(LineReader reader, String currLine) throws IOException {
		String line = currLine;
		// mehrere Zeilen, durch <br /> getrennt, zusammenfassen
		List<String> completed = reader.current();
		Matcher matcher = SPLIT_LINE.matcher(currLine);
		if (matcher.matches() && reader.readLine()) {
			List<String> curr;
			do {
				curr = reader.current();
				completed.add("\n");
				completed.addAll(curr);
			} while ((TextUtils.endsWith(curr, BR1)
							|| TextUtils.endsWith(curr, BR2))
					&& reader.readLine());
			line = String.join("", completed);
		}
		
		matcher = PARAGRAPH.matcher(line);
		if (matcher.matches()) {
			String clazz  = matcher.group(2);
			String result = matcher.group(3);
			result = changeQuotes(result);
			result = replaceReferences(result);
			result = replaceFormat(result, clazz);
			result = escapeDigits(result);
			result = nonBreakingSpaces(result);

			if (! result.isBlank()) {
				out.println(result);
			}
			
			return true;
		}
		
		return false;
	}

	static String stripTags(String text) {
		return text.replaceAll("<[^<>]*>", "");
	}

	private void skipPreText(LineReader reader) throws IOException {
		do {
			List<String> line = reader.next();
			if (TextUtils.startsWith(line, START_KAPITEL)) {
				break;
			}
			// Cover?
			if (TextUtils.startsWith(line, START_COVER) && isUrl(params.getInput())) {
				readCover(line);
			}
		} while (reader.readLine());
	}

	private static final Pattern COVER = Pattern.compile("<img alt=\"\" src=\"([^\"]*)\"");
	private void readCover(List<String> line) throws IOException {
		Matcher matcher = COVER.matcher(String.join("", line));
		matcher.find();
		String partialUrl = matcher.group(1);
		URL url = new URL(params.getInput());
		url = new URL(url, partialUrl);
		readDocument(url, "cover.jpg");
	}

	static String changeQuotes(String line) {
		return line.replace('„', '»').replace('“', '«').replace('–', '—');
	}
	private static final Pattern PATTERN_REFERENCE = Pattern.compile("<sup><a href=\"#A\\d+\" name=\"R\\d+\" id=\"R\\d+\">\\[(\\d+)\\]</a></sup>");

	String replaceReferences(String input) {
		Matcher matcher = PATTERN_REFERENCE.matcher(input);
		if (matcher.find()) {
			references.put(matcher.group(1), filename);
			String result = matcher.replaceAll("<@refnote $1/>");

			return result;
		}
		
		return input;
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
