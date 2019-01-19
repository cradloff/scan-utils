package org.github.cradloff.scanutils;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
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
	@Test public void testSeven() {
		checkSeven("Wort ohne 7.", "Wort ohne 7.", 0);
		checkSeven("Absatz 7l und 7i", "Absatz 7l und 7i", 0);
		checkSeven("Absatz 2l und 21", "Absatz 2l und 21", 0);
		checkSeven("<h2>Titel</h2>", "<h2>Titel</h2>", 0);

		checkSeven("Wort7 mit7l sieben7i", "Wort? mit?! sieben?!", 3);
		checkSeven("Wort2 mit2l zwei21", "Wort? mit?! zwei?!", 3);
		checkSeven("Wort7 mit7t sieben71", "Wort? mit?! sieben?!", 3);
		checkSeven("Wort? mit?l sieben?i", "Wort? mit?! sieben?!", 2);
		checkSeven("Wort? mit?t sieben?1", "Wort? mit?! sieben?!", 2);
		checkSeven("57 Wörter mit 7 Silben7", "57 Wörter mit 7 Silben?", 1);
		checkSeven("<h2>Wort7 mit7t zwei21</h2>", "<h2>Wort? mit?! zwei?!</h2>", 3);
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

	@Test public void testReplaceCharacters() {
		Set<String> dict = new HashSet<>(Arrays.asList("Schiff", "voraus", "Deck", "Verbrecher", "Zimmer", "sein", "fein", "Backenmuskulatur"));
		checkReplaceCharacters("Schiff", "Schiff", dict);
		checkReplaceCharacters("voraus", "voraus", dict);

		checkReplaceCharacters("Baelienmusknlaiuo", "Backenmuskulatur", dict);
		checkReplaceCharacters("5ehiss", "Schiff", dict);
		checkReplaceCharacters("rvoauf", "voraus", dict);
		checkReplaceCharacters("Vech", "Deck", dict);
		checkReplaceCharacters("Vceli", "Deck", dict);
		checkReplaceCharacters("Derhrecler", "Verbrecher", dict);
		checkReplaceCharacters("3innner", "Zimmer", dict);
		checkReplaceCharacters("Ziniwer", "Zimmer", dict);

		// Übereinstimmung mit den wenigsten Abweichungen vom Original werden bevorzugt
		checkReplaceCharacters("scin", "sein", dict);
		checkReplaceCharacters("fcin", "fein", dict);
	}

	private void checkReplaceCharacters(String input, String expected, Set<String> dict) {
		String actual = PreProcess.replaceCharacters(input, dict, 5);
		assertEquals(expected, actual);
	}

	@Test public void testPreProcess() throws IOException {
		Map<String, String> spellcheck = new HashMap<>();
		spellcheck.put("Aiie", "Alle");
		spellcheck.put("miene", "meine");
		spellcheck.put("mer", "wer");
		Set<String> silben = new HashSet<>(Arrays.asList("en", "ch"));
		Set<String> dict = new HashSet<>(Arrays.asList("Schiff", "voraus", "alle", "Entchen", "er", "es", "mal", "mir", "wir", "oh", "schwerfällig", "zu", "Piraten"));
		checkPreProcess("Alle meine Entchen\n", "Alle meine Entchen\n", dict, silben, spellcheck, 0);
		// meine ist nicht im Dictionary
		checkPreProcess("Al-le mei-ne Ent-chen\n", "Alle mei-ne Entchen\n", dict, silben, spellcheck, 2);
		checkPreProcess("Aisie ,,miesne<< Ent.chen\n", "Alle »meine« Entchen\n", dict, silben, spellcheck, 2);
		checkPreProcess("Ai-ie mi-ene ent-chen\n", "Alle meine Entchen\n", dict, silben, spellcheck, 3);
		checkPreProcess("Ai»ie7 meine7i Ent«ch.en7l\n", "Alle? meine?! Entchen?!\n", dict, silben, spellcheck, 4);
		checkPreProcess("Alllei «miene» Eint-chenl Zsu Wasser-teich!\n", "Alle! »meine« Entchen! Zu Wasser-teich!\n", dict, silben, spellcheck, 4);
		checkPreProcess("Ail»liel msal zsu msirl\n", "Alle! mal zu mir!\n", dict, silben, spellcheck, 4);
		checkPreProcess("«Sehiss rvoauf!»\n", "»Schiff voraus!«\n", dict, silben, spellcheck, 2);
		// durch Leerzeichen getrennte Wörter zusammenfassen
		checkPreProcess("Ai ie mi ene Ent chen\n", "Alle meine Entchen\n", dict, silben, spellcheck, 3);
		// Ersetzung von Zeichen durch Ausrufezeichen
		checkPreProcess("Piratenl Schifft voraus1\n", "Piraten! Schiff! voraus!\n", dict, silben, spellcheck, 3);
		// keine Entfernung von Bindestrichen nach Backslash
		checkPreProcess("er war »bleiern\\\\-schwerfällig« ...\n", "er war »bleiern\\\\-schwerfällig« ...\n", dict, silben, spellcheck, 0);
		checkPreProcess("er war hin\\\\-\nund hergerissen\n", "er war hin\\\\-\nund hergerissen\n", dict, silben, spellcheck, 0);
		// einzelner Bindestrich am Zeilenende
		checkPreProcess("er war hier —\nund dort\n", "er war hier —\nund dort\n", dict, silben, spellcheck, 0);
		// Bindestriche und Korrekturen bei Worttrennung am Zeilenende
		checkPreProcess("Ai-\nie mie-\nne Ent-\nchen\n", "Alle\nmeine\nEntchen\n", dict, silben, spellcheck, 3);
		// keine Ersetzung von Silben bei Worttrennung am Zeilenende
		checkPreProcess("mer war im Zim-\nmer? Aiie!\n", "wer war im Zim-mer?\nAlle!\n", dict, silben, spellcheck, 2);
		// und auch nicht in der Zeile
		checkPreProcess("mer war im Zim-mer? Aiie!\n", "wer war im Zim-mer? Alle!\n", dict, silben, spellcheck, 2);
		// s, i, l nicht bei Worttrennung am Zeilenende
		checkPreProcess("wir ess-\nen da-\nmals\n", "wir ess-en\nda-mals\n", dict, silben, spellcheck, 0);
		// Ersetzung von Zeichen in Wörten mit Bindestrich
		checkPreProcess("Ai-ie zum Pivaien-5chifs\n", "Alle zum Piraten-Schiff\n", dict, silben, spellcheck, 3);
		// Brüche
		checkPreProcess("Um 1/2 12 Uhr", "Um ½ 12 Uhr\n", dict, silben, spellcheck, 1);
		// <preprocess> soll immer am Anfang eines Absatzes stehen
		checkPreProcess("<@pagebreak/>\n\n", "\n<@pagebreak/>\n", dict, silben, spellcheck, 0);
		// keine Ersetzung von Silben (wenn z.B. Bindestrich fehlt)
		checkPreProcess("Ent ch en\n", "Ent ch en\n", dict, silben, spellcheck, 0);
		// keine Ersetzungen in HTML-Tags
		checkPreProcess("<h2>Sehiss rvoauf!</h2>\n", "<h2>Schiff voraus!</h2>\n", dict, silben, spellcheck, 2);
		checkPreProcess("<a href='aiie.mer'>Sehiss rvoauf!</a>", "<a href='aiie.mer'>Schiff voraus!</a>\n", dict, silben, spellcheck, 2);
	}

	private void checkPreProcess(String line, String expected, Set<String> dict, Set<String> silben, Map<String, String> spellcheck, int expectedCount) throws IOException {
		PreProcess pp = new PreProcess(new PreProcess.Parameter());
		try (
				StringReader in = new StringReader(line);
				StringWriter out = new StringWriter();
				PrintWriter log = new PrintWriter(new ByteArrayOutputStream());
				) {
			int count = pp.preProcess(in, out, log, spellcheck, dict, silben);
			String actual = out.toString();
			assertEquals(expected, actual);
			assertEquals("count", expectedCount, count);
		}
	}
}
