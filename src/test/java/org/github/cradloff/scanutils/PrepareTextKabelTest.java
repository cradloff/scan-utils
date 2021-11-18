package org.github.cradloff.scanutils;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import org.junit.Test;

public class PrepareTextKabelTest {
	@Test
	public void stripTags() {
		assertEquals("Text ohne Tags", PrepareTextKabel.stripTags("Text ohne Tags"));
		assertEquals("Text ohne Tags", PrepareTextKabel.stripTags("<h1><div class=\"strong\">Text <em>ohne</em> Tags<br/></div></h1>"));
	}

	@Test
	public void changeQuotes() {
		assertEquals("Text mit »Anführungszeichen«", PrepareTextKabel.changeQuotes("Text mit „Anführungszeichen“"));
		assertEquals("Nein — nein", PrepareTextKabel.changeQuotes("Nein – nein"));
	}

	@Test
	public void replaceReferences() {
		assertEquals("Abenteurerin<@refnote 1/>", PrepareTextKabel.replaceReferences("Abenteurerin<sup><a href=\"#A1\" name=\"R1\" id=\"R1\">[1]</a></sup>"));
	}

	@Test
	public void replaceFormat() {
		assertEquals("Ist das *wirklich* wahr?", PrepareTextKabel.replaceFormat("Ist das <span style=\"letter-spacing:2px\">wirklich</span> wahr?", "rtejustify"));
		assertEquals("Das steht hier.", PrepareTextKabel.replaceFormat("Das steht <a href=\"/node/1234\">hier</a>.", "rtejustify"));
		assertEquals("> Dein Harald.", PrepareTextKabel.replaceFormat("Dein Harald.", "rteindent2 rtejustify"));
		assertEquals("> > Dein Harald.", PrepareTextKabel.replaceFormat("Dein Harald.", "rteindent3 rtejustify"));
		assertEquals("> > > Dein Harald.", PrepareTextKabel.replaceFormat("Dein Harald.", "rteindent4 rtejustify"));
		assertEquals("<p class=\"centered\">Lieber Harald,</p>", PrepareTextKabel.replaceFormat("Lieber Harald,", "rtecenter rteindent2"));
		assertEquals("<p class=\"right\">Dein Harald.</p>", PrepareTextKabel.replaceFormat("Dein Harald.", "rteindent3 rteright"));
		assertEquals("<p class=\"right\">Dein Harald.</p>", PrepareTextKabel.replaceFormat("Dein Harald.", "rteindent4 rteright"));
		assertEquals("<p class=\"centered\">Ist das <em>wirklich</em> wahr?</p>", PrepareTextKabel.replaceFormat("Ist das <span style=\"letter-spacing:2px\">wirklich</span> wahr?", "rtecenter rteindent2"));
	}

	@Test
	public void escapeDigits() {
		assertEquals("1\\. April", PrepareTextKabel.escapeDigits("1. April"));
		assertEquals("Der 1. April", PrepareTextKabel.escapeDigits("Der 1. April"));
	}

	@Test
	public void nonBreakingSpaces() {
		assertEquals("Schmidt G.&nbsp;m.&nbsp;b.&nbsp;H.", PrepareTextKabel.nonBreakingSpaces("Schmidt G. m. b. H."));
		assertEquals("über 10&nbsp;000 Mark", PrepareTextKabel.nonBreakingSpaces("über 10 000 Mark"));
	}

	public void prepareTextKabel() throws IOException {
		checkPrepareText("Normaler Text,\nmit mehreren\nZeilen\n", "Normaler Text,\nmit mehreren\nZeilen\n");

		// Anführungszeichen durch Guillemets ersetzen
		checkPrepareText("Text mit „Anführungszeichen“\n",
				"Text mit »Anführungszeichen«\n");

		// Kapitel-Überschriften in h2/h3-Tags verpacken
		checkPrepareText("1. Kapitel.\n"
				+ "Kapitelüberschrift.\n"
				+ "\n"
				+ "Absatz.\n",
				"<h2>1. Kapitel.</h2>\n"
				+ "<h3>Kapitelüberschrift.</h3>\n"
				+ "\n"
				+ "Absatz.\n");
		checkPrepareText("1. Kapitel.\n"
				+ "\n"
				+ "Absatz.\n",
				"<h2>1. Kapitel.</h2>\n"
				+ "\n"
				+ "Absatz.\n");

		// Ziffern mit Punkt escapen
		checkPrepareText("1. April bis 1. Mai",
				"1\\. April bis 1. Mai");
		checkPrepareText("31. Dezember",
				"31\\. Dezember");

		// G. m. b. H. und große Zahlen mit non breaking spaces zusammenhalten
		checkPrepareText("Verlag moderner Lektüre G. m. b. H.",
				"Verlag moderner Lektüre G.&nbsp;m.&nbsp;b.&nbsp;H.");
		checkPrepareText("Eine Summe zwischen 10 000 und 20 000",
				"Eine Summe zwischen 10&nbsp;000 und 20&nbsp;000");

		// Ersetzen von Referenzen
		checkPrepareText("Text[1] mit[10] Referenzen[15]", "Text<@refnote 1/> mit<@refnote 10/> Referenzen<@refnote 15/>");

		// und von Fußnoten
		checkPrepareText("   ↑ Fußnote „eins“.\n" +
				"   ↑ Fußnote „zwei“.\n",
				"<@footnote 1 \"FILENAME\">Fußnote »eins«.</@footnote>\n" +
				"<@footnote 2 \"FILENAME\">Fußnote »zwei«.</@footnote>\n");
	}

	private void checkPrepareText(String line, String expected) throws IOException {
		try (
				StringReader in = new StringReader(line);
				StringWriter out = new StringWriter();
				) {
			new PrepareTextKabel().prepareText(in, new PrintWriter(out));
			String actual = out.toString();
			Assert.assertLinesEqual(expected, actual);
		}
	}
}
