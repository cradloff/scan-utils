package org.github.cradloff.scanutils;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class PreProcessTest {
	@Test public void testSplit() {
		checkSplit("Alle meine Entchen", "Alle", " ", "meine", " ", "Entchen");
		checkSplit(" Alle  meine  Entchen ", " ", "Alle", "  ", "meine", "  ", "Entchen", " ");
		checkSplit("Alle mei-ne Ent—chen", "Alle", " ", "mei-ne", " ", "Ent—chen");
		checkSplit("Alle, meine 'Entchen’", "Alle", ",", " ", "meine", " ", "'Entchen’");
		checkSplit("wollen wir7", "wollen", " ", "wir7");
		checkSplit("wollen wir?", "wollen", " ", "wir", "?");
		checkSplit("wollen wir?!", "wollen", " ", "wir", "?", "!");
		checkSplit("er war bleiern\\-schwerfällig ...", "er", " ", "war", " ", "bleiern\\-schwerfällig", " ", ".", ".", ".");
	}

	private void checkSplit(String line, String... wordsExcpected) {
		List<String> words = TextUtils.split(line);
		assertEquals(Arrays.asList(wordsExcpected), words);
	}

	@Test public void removeDashes() {
		assertEquals("Wort", PreProcess.removeDashes("Wort"));
		assertEquals("Wort", PreProcess.removeDashes("Wo-rt"));
		assertEquals("Wort", PreProcess.removeDashes("W-o--r-t"));
		assertEquals("Wort", PreProcess.removeDashes("W-o—r-t"));
		assertEquals("Wort-", PreProcess.removeDashes("Wort-"));
		assertEquals("bleiern\\-schwerfällig", PreProcess.removeDashes("bleiern\\-schwerfällig"));
	}

	@Test public void testSeven() {
		checkSeven("Wort ohne 7.", "Wort ohne 7.", 0);
		checkSeven("Absatz 7l und 7i", "Absatz 7l und 7i", 0);
		checkSeven("Wort7 mit7l sieben7i", "Wort? mit?! sieben?!", 3);
		checkSeven("Wort? mit?l sieben?i", "Wort? mit?! sieben?!", 2);
		checkSeven("57 Wörter mit 7 Silben7", "57 Wörter mit 7 Silben?", 1);
	}

	private void checkSeven(String line, String expected, int expectedCount) {
		List<String> words = TextUtils.split(line);
		int count = PreProcess.replaceSeven(words);
		String actual = String.join("", words);
		assertEquals(expected, actual);
		assertEquals("count", expectedCount, count);
	}

	@Test public void testFraction() {
		checkFraction("Um 1/2 12 Uhr", "Um ½ 12 Uhr", 1);
		checkFraction("1/2 1/3 2/3 1/4 3/4 1/5 2/5 3/5 4/5 1/6 5/6 1/7 1/8 3/8 5/8 7/8 1/9 1/10", "½ ⅓ ⅔ ¼ ¾ ⅕ ⅖ ⅗ ⅘ ⅙ ⅚ ⅐ ⅛ ⅜ ⅝ ⅞ ⅑ ⅒", 18);
		checkFraction("1/1 2/2 2/4 1/11 1/12", "1/1 2/2 2/4 1/11 1/12", 0);
		checkFraction("/1 1/2 1/", "/1 ½ 1/", 1);
	}

	private void checkFraction(String line, String expected, int expectedCount) {
		List<String> words = TextUtils.split(line);
		int count = PreProcess.replaceFraction(words);
		String actual = String.join("", words);
		assertEquals(expected, actual);
		assertEquals("count", expectedCount, count);
	}

	@Test public void testRemoveSil() {
		Set<String> dict = new HashSet<>(Arrays.asList("dein", "uns", "ihr", "Klub", "Harst"));
		checkRemoveSil("dein", "dein", dict);
		checkRemoveSil("uns", "uns", dict);
		checkRemoveSil("ihr", "ihr", dict);
		checkRemoveSil("Harsts", "Harsts", dict);
		checkRemoveSil("dseins", "deins", dict);
		checkRemoveSil("uinss", "uns", dict);
		checkRemoveSil("suns", "uns", dict);
		checkRemoveSil("siishr", "ihr", dict);
		checkRemoveSil("Kliusb", "Klub", dict);
		// 's' am Ende eines groß geschriebenen Wortes ignorieren
		checkRemoveSil("Kliusbs", "Klubs", dict);
		checkRemoveSil("Hlarst", "Harst", dict);
		checkRemoveSil("Hlairsst", "Harst", dict);
		checkRemoveSil("Hlairssts", "Harsts", dict);
	}

	private void checkRemoveSil(String input, String expected, Set<String> dict) {
		String actual = PreProcess.removeSil(input, dict);
		assertEquals(expected, actual);
	}

	@Test public void testPreProcess() throws IOException {
		Map<String, String> spellcheck = new HashMap<>();
		spellcheck.put("Aiie", "Alle");
		spellcheck.put("miene", "meine");
		spellcheck.put("mer", "wer");
		Set<String> dict = new HashSet<>();
		dict.add("alle");
		dict.add("Entchen");
		dict.add("es");
		dict.add("mal");
		dict.add("mir");
		dict.add("schwerfällig");
		dict.add("zu");
		checkPreProcess("Alle meine Entchen\n", "Alle meine Entchen\n", dict, spellcheck, 0);
		// meine ist nicht im Dictionary
		checkPreProcess("Al-le mei-ne Ent-chen\n", "Alle mei-ne Entchen\n", dict, spellcheck, 2);
		checkPreProcess("Aisie ,,miesne<< Ent.chen\n", "Alle »meine« Entchen\n", dict, spellcheck, 2);
		checkPreProcess("Ai-ie mi-ene Ent-chen\n", "Alle meine Entchen\n", dict, spellcheck, 3);
		checkPreProcess("Ai»ie7 meine7i Ent«ch.en7l\n", "Alle? meine?! Entchen?!\n", dict, spellcheck, 4);
		checkPreProcess("Alllei miene Eint-chenl Zsu Wasser-teich!\n", "Alle! meine Entchen! Zu Wasser-teich!\n", dict, spellcheck, 4);
		checkPreProcess("Ail»liel msal zsu msirl\n", "Alle! mal zu mir!\n", dict, spellcheck, 4);
		// keine Entfernung von Bindestrichen nach Backslash
		checkPreProcess("er war bleiern\\\\-schwerfällig ...\n", "er war bleiern\\\\-schwerfällig ...\n", dict, spellcheck, 0);
		checkPreProcess("er war hin\\\\-\nund hergerissen\n", "er war hin\\\\-\nund hergerissen\n", dict, spellcheck, 0);
		// einzelner Bindestrich am Zeilenende
		checkPreProcess("er war hier —\nund dort\n", "er war hier —\nund dort\n", dict, spellcheck, 0);
		// Bindestriche und Korrekturen bei Worttrennung am Zeilenende
		checkPreProcess("Ai-\nie mie-\nne Ent-\nchen\n", "Alle\nmeine\nEntchen\n", dict, spellcheck, 3);
		// keine Ersetzung von Silben bei Worttrennung am Zeilenende
		checkPreProcess("mer war im Zim-\nmer? Aiie!\n", "wer war im Zim-mer?\nAlle!\n", dict, spellcheck, 2);
		// s, i, l nicht bei Worttrennung am Zeilenende
		checkPreProcess("wir ess-\nen da-\nmals\n", "wir ess-en\nda-mals\n", dict, spellcheck, 0);
		// Brüche
		checkPreProcess("Um 1/2 12 Uhr", "Um ½ 12 Uhr\n", dict, spellcheck, 1);
		// <preprocess> soll immer am Anfang eines Absatzes stehen
		checkPreProcess("<@pagebreak/>\n\n", "\n<@pagebreak/>\n", dict, spellcheck, 0);
	}

	private void checkPreProcess(String line, String expected, Set<String> dict, Map<String, String> spellcheck, int expectedCount) throws IOException {
		PreProcess pp = new PreProcess();
		StringReader in = new StringReader(line);
		StringWriter out = new StringWriter();
		int count = pp.preProcess(in, out, spellcheck, dict);
		String actual = out.toString();
		assertEquals(expected, actual);
		assertEquals("count", expectedCount, count);
	}
}
