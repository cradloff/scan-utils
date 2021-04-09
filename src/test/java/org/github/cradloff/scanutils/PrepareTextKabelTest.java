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

		// Ersetzen von Referenzen
		checkPrepareText("Text mit Referenzen[1]", "Text mit Referenzen<@refnote 1/>");

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
