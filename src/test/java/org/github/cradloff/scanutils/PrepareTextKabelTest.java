package org.github.cradloff.scanutils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import org.junit.Test;

public class PrepareTextKabelTest {

	@Test public void prepareTextKabel() throws IOException {
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
