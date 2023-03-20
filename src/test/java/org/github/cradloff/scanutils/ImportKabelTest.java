package org.github.cradloff.scanutils;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import org.junit.Test;

public class ImportKabelTest {
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
		assertEquals("Abenteurerin<@refnote 1/>", ImportKabel.replaceReferences("Abenteurerin<sup><a href=\"#A1\" name=\"R1\" id=\"R1\">[1]</a></sup>"));
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
	public void importKabel() throws IOException {
		checkImport("Normaler Text,\nmit mehreren\nZeilen\n", "Normaler Text,\n\nmit mehreren\n\nZeilen\n\n");

		// Anführungszeichen durch Guillemets ersetzen
		checkImport("Text mit „Anführungszeichen“\n",
				"Text mit »Anführungszeichen«\n\n");

		// Kapitel-Überschriften in h2/h3-Tags verpacken
		checkImport("<h2 class=\"rtecenter\"><span style=\"font-size:16px\"><strong>1. Kapitel.</strong></span></h2>\n"
				+ "<h3 class=\"rtecenter\"><span style=\"font-size:16px\"><strong>Kapitelüberschrift.</strong></span></h3>\n"
				+ "Einfacher Absatz.\n"
				+ "<p class=\"rtejustify\">„Nun, aber —“</p>\n",
				
				"<h2>1. Kapitel.</h2>\n"
				+ "\n"
				+ "<h3>Kapitelüberschrift.</h3>\n"
				+ "\n"
				+ "Einfacher Absatz.\n"
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
		try (
				StringReader in = new StringReader(ImportKabel.BEGIN_OF_TEXT + "\n" + line);
				StringWriter out = new StringWriter();
				) {
			new ImportKabel().prepareText(in, new PrintWriter(out));
			String actual = out.toString();
			Assert.assertLinesEqual(expected, actual);
		}
	}
}
