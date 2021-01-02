package org.github.cradloff.scanutils;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class PrepareTextTest {
	@Test public void removeLitter() {
		checkRemoveLitter("iText ohne Schmierzeichen", "iText ohne Schmierzeichen");

		checkRemoveLitter("Schmierzeichen_ mitten% im #Text", "Schmierzeichen mitten im Text");

		checkRemoveLitter(",Text mit Schmierzeichen vorn,", "Text mit Schmierzeichen vorn,");
		checkRemoveLitter("‚Text mit Schmierzeichen vorn,", "Text mit Schmierzeichen vorn,"); // Apostroph unten
		checkRemoveLitter("; Text mit Schmierzeichen vorn;", "Text mit Schmierzeichen vorn;");
		checkRemoveLitter(": Text mit Schmierzeichen vorn:", "Text mit Schmierzeichen vorn:");
		checkRemoveLitter("_ Text mit Schmierzeichen vorn", "Text mit Schmierzeichen vorn");
		checkRemoveLitter("| Text mit Schmierzeichen vorn", "Text mit Schmierzeichen vorn");
		checkRemoveLitter("i Text mit Schmierzeichen vorn", "Text mit Schmierzeichen vorn");
		checkRemoveLitter("/ Text mit Schmierzeichen vorn", "Text mit Schmierzeichen vorn");
		checkRemoveLitter("\\ Text mit Schmierzeichen vorn", "Text mit Schmierzeichen vorn");
		checkRemoveLitter("i | / \\ ‘’., Text mit Schmierzeichen vorn", "Text mit Schmierzeichen vorn");

		checkRemoveLitter("Text mit Schmierzeichen hinten |", "Text mit Schmierzeichen hinten");
		checkRemoveLitter("Text mit Schmierzeichen hinten i", "Text mit Schmierzeichen hinten");
		checkRemoveLitter("Text mit Schmierzeichen hinten j", "Text mit Schmierzeichen hinten");
		checkRemoveLitter("Text mit Schmierzeichen hinten _", "Text mit Schmierzeichen hinten");
		checkRemoveLitter("Text mit Schmierzeichen hinten /", "Text mit Schmierzeichen hinten");
		checkRemoveLitter("Text mit Schmierzeichen hinten :", "Text mit Schmierzeichen hinten");
		checkRemoveLitter("Text mit Schmierzeichen hinten ;", "Text mit Schmierzeichen hinten");
		checkRemoveLitter("Text mit Schmierzeichen hinten \\", "Text mit Schmierzeichen hinten");
		checkRemoveLitter("Text mit Schmierzeichen hinten | i j / ; : \\ ‘’ . ,", "Text mit Schmierzeichen hinten");
	}

	private void checkRemoveLitter(String input, String expected) {
		String actual = PrepareText.removeLitter(input);
		assertEquals(expected, actual);
	}

	@Test public void changeQuotes() {
		checkChangeQuotes("Er sprach: *Hinweg*", "Er sprach: »Hinweg«");
		checkChangeQuotes("Er sprach: *Hinweg!*", "Er sprach: »Hinweg!«");
		checkChangeQuotes("Er sprach: \"Hinweg!\"", "Er sprach: »Hinweg!«");
		checkChangeQuotes("Er sprach: “Hinweg!”", "Er sprach: »Hinweg!«");
		checkChangeQuotes("Er sprach: „Hinweg!”", "Er sprach: »Hinweg!«");
		checkChangeQuotes("Er sprach: „Hinweg! ”", "Er sprach: »Hinweg!«");
	}

	private void checkChangeQuotes(String input, String expected) {
		String actual = PrepareText.changeQuotes(input);
		assertEquals(expected, actual);
	}

	@Test public void changeDash() {
		checkChangeDash("Kein »Trennzeichen«", "Kein »Trennzeichen«");

		checkChangeDash("Trenn=", "Trenn-");
		checkChangeDash("Trenn»", "Trenn-");
		checkChangeDash("Da = passierte es!", "Da — passierte es!");
	}

	private void checkChangeDash(String input, String expected) {
		String actual = PrepareText.changeDash(input);
		assertEquals(expected, actual);
	}

	@Test public void changeSpecial() {
		checkSpecial("Wort ohne Sonderzeichen", "Wort ohne Sonderzeichen");
		checkSpecial("<@pagebreak/>", "<@pagebreak/>");

		// "\" wird durch "s" ersetzt
		checkSpecial("\\o war\\!", "so wars!");

		// "<" wird durch "ch" ersetzt
		checkSpecial("no< ni<t", "noch nicht");
		// ">" durch "ck"
		checkSpecial("Er bli>te zurü>", "Er blickte zurück");
		checkSpecial("Er ni>te ni<t", "Er nickte nicht");
		checkSpecial("Komm ni<t zurü>", "Komm nicht zurück");
		checkSpecial("dur<s<nittli<", "durchschnittlich");
		checkSpecial("Er setzte si<,", "Er setzte sich,");

		checkSpecial("Es i} ſchon gut.", "Es ist schon gut.");

		// "{" wird durch "sch" ersetzt, nach einen "s" nur durch "ch"
		checkSpecial("zwi{en den Büs{en", "zwischen den Büschen");
		// "{<" wird durch "sch" ersetzt
		checkSpecial("zwis{<en den Bü{<en", "zwischen den Büschen");
	}

	private void checkSpecial(String input, String expected) {
		String actual = PrepareText.changeSpecial(input);
		assertEquals(expected, actual);
	}

	private static final Map<String, String> REPLACEMENTS = new HashMap<>();
	static {
		REPLACEMENTS.put("Bieter", "Meter");
		REPLACEMENTS.put("Fahren", "Jahren");
		REPLACEMENTS.put("vor!", "dort");
		REPLACEMENTS.put("\\b([\\d]+)len\\b", "$1ten");
	}
	@Test public void replaceOnce() {
		checkReplaceOnce("Nach vor! in zehn Bieter.", "Nach dort in zehn Meter.");
		checkReplaceOnce("Vor zwei Fahren", "Vor zwei Jahren");
		checkReplaceOnce("Am 2ten, 3len und 15len", "Am 2ten, 3ten und 15ten");
	}

	private void checkReplaceOnce(String input, String expected) {
		String actual = PrepareText.replaceOnce(input, REPLACEMENTS);
		assertEquals(expected, actual);
	}

	@Test public void prepareText() throws IOException {
		checkPrepareText("Normaler Text,\nmit mehreren\nZeilen\n", "Normaler Text,\nmit mehreren\nZeilen\n");

		checkPrepareText("| Text -- mit ,\n,Schmierzei<en\nund \"Anführungszeichen® \n und Binde=\nstrich»\nen |",
				"Text — mit\nSchmierzeichen\nund »Anführungszeichen«\nund Binde-\nstrich-\nen\n");
		checkPrepareText("\\ |/ Text mit ,\n;Schmierzeichen\n:und *Anführungszeichen® \n und Binde=\nstrich»\nen i",
				"Text mit\nSchmierzeichen\nund »Anführungszeichen«\nund Binde-\nstrich-\nen\n");
		checkPrepareText("Text mit vielen\n\n\n\nLeerzeilen", "Text mit vielen\n\nLeerzeilen\n");
		checkPrepareText("Text' mit >>Sonderzeichen<<", "Text’ mit »Sonderzeichen«\n");
		checkPrepareText("werden. — Beeile Dich.“ — u", "werden. — Beeile Dich.« —\n");
		checkPrepareText("Nach vor! in zehn Bieter.", "Nach dort in zehn Meter.");
	}

	private void checkPrepareText(String line, String expected) throws IOException {
		try (
				StringReader in = new StringReader(line);
				StringWriter out = new StringWriter();
				) {
			PrepareText.prepareText(in, new PrintWriter(out), REPLACEMENTS);
			String actual = out.toString();
			Assert.assertLinesEqual(expected, actual);
		}
	}
}
