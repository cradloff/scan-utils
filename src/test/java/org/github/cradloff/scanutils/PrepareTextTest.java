package org.github.cradloff.scanutils;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import org.junit.Test;

public class PrepareTextTest {
	@Test public void removeLitter() {
		checkRemoveLitter("iText ohne Schmierzeichen", "iText ohne Schmierzeichen");

		checkRemoveLitter(",Text mit Schmierzeichen vorn,", "Text mit Schmierzeichen vorn,");
		checkRemoveLitter("; Text mit Schmierzeichen vorn;", "Text mit Schmierzeichen vorn;");
		checkRemoveLitter(": Text mit Schmierzeichen vorn:", "Text mit Schmierzeichen vorn:");
		checkRemoveLitter("| Text mit Schmierzeichen vorn", "Text mit Schmierzeichen vorn");
		checkRemoveLitter("i Text mit Schmierzeichen vorn", "Text mit Schmierzeichen vorn");
		checkRemoveLitter("/ Text mit Schmierzeichen vorn", "Text mit Schmierzeichen vorn");
		checkRemoveLitter("\\ Text mit Schmierzeichen vorn", "Text mit Schmierzeichen vorn");
		checkRemoveLitter("i | / \\ Text mit Schmierzeichen vorn", "Text mit Schmierzeichen vorn");

		checkRemoveLitter("Text mit Schmierzeichen hinten |", "Text mit Schmierzeichen hinten");
		checkRemoveLitter("Text mit Schmierzeichen hinten i", "Text mit Schmierzeichen hinten");
		checkRemoveLitter("Text mit Schmierzeichen hinten j", "Text mit Schmierzeichen hinten");
		checkRemoveLitter("Text mit Schmierzeichen hinten /", "Text mit Schmierzeichen hinten");
		checkRemoveLitter("Text mit Schmierzeichen hinten :", "Text mit Schmierzeichen hinten");
		checkRemoveLitter("Text mit Schmierzeichen hinten ;", "Text mit Schmierzeichen hinten");
		checkRemoveLitter("Text mit Schmierzeichen hinten \\", "Text mit Schmierzeichen hinten");
		checkRemoveLitter("Text mit Schmierzeichen hinten | i j / ; : \\", "Text mit Schmierzeichen hinten");
	}

	private void checkRemoveLitter(String input, String expected) {
		String actual = PrepareText.removeLitter(input);
		assertEquals(expected, actual);
	}

	@Test public void changeQuotes() {
		checkChangeQuotes("Er sprach: *Hinweg!*", "Er sprach: \"Hinweg!\"");
		checkChangeQuotes("Er sprach: ®Hinweg!®", "Er sprach: \"Hinweg!\"");
	}

	private void checkChangeQuotes(String input, String expected) {
		String actual = PrepareText.changeQuotes(input);
		assertEquals(expected, actual);
	}

	@Test public void changeDash() {
		checkChangeDash("Kein »Trennzeichen«", "Kein »Trennzeichen«");

		checkChangeDash("Trenn=", "Trenn-");
		checkChangeDash("Trenn»", "Trenn-");
	}

	private void checkChangeDash(String input, String expected) {
		String actual = PrepareText.changeDash(input);
		assertEquals(expected, actual);
	}

	@Test public void prepareText() throws IOException {
		checkPrepareText("Normaler Text,\nmit mehreren\nZeilen\n", "Normaler Text,\nmit mehreren\nZeilen\n");

		checkPrepareText("| Text mit ,\n,Schmierzeichen\nund *Anführungszeichen® \n und Binde=\nstrich»\nen |",
				"Text mit\nSchmierzeichen\nund \"Anführungszeichen\"\nund Binde-\nstrich-\nen\n");
		checkPrepareText("\\|/ Text mit ,\n;Schmierzeichen\n:und *Anführungszeichen® \n und Binde=\nstrich»\nen i",
				"Text mit\nSchmierzeichen\nund \"Anführungszeichen\"\nund Binde-\nstrich-\nen\n");
		checkPrepareText("Text mit vielen\n\n\n\nLeerzeilen", "Text mit vielen\n\nLeerzeilen\n");
	}

	private void checkPrepareText(String line, String expected) throws IOException {
		try (
				StringReader in = new StringReader(line);
				StringWriter out = new StringWriter();
				) {
			PrepareText.prepareText(in, new PrintWriter(out));
			String actual = out.toString();
			assertEquals(expected, actual);
		}
	}
}
