package org.github.cradloff.scanutils;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

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

	@Test public void testSpecial() {
		checkSpecial("Wort ohne Sonderzeichen", "Wort ohne Sonderzeichen", 0);
		checkSpecial("no< ni<t", "noch nicht", 2);
		checkSpecial("Er bli>te zurü>", "Er blickte zurück", 2);
		checkSpecial("Er ni>te ni<t", "Er nickte nicht", 2);
		checkSpecial("dur<s<nittli<", "durchschnittlich", 3);
		checkSpecial("Er setzte si<,", "Er setzte sich,", 1);
		// keine Ersetzung in Tags
		checkSpecial("Er <em>nickte nicht</em>", "Er <em>nickte nicht</em>", 0);
		checkSpecial("Super<em>duper</em>gut", "Super<em>duper</em>gut", 0);
		checkSpecial("<@pagebreak/>", "<@pagebreak/>", 0);
		checkSpecial("<p style=\"white-space: pre-wrap;\">", "<p style=\"white-space: pre-wrap;\">", 0);
		checkSpecial("<tr><td>Text</td></tr>", "<tr><td>Text</td></tr>", 0);
		// aber dazwischen
		checkSpecial("Er <em>ni>te nicht</em>", "Er <em>nickte nicht</em>", 1);
		checkSpecial("Er <em>nickte ni<t</em>", "Er <em>nickte nicht</em>", 1);
		checkSpecial("Er ni>te<br/>ni<t", "Er nickte<br/>nicht", 2);
		checkSpecial("<tr><td>Er ni>te ni<t</td></tr>", "<tr><td>Er nickte nicht</td></tr>", 2);
		// keine Ersetzung am Zeilenanfang (Formatierung mit >)
		checkSpecial("> Er ni>te ni<t", "> Er nickte nicht", 2);
		// keine Ersetzung von >> und <<
		checkSpecial("Er >>ni>te<<.", "Er >>nickte<<.", 1);
		checkSpecial("Er >>ni>te<< ni<t", "Er >>nickte<< nicht", 2);

		// geschweifte Klammer wird durch 'sch' ersetzt
		checkSpecial("Er {wieg {on", "Er schwieg schon", 2);

		// gemischt
		checkSpecial("<em>Er {wieg s<on</em>", "<em>Er schwieg schon</em>", 2);
	}

	private void checkSpecial(String line, String expected, int expectedCount) {
		List<String> words = TextUtils.split(line);
		int count = PreProcess.replaceSpecial(words);
		assertEquals(TextUtils.split(expected), words);
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

	@Test public void testWhitespace() {
		// einige Satzzeichen kommen ausschließlich nach Wörtern, nie vor Leerzeichen
		checkWhitespace("Hier! ist, alles. in; Ordnung: ok?!", "Hier! ist, alles. in; Ordnung: ok?!", 0);
		checkWhitespace("Hier ! ist , alles . in ; Ordnung : ok ?!", "Hier! ist, alles. in; Ordnung: ok?!", 6);
		checkWhitespace("angeben ?«\n", "angeben?«\n", 1);
	}

	private void checkWhitespace(String line, String expected, int expectedCount) {
		List<String> words = TextUtils.split(line);
		int count = PreProcess.checkWhitespace(words);
		String actual = String.join("", words);
		assertEquals(expected, actual);
		assertEquals("count", expectedCount, count);
	}

	@Test public void testReplaceCharacters() {
		TreeSet<String> dict = new TreeSet<>(Arrays.asList("Schiff", "voraus", "Deck", "Verbrecher", "Zimmer", "sein", "fein", "Backenmuskulatur"));
		checkReplaceCharacters("Schiff", "Schiff", dict);
		checkReplaceCharacters("voraus", "voraus", dict);

		checkReplaceCharacters("Baelienmusknlaiuo", "Backenmuskulatur", dict);
		checkReplaceCharacters("5ehiss", "Schiff", dict);
		checkReplaceCharacters("$ehiss", "Schiff", dict);
		checkReplaceCharacters("rvoauf", "voraus", dict);
		checkReplaceCharacters("Vech", "Deck", dict);
		checkReplaceCharacters("Vceli", "Deck", dict);
		checkReplaceCharacters("Derhrecler", "Verbrecher", dict);
		checkReplaceCharacters("3innner", "Zimmer", dict);
		checkReplaceCharacters("Ziniwer", "Zimmer", dict);

		// Übereinstimmung mit den wenigsten Abweichungen vom Original werden bevorzugt
		checkReplaceCharacters("scin", "sein", dict);
		checkReplaceCharacters("sctn", "sein", dict);
		checkReplaceCharacters("fcin", "fein", dict);
		checkReplaceCharacters("fctn", "fein", dict);
	}

	private void checkReplaceCharacters(String input, String expected, SortedSet<String> dict) {
		String actual = LineProcessor.replaceCharacters(input, dict, 5);
		assertEquals(expected, actual);
	}

	@Test public void testPreProcess() throws Exception {
		Map<String, String> spellcheck = new HashMap<>();
		spellcheck.put("Aiie", "Alle");
		spellcheck.put("miene", "meine");
		spellcheck.put("mer", "wer");
		spellcheck.put("“", "«");
		spellcheck.put("”", "«");
		spellcheck.put("„", "»");

		Set<String> silben = new HashSet<>(Arrays.asList("en", "ch"));
		Set<String> dict = new HashSet<>(Arrays.asList("Schiff", "voraus", "alle", "Entchen", "er", "es", "hier", "mal", "mir", "war", "wir", "oh",
				"schwerfällig", "zu", "Piraten", "Uhr", "in"));
		checkPreProcess("Alle meine Entchen\n", "Alle meine Entchen\n", dict, silben, spellcheck, 0);
		checkPreProcess("Alle meine Ent<en {wimmen zum $<iff\n", "Alle meine Entchen schwimmen zum Schiff\n", dict, silben, spellcheck, 4);
		// meine ist nicht im Dictionary
		checkPreProcess("Al-le mei-ne Ent-chen\n", "Alle mei-ne Entchen\n", dict, silben, spellcheck, 2);
		checkPreProcess("Aiie ,,miene<< Ent.chen\n", "Alle »meine« Entchen\n", dict, silben, spellcheck, 2);
		checkPreProcess("Alle „meine“ „Entchen”\n", "Alle »meine« »Entchen«\n", dict, silben, spellcheck, 4);
		checkPreProcess("Ai-ie mi-ene ent=chen\n", "Alle meine Entchen\n", dict, silben, spellcheck, 3);
		checkPreProcess("Ai»ie7 meine7i Ent«ch.en7l\n", "Alle? meine?! Entchen?!\n", dict, silben, spellcheck, 4);
		checkPreProcess("Alle «miene» Ent-chen Zu Wasser-teich!\n", "Alle »meine« Entchen Zu Wasser-teich!\n", dict, silben, spellcheck, 2);
		checkPreProcess("Al»le mal zu mir\n", "Alle mal zu mir\n", dict, silben, spellcheck, 0);
		checkPreProcess("«Sehiss rvoauf!»\n", "»Schiff voraus!«\n", dict, silben, spellcheck, 2);
		checkPreProcess("Und dann — »2 Sonnen»,\n", "Und dann — »2 Sonnen«,\n", dict, silben, spellcheck, 0);
		checkPreProcess("für alle Fälle —»", "für alle Fälle —«", dict, silben, spellcheck, 0);
		// ein Bindestrich kann für ein beliebiges Zeichen stehen
		checkPreProcess("Ai-e me-ne En-ch-en\n", "Alle me-ne Entchen\n", dict, silben, spellcheck, 2);
		// keine Ersetzung von einzelnen Buchstaben
		checkPreProcess("G. m. b. H.\n", "G. m. b. H.\n", dict, silben, spellcheck, 0);
		// am Zeilenanfang werden Wörter ergänzt, die vorne abgeschnitten sind
		checkPreProcess("iff oraus\nlle ine\nntchen\n", "Schiff oraus\nalle ine\nEntchen\n", dict, silben, spellcheck, 3);
		// aber nur, wenn sie mehr als zwei Zeichen haben
		checkPreProcess("ff oraus\nle ine\nen\n", "ff oraus\nle ine\nen\n", dict, silben, spellcheck, 0);
		// Groß-/Kleinschreibung korrigieren
		checkPreProcess("piraten-SchIff vorauS\n", "Piraten-Schiff voraus\n", dict, silben, spellcheck, 3);
		checkPreProcess("pixaten-SchIss vorauS\n", "Piraten-Schiff voraus\n", dict, silben, spellcheck, 3);
		// nicht nach einem Bindestrich
		checkPreProcess("Modellauto und -schiff\n", "Modellauto und -schiff\n", dict, silben, spellcheck, 0);
		// durch Leerzeichen getrennte Wörter zusammenfassen
		checkPreProcess("Aiie mi ene Ent chen\n", "Alle meine Entchen\n", dict, silben, spellcheck, 3);
		// zusammengeschriebene Wörter wieder trennen
		checkPreProcess("Allemal zumir\n", "Alle mal zu mir\n", dict, silben, spellcheck, 2);
		// aber nur, wenn beide Wort-Bestandteile im Wörterbuch stehen
		checkPreProcess("Alleher zudir\n", "Alleher zudir\n", dict, silben, spellcheck, 0);
		// Ersetzung von besonderen Zeichen
		checkPreProcess("Âlle meine €ntch&n", "Alle meine Entchen", dict, silben, spellcheck, 2);
		checkPreProcess("Þiræten-$ch|ﬀ vørauſ", "Piraten-Schiff voraus", dict, silben, spellcheck, 3);
		// Ersetzung von Zeichen durch Ausrufezeichen
		checkPreProcess("Piratenl Schifft voraus1\n", "Piraten! Schiff! voraus!\n", dict, silben, spellcheck, 3);
		// keine Entfernung von Bindestrichen nach Backslash
		checkPreProcess("er war »bleiern\\\\-schwerfällig« ...\n", "er war »bleiern\\\\-schwerfällig« ...\n", dict, silben, spellcheck, 0);
		checkPreProcess("er war hin\\\\-\nund hergerissen\n", "er war hin\\\\-\nund hergerissen\n", dict, silben, spellcheck, 0);
		// einzelner Bindestrich am Zeilenende
		checkPreProcess("er war hier -—-\nund dort\n", "er war hier —\nund dort\n", dict, silben, spellcheck, 0);
		// Bindestriche und Korrekturen bei Worttrennung am Zeilenende
		checkPreProcess("Ai-\nie mie-\nne Ent-\nchen\n", "Alle\nmeine\nEntchen\n", dict, silben, spellcheck, 3);
		// Bindestrich und Schmierzeichen am Zeilenanfang
		checkPreProcess("Alle mie-\n»ne Entchen\n", "Alle meine\nEntchen\n", dict, silben, spellcheck, 1);
		checkPreProcess("Alle mie-\n«ne Entchen\n", "Alle meine\nEntchen\n", dict, silben, spellcheck, 1);
		checkPreProcess("Alle mie-\n,ne Entchen\n", "Alle meine\nEntchen\n", dict, silben, spellcheck, 1);
		// Bindestriche und Pagebreaks
		checkPreProcess("Alle mie-\n<@pagebreak/>\nne Entchen\n", "Alle meine\n<@pagebreak/>\nEntchen\n", dict, silben, spellcheck, 1);
		// keine Ersetzung von Silben bei Worttrennung am Zeilenende
		checkPreProcess("mer war im Zim-\nmer? Aiie!\n", "wer war im Zim-mer?\nAlle!\n", dict, silben, spellcheck, 2);
		// und auch nicht in der Zeile
		checkPreProcess("mer war im Zim-mer? Aiie!\n", "wer war im Zim-mer? Alle!\n", dict, silben, spellcheck, 2);
		// s, i, l nicht bei Worttrennung am Zeilenende
		checkPreProcess("wir ess-\nen da-\nmals\n", "wir ess-en\nda-mals\n", dict, silben, spellcheck, 0);
		// Ersetzung von Zeichen in Wörten mit Bindestrich
		checkPreProcess("Ai-ie zum Pivaien-5chifs\n", "Alle zum Piraten-Schiff\n", dict, silben, spellcheck, 3);
		// Wörter mit zusätzlichem Großbuchstaben am Anfang
		checkPreProcess("PViraten VPiraten VPixaten", "Piraten Piraten Piraten", dict, silben, spellcheck, 3);
		// Brüche
		checkPreProcess("Um 1/2 12 Uhr", "Um ½ 12 Uhr\n", dict, silben, spellcheck, 1);
		// <preprocess> soll immer am Anfang eines Absatzes stehen
		checkPreProcess("Zeile 1\n<@pagebreak/>\n\nZeile 2\n", "Zeile 1\n\n<@pagebreak/>\nZeile 2\n", dict, silben, spellcheck, 0);
		// keine Ersetzung von Silben (wenn z.B. Bindestrich fehlt)
		checkPreProcess("Ent ch en\n", "Ent ch en\n", dict, silben, spellcheck, 0);
		// keine Ersetzungen in HTML-Tags
		checkPreProcess("<h2>Sehiss rvoauf!</h2>\n", "<h2>Schiff voraus!</h2>\n", dict, silben, spellcheck, 2);
		checkPreProcess("<a href='aiie.mer'>Sehiss rvoauf!</a>", "<a href='aiie.mer'>Schiff voraus!</a>\n", dict, silben, spellcheck, 2);
		checkPreProcess("<img src=\"aiie.mer\"/>\n", "<img src=\"aiie.mer\"/>\n", dict, silben, spellcheck, 0);
		// Test mit mehreren Zeilen
		checkPreProcess("Al-le meine Ent-<en\n"
				+ "piraten-S<Iff vorauS\n"
				+ "\n"
				+ "Allemal zumir\n",

				"Alle meine Entchen\n"
				+ "Piraten-Schiff voraus\n"
				+ "\n"
				+ "Alle mal zu mir\n", dict, silben, spellcheck, 9);
	}

	private void checkPreProcess(String line, String expected, Set<String> dict, Set<String> silben, Map<String, String> spellcheck, int expectedCount)
			throws Exception {
		PreProcess pp = new PreProcess(new PreProcess.Parameter());
		try (
				StringReader in = new StringReader(line);
				StringWriter out = new StringWriter();
				PrintWriter log = new PrintWriter(new ByteArrayOutputStream());
				) {
			int count = pp.preProcess(in, out, log, spellcheck, dict, silben);
			String actual = out.toString();
			Assert.assertLinesEqual(expected, actual);
			assertEquals("count", expectedCount, count);
		}
	}
}
