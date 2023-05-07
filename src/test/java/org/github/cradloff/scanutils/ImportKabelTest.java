package org.github.cradloff.scanutils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ImportKabelTest {
	private ImportKabel importKabel;
	private StringWriter out;
	
	@BeforeEach
	public void setUp() {
		importKabel = new ImportKabel() {
			@Override
			void openFile(String newFilename) throws IOException {
				// nichts tun
			}
			@Override
			void close() {
				// nichts tun
			}
			@Override
			String filenameForReference(String ref) {
				return "FILENAME";
			}
		};
		out = new StringWriter();
		importKabel.out = new PrintWriter(out);
	}
	
	@Test
	public void stripTags() {
		assertEquals("Text ohne Tags", ImportKabel.stripTags("Text ohne Tags"));
		assertEquals("Text ohne Tags", ImportKabel.stripTags("<h1><div class=\"strong\">Text <em>ohne</em> Tags<br/></div></h1>"));
	}

	@Test
	public void changeQuotes() {
		assertEquals("Text mit »Anführungszeichen«", ImportKabel.changeQuotes("Text mit „Anführungszeichen“"));
		assertEquals("Nein — nein", ImportKabel.changeQuotes("Nein – nein"));
	}

	@Test
	public void replaceReferences() {
		assertEquals("Abenteurerin<@refnote 1/>", importKabel.replaceReferences("Abenteurerin<sup><a href=\"#A1\" name=\"R1\" id=\"R1\">[1]</a></sup>"));
	}

	@Test
	public void replaceFormat() {
		assertEquals("Ist das *wirklich* wahr?", ImportKabel.replaceFormat("Ist das <span style=\"letter-spacing:2px\">wirklich</span> wahr?", "rtejustify"));
		assertEquals("Das steht hier.", ImportKabel.replaceFormat("Das steht <a href=\"/node/1234\">hier</a>.", "rtejustify"));
		assertEquals("> Dein Harald.", ImportKabel.replaceFormat("Dein Harald.", "rteindent2 rtejustify"));
		assertEquals("> > Dein Harald.", ImportKabel.replaceFormat("Dein Harald.", "rteindent3 rtejustify"));
		assertEquals("> > > Dein Harald.", ImportKabel.replaceFormat("Dein Harald.", "rteindent4 rtejustify"));
		assertEquals("<p class=\"centered\">Lieber Harald,</p>", ImportKabel.replaceFormat("Lieber Harald,", "rtecenter rteindent2"));
		assertEquals("<p class=\"right\">Dein Harald.</p>", ImportKabel.replaceFormat("Dein Harald.", "rteindent3 rteright"));
		assertEquals("<p class=\"right\">Dein Harald.</p>", ImportKabel.replaceFormat("Dein Harald.", "rteindent4 rteright"));
		assertEquals("<p class=\"centered\">Ist das <em>wirklich</em> wahr?</p>", ImportKabel.replaceFormat("Ist das <span style=\"letter-spacing:2px\">wirklich</span> wahr?", "rtecenter rteindent2"));
	}

	@Test
	public void escapeDigits() {
		assertEquals("1\\. April", ImportKabel.escapeDigits("1. April"));
		assertEquals("Der 1. April", ImportKabel.escapeDigits("Der 1. April"));
	}

	@Test
	public void nonBreakingSpaces() {
		assertEquals("Schmidt G.&nbsp;m.&nbsp;b.&nbsp;H.", ImportKabel.nonBreakingSpaces("Schmidt G. m. b. H."));
		assertEquals("über 10&nbsp;000 Mark", ImportKabel.nonBreakingSpaces("über 10 000 Mark"));
	}
	
	@Test
	public void processHeadings() {
		checkHeadings("<h2 class=\"rtecenter\"><span style=\"font-size:16px\"><strong>1. Kapitel.</strong></span></h2>",
				"<h2>1. Kapitel.</h2>\n");
		checkHeadings("<h3 class=\"rtecenter\"><span style=\"font-size:16px\"><strong>„Zum gestiefelten Kater“.</strong></span></h3>",
				"<h3>»Zum gestiefelten Kater«.</h3>\n");
		checkHeadings("<h3 class=\"rtecenter\"><span style=\"font-size:16px\"><strong>Untertitel mit <a href=\"/node/123#Verweis\">Verweis</a>.</strong></span></h3>",
				"<h3>Untertitel mit Verweis.</h3>\n");
	}

	private void checkHeadings(String line, String expected) {
		clearBuffer();
		importKabel.processHeading(line);
		String actual = out.toString();
		Assert.assertLinesEqual(expected, actual);
	}
	
	@Test
	public void processLine() throws IOException {
		checkProcessLine("unformatierte Zeile",
				"unformatierte Zeile\n");
		checkProcessLine("<p class=\"rtejustify\">Einfache Zeile.</p>", "Einfache Zeile.\n");
		checkProcessLine("Absatz mit „Quotes“.",
				"Absatz mit »Quotes«.\n");
		checkProcessLine("Absatz mit Referenz<sup><a href=\"#A1\" name=\"R1\" id=\"R1\">[1]</a></sup>.",
				"Absatz mit Referenz<@refnote 1/>.\n");
		checkProcessLine("<p class=\"rteindent2 rtejustify\">Formatierter Absatz.</p>",
				"> Formatierter Absatz.\n");
		checkProcessLine("<p class=\"rteindent3 rtejustify\">Formatierter Absatz.</p>",
				"> > Formatierter Absatz.\n");
		checkProcessLine("<p class=\"rteindent4 rtejustify\">Formatierter Absatz.</p>",
				"> > > Formatierter Absatz.\n");
		checkProcessLine("<p class=\"rteindent3 rteright\">Formatierter Absatz.</p>",
				"<p class=\"right\">Formatierter Absatz.</p>\n");
		checkProcessLine("<p class=\"rteindent3 rtecenter\">Formatierter Absatz.</p>",
				"<p class=\"centered\">Formatierter Absatz.</p>\n");
		checkProcessLine("<p class=\"rtejustify\"><span style=\"letter-spacing:2px\">Formatierter</span> Absatz.</p>",
				"*Formatierter* Absatz.\n");
		checkProcessLine("<p class=\"rteindent3 rteright\"><span style=\"letter-spacing:2px\">Formatierter</span> Absatz.</p>",
				"<p class=\"right\"><em>Formatierter</em> Absatz.</p>\n");
	}

	private void checkProcessLine(String line, String expected) throws IOException {
		LineReader reader = new LineReader(new StringReader(line));
		clearBuffer();
		importKabel.processLine(reader, line);
		String actual = out.toString();
		Assert.assertLinesEqual(expected, actual);
	}

	@Test
	public void processFootnote() {
		checkProcessFootnote("<li class=\"rtejustify\"><a href=\"#R1\" name=\"A1\" id=\"A1\">↑</a>Fußnote „eins“.</li>",
				"<@footnote 1 \"FILENAME\">Fußnote »eins«.</@footnote>\n");
	}

	private void checkProcessFootnote(String line, String expected) {
		clearBuffer();
		importKabel.processFootnote(line);
		String actual = out.toString();
		Assert.assertLinesEqual(expected, actual);
	}
	
	@Test
	public void importKabel() throws IOException {
		checkImport("Normaler Text,\nmit mehreren\nZeilen\n", "Normaler Text,\n\nmit mehreren\n\nZeilen\n\n");

		// Anführungszeichen durch Guillemets ersetzen
		checkImport("Text mit „Anführungszeichen“\n",
				"Text mit »Anführungszeichen«\n\n");

		// Kapitel-Überschriften in h2/h3-Tags verpacken
		checkImport("<h2 class=\"rtecenter\"><span style=\"font-size:16px\"><strong>1. Kapitel.</strong></span></h2>\n"
				+ "<h3 class=\"rtecenter\"><span style=\"font-size:16px\"><strong>Kapitelüberschrift.</strong></span></h3>\n"
				+ "Einfacher Absatz.\n"
				+ "<p class=\"rtecenter rteindent2\">Absatz mit<br />\n"
				+ "Umbruch.</p>\n"
				+ "<p class=\"rtejustify\">„Nun, aber —“</p>\n",
				
				"<h2>1. Kapitel.</h2>\n"
				+ "\n"
				+ "<h3>Kapitelüberschrift.</h3>\n"
				+ "\n"
				+ "Einfacher Absatz.\n"
				+ "\n"
				+ "<p class=\"centered\">Absatz mit<br />\n"
				+ "Umbruch.</p>\n"
				+ "\n"
				+ "»Nun, aber —«\n"
				+ "\n");
		checkImport("<h2>1. Kapitel.</h2>\n"
				+ "Absatz.\n",
				
				"<h2>1. Kapitel.</h2>\n"
				+ "\n"
				+ "Absatz.\n"
				+ "\n");

		// Ziffern mit Punkt escapen
		checkImport("1. April bis 1. Mai",
				"1\\. April bis 1. Mai\n\n");
		checkImport("31. Dezember",
				"31\\. Dezember\n\n");

		// G. m. b. H. und große Zahlen mit non breaking spaces zusammenhalten
		checkImport("Verlag moderner Lektüre G. m. b. H.",
				"Verlag moderner Lektüre G.&nbsp;m.&nbsp;b.&nbsp;H.\n\n");
		checkImport("Eine Summe zwischen 10 000 und 20 000",
				"Eine Summe zwischen 10&nbsp;000 und 20&nbsp;000\n\n");

		// Ersetzen von Referenzen
		checkImport("Text<sup><a href=\"#A1\" name=\"R1\" id=\"R1\">[1]</a></sup> mit<sup><a href=\"#A2\" name=\"R2\" id=\"R2\">[10]</a></sup> Referenzen<sup><a href=\"#A3\" name=\"R3\" id=\"R3\">[15]</a></sup>",
				"Text<@refnote 1/> mit<@refnote 10/> Referenzen<@refnote 15/>\n\n");

		// und von Fußnoten
		checkImport("<li class=\"rtejustify\"><a href=\"#R1\" name=\"A1\" id=\"A1\">↑</a>Fußnote „eins“.</li>\n" +
				"<li class=\"rtejustify\"><a href=\"#R2\" name=\"A2\" id=\"A2\">↑</a>Fußnote „zwei“.</li>\n",
				
				"<@footnote 1 \"FILENAME\">Fußnote »eins«.</@footnote>\n\n" +
				"<@footnote 2 \"FILENAME\">Fußnote »zwei«.</@footnote>\n\n");
	}

	private void checkImport(String line, String expected) throws IOException {
		String head = "<h1 class=\"rtecenter\"><strong><span style=\"font-size:28px\">Kapitel.</span></strong></h1>";
		try (StringReader in = new StringReader(head + "\n" + line)) {
			clearBuffer();
			importKabel.prepareText(in);
			String actual = out.toString();
			actual = actual.replace(head + "\n\n", "");
			Assert.assertLinesEqual(expected, actual);
		}
	}

	private void clearBuffer() {
		out.getBuffer().setLength(0);
	}
}
