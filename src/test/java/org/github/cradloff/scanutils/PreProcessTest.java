package org.github.cradloff.scanutils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.*;
import java.util.*;

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;
import org.apache.commons.collections4.bag.TreeBag;
import org.github.cradloff.scanutils.PreProcess.Parameter;
import org.junit.jupiter.api.Test;

public class PreProcessTest {
	@Test public void testSatzzeichenErsetzen() {
		// an bestimmten Stellen darf nicht ersetzt werden
		checkSatzzeichen("Wort ohne 7.", "Wort ohne 7.", 0);
		checkSatzzeichen("Absatz 7l und 7i", "Absatz 7l und 7i", 0);
		checkSatzzeichen("Absatz 2l und 21", "Absatz 2l und 21", 0);
		checkSatzzeichen("<h2>Titel</h2>", "<h2>Titel</h2>", 0);
		// in bekannten Wörtern (Mukki) nichts ersetzen
		checkSatzzeichen("Wort Schnukki Mukki", "Wort Schnuk?! Mukki", 1);
		// die Endung kt ist sehr häufig und wird deswegen ebenfalls ignoriert
		checkSatzzeichen("Wort verdeckt", "Wort verdeckt", 0);

		checkSatzzeichen("Wort7 mit7l sieben7i", "Wort? mit?! sieben?!", 3);
		checkSatzzeichen("Wort7 mit7 l sieben7 i", "Wort? mit?! sieben?!", 3);
		checkSatzzeichen("Wort2 mit2l zwei21", "Wort? mit?! zwei?!", 3);
		checkSatzzeichen("Wort2 mitkl zwei21", "Wort? mit?! zwei?!", 3);
		checkSatzzeichen("Wort2 mit2 l zwei2 1", "Wort? mit?! zwei?!", 3);
		checkSatzzeichen("Wort7 mit7t sieben71", "Wort? mit?! sieben?!", 3);
		checkSatzzeichen("Wort? mit?l sieben?i", "Wort? mit?! sieben?!", 2);
		checkSatzzeichen("Wort? mit?t sieben?1", "Wort? mit?! sieben?!", 2);
		checkSatzzeichen("57 Wörter mit 7 Silben7", "57 Wörter mit 7 Silben?", 1);
		checkSatzzeichen("<h2>Wort7 mit7t zwei21</h2>", "<h2>Wort? mit?! zwei?!</h2>", 3);

		checkSatzzeichen("»Wer war’s? !« riefen sie.", "»Wer war’s?!« riefen sie.", 1);
		checkSatzzeichen("»Wer war’s? 1« riefen sie.", "»Wer war’s?!« riefen sie.", 1);
		checkSatzzeichen("»Wer war’s?1« riefen sie.", "»Wer war’s?!« riefen sie.", 1);
		checkSatzzeichen("»Er war’s! !« riefen sie.", "»Er war’s!!« riefen sie.", 1);
		checkSatzzeichen("»Er war’s! 1« riefen sie.", "»Er war’s!!« riefen sie.", 1);
		checkSatzzeichen("»Er war’s!1« riefen sie.", "»Er war’s!!« riefen sie.", 1);

		checkSatzzeichen("hierher … 1", "hierher …!", 1);
		checkSatzzeichen("hierher …1", "hierher …!", 1);
		checkSatzzeichen("hierher … 1", "hierher …!", 1);
		checkSatzzeichen("hierher …1", "hierher …!", 1);
		checkSatzzeichen("hierher … 11", "hierher …!!", 1);
		checkSatzzeichen("hierher …11", "hierher …!!", 1);
		checkSatzzeichen("hierher … !1", "hierher …!!", 1);
		checkSatzzeichen("hierher …!1", "hierher …!!", 1);
		checkSatzzeichen("hierher … ?1", "hierher …?!", 2);
		checkSatzzeichen("hierher …?1", "hierher …?!", 1);

		checkSatzzeichen("- und - dann -", "— und — dann —", 3);
		checkSatzzeichen("Ober- und Unter-Seite", "Ober- und Unter-Seite", 0);
		checkSatzzeichen("Brahma-Priester und -Tempel", "Brahma-Priester und -Tempel", 0);
	}

	private void checkSatzzeichen(String line, String expected, int expectedCount) {
		List<String> words = TextUtils.split(line);
		TreeBag<String> dict = new TreeBag<>();
		dict.add("Mukki");
		int count = PreProcess.satzzeichenErsetzen(words, dict);
		String actual = String.join("", words);
		assertEquals(expected, actual);
		assertEquals(expectedCount, count, "count");
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
		assertEquals(expectedCount, count, "count");
	}

	@Test public void testFraction() {
		checkFraction("Um 1/2 12 Uhr", "Um ½ 12 Uhr", 1);
		checkFraction("Um 21/4 Uhr", "Um 2¼ Uhr", 1);
		checkFraction("1/2 1/3 2/3 1/4 3/4 1/5 2/5 3/5 4/5 1/6 5/6 1/7 1/8 3/8 5/8 7/8 1/9 1/10", "½ ⅓ ⅔ ¼ ¾ ⅕ ⅖ ⅗ ⅘ ⅙ ⅚ ⅐ ⅛ ⅜ ⅝ ⅞ ⅑ ⅒", 18);
		checkFraction("1/1 2/2 2/4 1/11 1/12", "1/1 2/2 2/4 1/11 1/12", 0);
		checkFraction("/1 1/2 1/", "/1 ½ 1/", 1);
		checkFraction("1/100 1/23", "1/100 1/23", 0);
	}

	private void checkFraction(String line, String expected, int expectedCount) {
		List<String> words = TextUtils.split(line);
		int count = PreProcess.replaceFraction(words);
		String actual = String.join("", words);
		assertEquals(expected, actual);
		assertEquals(expectedCount, count, "count");
	}

	@Test public void testWhitespace() {
		// einige Satzzeichen kommen ausschließlich nach Wörtern, nie vor Leerzeichen
		checkWhitespace("Hier! ist, alles. in; Ordnung: ok?!", "Hier! ist, alles. in; Ordnung: ok?!", 0);
		checkWhitespace("Hier ! ist , alles . in ; Ordnung : ok ?!", "Hier! ist, alles. in; Ordnung: ok?!", 6);
		checkWhitespace("angeben ?«\n", "angeben?«\n", 1);
		checkWhitespace("angeben … ?«\n", "angeben …?«\n", 1);
		checkWhitespace("angeben … !«\n", "angeben …!«\n", 1);
		checkWhitespace("angeben … ?!«\n", "angeben …?!«\n", 1);
		checkWhitespace("angeben … !!«\n", "angeben …!!«\n", 1);
	}

	private void checkWhitespace(String line, String expected, int expectedCount) {
		List<String> words = TextUtils.split(line);
		int count = PreProcess.checkWhitespace(words);
		String actual = String.join("", words);
		assertEquals(expected, actual);
		assertEquals(expectedCount, count, "count");
	}

	@Test public void testReplaceQuotes() {
		checkReplaceQuotes("«verkehrte» Quotes werden «umgedreht»",
				"»verkehrte« Quotes werden »umgedreht«");
		checkReplaceQuotes("auch …» nach …?» bestimmten …!» Satzzeichen …?!» ersetzen …!!»",
				"auch …« nach …?« bestimmten …!« Satzzeichen …?!« ersetzen …!!«");
	}

	private void checkReplaceQuotes(String line, String expectedLine) {
		TreeBag<String> dict = new TreeBag<>();
		List<String> tokens = TextUtils.split(line);
		LineProcessor lineProcessor = new LineProcessor(new Parameter(), tokens, new HashMap<>(), dict, TextUtils.inverse(dict), new TreeSet<>(dict.uniqueSet()), new HashBag<>());
		for (int i = 0; i < tokens.size(); i++) {
			String token = lineProcessor.changeQuotes(tokens.get(i), i);
			tokens.set(i, token);
		}

		String actualLine = String.join("", tokens);
		assertEquals(expectedLine, actualLine);
	}

	@Test public void testReplaceCharacters() {
		List<String> line = new ArrayList<>();
		TreeBag<String> dict = new TreeBag<>(Arrays.asList("Schiff", "worauf", "Deck", "Verbrecher", "Zimmer", "sein", "sein", "fein", "Backenmuskulatur", "O’Hara"));
		dict.add("voraus", 10);
		dict.add("dann", 1000);
		dict.add("Dann");
		dict.add("Damm", 10);
		LineProcessor lineProcessor = new LineProcessor(new Parameter(), line, new HashMap<>(), dict, TextUtils.inverse(dict), new TreeSet<>(dict.uniqueSet()), new HashBag<>());
		// gibt es keine passende Ersetzung, wird das Wort wieder zurückgeliefert
		checkReplaceCharacters("Erbsensuppe", "Erbsensuppe", lineProcessor);

		checkReplaceCharacters("Baelienmusknlaiuo", "Backenmuskulatur", lineProcessor);
		checkReplaceCharacters("5ehiss", "Schiff", lineProcessor);
		checkReplaceCharacters("$ehiss", "Schiff", lineProcessor);
		checkReplaceCharacters("rvoauf", "voraus", lineProcessor);
		checkReplaceCharacters("Vech", "Deck", lineProcessor);
		checkReplaceCharacters("Vceli", "Deck", lineProcessor);
		checkReplaceCharacters("DeX", "Deck", lineProcessor);
		checkReplaceCharacters("Derhrecler", "Verbrecher", lineProcessor);
		checkReplaceCharacters("3innner", "Zimmer", lineProcessor);
		checkReplaceCharacters("Ziniwer", "Zimmer", lineProcessor);
		checkReplaceCharacters("0’Hgrg", "O’Hara", lineProcessor);

		// Übereinstimmungen mit den wenigsten Abweichungen vom Original werden bevorzugt
		checkReplaceCharacters("scin", "sein", lineProcessor);
		checkReplaceCharacters("sctn", "sein", lineProcessor);
		checkReplaceCharacters("fcin", "fein", lineProcessor);
		checkReplaceCharacters("fctn", "fein", lineProcessor);
		// "sein" ist häufiger als "fein"
		checkReplaceCharacters("jein", "sein", lineProcessor);
		// "voraus" ist 10x häufiger als "worauf", deshalb kann es ein zusätzliches Zeichen Unterschied haben
		checkReplaceCharacters("uoxauf", "voraus", lineProcessor);
		// bei zwei Unterschieden gewinnt aber "worauf"
		checkReplaceCharacters("woxauf", "worauf", lineProcessor);
		
		// bei unterschiedlicher Groß-/Kleinschreibung gewinnt das Wort mit identischer Groß-/Kleinschreibung
		// unabhängig von der Häufigkeit
		checkReplaceCharacters("danu", "dann", lineProcessor);
		checkReplaceCharacters("Danu", "Dann", lineProcessor);
	}

	private void checkReplaceCharacters(String input, String expected, LineProcessor lineProcessor) {
		String actual = lineProcessor.replaceCharacters(input, 5);
		assertEquals(expected, actual);
	}

	@Test public void testMergeLinebreak() throws IOException {
		Bag<String> dict = new HashBag<>(Set.of("Alle", "meine", "Entchen", "befestigt", "festigt"));
		// am Zeilenende getrennte Wörter werden wieder zusammengefügt
		checkMergeLinebreak("Al-\nle mei-\nne Ent-\nchen", "Alle\nmeine\nEntchen\n", dict);
		// nachfolgende Satzzeichen werden mitgenommen
		checkMergeLinebreak("Al-\nle, mei-\nne. Ent-\nchen", "Alle,\nmeine.\nEntchen\n", dict);
		// einige Buchstaben (sevr) stehen manchmal statt einem Bindestrich am Zeilenende
		checkMergeLinebreak("Als\nle meie\nne Entv\nchen", "Alle\nmeine\nEntchen\n", dict);
		checkMergeLinebreak("bes\nfestigt und be-\nfestigt", "befestigt\nund befestigt\n", dict);
		// manchmal kommt ein Quote statt einem Bindestrich
		checkMergeLinebreak("mei«\nne Ent»\nchen", "meine\nEntchen\n", dict);
		// manchmal auch ein Quote und ein Bindestrich
		checkMergeLinebreak("mei«-\nne Ent»-\nchen", "meine\nEntchen\n", dict);
		// teilweise fehlt der Bindestrich komplett
		checkMergeLinebreak("Al\nle mei\nne Ent\nchen", "Alle\nmeine\nEntchen\n", dict);
		// keine Ersetzung von bekannten Wörtern
		checkMergeLinebreak("erklärte\ner", "erklärte\ner", new HashBag<>(Set.of("er", "erklärte", "erklärter")));
		// Bindestrich und Schmierzeichen am Zeilenanfang
		checkMergeLinebreak("Alle mei-\n»ne Entchen", "Alle meine\nEntchen", dict);
		checkMergeLinebreak("Alle mei-\n«ne Entchen", "Alle meine\nEntchen", dict);
		checkMergeLinebreak("Alle mei-\n,ne Entchen", "Alle meine\nEntchen", dict);
		// Bindestriche und Pagebreaks
		checkMergeLinebreak("Alle mei-\n<@pagebreak/>\nne Entchen", "Alle meine\n<@pagebreak/>\nEntchen", dict);
	}

	private void checkMergeLinebreak(String line, String expected, Bag<String> dict) throws IOException {
		try (StringReader in = new StringReader(line)) {
			LineReader reader = new LineReader(in, 1, 2);
			StringBuilder actual = new StringBuilder();
			while (reader.readLine()) {
				PreProcess.mergeLinebreak(reader, dict);
				for (String token : reader.current()) {
					actual.append(token);
				}
				if (reader.hasNext()) {
					actual.append("\n");
				}
			}
			assertEquals(expected, actual.toString());
		}
	}

	@Test public void testPreProcess() throws Exception {
		Map<String, String> spellcheck = new HashMap<>();
		spellcheck.put("Aiie", "Alle");
		spellcheck.put("miene", "meine");
		spellcheck.put("mer", "wer");
		spellcheck.put("“", "«");
		spellcheck.put("”", "«");
		spellcheck.put("„", "»");
		spellcheck.put("…?1«", "…?!«");
		spellcheck.put("ic", "ich");
		spellcheck.put("ſagte", "sagte");

		Bag<String> silben = new HashBag<>(Arrays.asList("en", "ch"));
		Bag<String> dict = new HashBag<>(Arrays.asList("alle", "alle", "bereit", "dort", "Entchen", "Entchen", "er", "es", "halt", "hier", "hin", "hinzu",
				"im", "in", "mal", "mir", "Nachen", "nicht", "oh", "Piraten", "rief", "Schiff", "schwerfällig", "sie", "sie", "Uhr", "voraus", "war", "wer", "wir", "zu"));
		checkPreProcess("Alle meine Entchen\n", "Alle meine Entchen\n", dict, silben, spellcheck, 0);
		checkPreProcess("Alle meine Ent<en {wimmen zum $<iff\n", "Alle meine Entchen schwimmen zum Schiff\n", dict, silben, spellcheck, 4);
		// meine ist nicht im Dictionary
		checkPreProcess("Al-le mei-ne Ent-chen\n", "Alle mei-ne Entchen\n", dict, silben, spellcheck, 2);
		checkPreProcess("Aiie ,,miene<< Ent.chen ...\n", "Alle »meine« Entchen …\n", dict, silben, spellcheck, 2);
		checkPreProcess("Alle „meine“ „Entchen”\n", "Alle »meine« »Entchen«\n", dict, silben, spellcheck, 4);
		checkPreProcess("Ai-ie mi-ene ent=chen\n", "Alle meine Entchen\n", dict, silben, spellcheck, 3);
		checkPreProcess("Ai»ie7 meine7i Ent«ch.en7l\n", "Alle? meine?! Entchen?!\n", dict, silben, spellcheck, 4);
		checkPreProcess("Alle «miene» Ent-chen Zu Wasser-teich!\n", "Alle »meine« Entchen Zu Wasser-teich!\n", dict, silben, spellcheck, 2);
		checkPreProcess("Al»le mal zu mir …?1«\n", "Alle mal zu mir …?!«\n", dict, silben, spellcheck, 1);
		checkPreProcess("«Sehiss rvoauf!»\n", "»Schiff voraus!«\n", dict, silben, spellcheck, 2);
		checkPreProcess("Und dann — »2 Sonnen»,\n", "Und dann — »2 Sonnen«,\n", dict, silben, spellcheck, 0);
		checkPreProcess("für alle Fälle —»", "für alle Fälle —«", dict, silben, spellcheck, 0);
		checkPreProcess("hinüberbalanzieren. –»", "hinüberbalanzieren. –«", dict, silben, spellcheck, 0);
		// ein Bindestrich kann für ein beliebiges Zeichen stehen
		checkPreProcess("Ai-e me-ne En-ch-en\n", "Alle me-ne Entchen\n", dict, silben, spellcheck, 2);
		// Sonderzeichen, die nicht im deutschen Alphabet und nicht in similar_chars.csv vorkommen ebenfalls
		checkPreProcess("Allç h¡er ÿu mﬅr. þchiff v°raus.", "Alle hier zu mir. Schiff voraus.", dict, silben, spellcheck, 6);
		// Sonderzeichen in einem Wort ersetzen
		checkPreProcess("Pira!en bere:t?", "Piraten bereit?", dict, silben, spellcheck, 2);
		// keine Ersetzung von einzelnen Buchstaben
		checkPreProcess("G. m. b. H.\n", "G. m. b. H.\n", dict, silben, spellcheck, 0);
		checkPreProcess("U.&nbsp;G.&nbsp;B.-Gespenst\n", "U.&nbsp;G.&nbsp;B.-Gespenst\n", dict, silben, spellcheck, 0);
		// unterscheiden sich die Wörter nur in der Groß-/Kleinschreibung, wird die Original-Schreibweise bevorzugt
		checkPreProcess("Aiie, aiie\n", "Alle, alle\n", dict, silben, spellcheck, 2);
		checkPreProcess("Zie, zie\n", "Sie, sie\n", dict, silben, spellcheck, 2);
		// aber nur, wenn sie mehr als zwei Zeichen haben
		checkPreProcess("ff oraus\nle ine\nen\n", "ff oraus\nle ine\nen\n", dict, silben, spellcheck, 0);
		// Groß-/Kleinschreibung korrigieren
		checkPreProcess("piraten-SchIff vorauS\n", "Piraten-Schiff voraus\n", dict, silben, spellcheck, 3);
		checkPreProcess("pixaten-SchIss vorauS\n", "Piraten-Schiff voraus\n", dict, silben, spellcheck, 3);
		// nicht nach einem Bindestrich
		checkPreProcess("Modellauto und -schiff\n", "Modellauto und -schiff\n", dict, silben, spellcheck, 0);
		// durch Leerzeichen getrennte Wörter zusammenfassen
		checkPreProcess("Ai ie miene Ent chen\n", "Alle meine Entchen\n", dict, silben, spellcheck, 3);
		// zusammengeschriebene Wörter wieder trennen
		checkPreProcess("Allemal zumir\n", "Alle mal zu mir\n", dict, silben, spellcheck, 2);
		// aber nur, wenn beide Wort-Bestandteile im Wörterbuch stehen
		checkPreProcess("Alleher zudir\n", "Alleher zudir\n", dict, silben, spellcheck, 0);
		// Ersetzung von besonderen Zeichen
		checkPreProcess("Âlle meine €ntch&n", "Alle meine Entchen", dict, silben, spellcheck, 2);
		checkPreProcess("Þiræten-$ch|ﬀ vørauſ", "Piraten-Schiff voraus", dict, silben, spellcheck, 3);
		checkPreProcess("Dann ſagte ſic", "Dann sagte sie", dict, silben, spellcheck, 2);
		// Ersetzung von Zeichen durch Ausrufezeichen
		checkPreProcess("Piratenl Schifft voraus1\n", "Piraten! Schiff! voraus!\n", dict, silben, spellcheck, 3);
		checkPreProcess("»Wer war das? 1« fragte er.", "»Wer war das?!« fragte er.", dict, silben, spellcheck, 1);
		checkPreProcess("»Wer war das?1» fragte er.", "»Wer war das?!« fragte er.", dict, silben, spellcheck, 1);
		checkPreProcess("»Ich nicht! 1» sagte er.", "»Ich nicht!!« sagte er.", dict, silben, spellcheck, 1);
		checkPreProcess("»Er war's! !« riefen sie.", "»Er war’s!!« riefen sie.", dict, silben, spellcheck, 1);
		checkPreProcess("»Sie war's!1« rief er.", "»Sie war’s!!« rief er.", dict, silben, spellcheck, 1);
		// Ersetzung von Ausrufezeichen durch ein 't' oder 'l'
		checkPreProcess("Sie war berei!, er ha!! nich!!", "Sie war bereit, er halt nicht!", dict, silben, spellcheck, 3);
		// keine Entfernung von Bindestrichen nach Backslash
		checkPreProcess("er kam hin\\-zu ...\n", "er kam hin\\-zu …\n", dict, silben, spellcheck, 0);
		checkPreProcess("er war hin\\-\nund hergerissen\n", "er war hin\\-\nund hergerissen\n", dict, silben, spellcheck, 0);
		// einzelner Bindestrich am Zeilenende
		checkPreProcess("er war hier -—-\nund dort\n", "er war hier —\nund dort\n", dict, silben, spellcheck, 0);
		// Bindestriche und Korrekturen bei Worttrennung am Zeilenende
		checkPreProcess("Ai-\nie mie-\nne Ent-\nchen\n", "Alle\nmeine\nEntchen\n", dict, silben, spellcheck, 2);
		// Bindestriche und Pagebreaks
		checkPreProcess("Alle mie-\n<@pagebreak/>\nne Entchen\n", "Alle meine\n<@pagebreak/>\nEntchen\n", dict, silben, spellcheck, 1);
		// keine Ersetzung von Silben bei Worttrennung am Zeilenende
		checkPreProcess("mer war im Zim-\nmer? Aiie!\n", "wer war im Zimmer?\nAlle!\n", dict, silben, spellcheck, 2);
		// und auch nicht in der Zeile
		checkPreProcess("mer war im Zim-mer? Aiie!\n", "wer war im Zim-mer? Alle!\n", dict, silben, spellcheck, 2);
		// s, i, l nicht bei Worttrennung am Zeilenende
		checkPreProcess("wir ess-\nen da-\nmals\n", "wir essen\ndamals\n", dict, silben, spellcheck, 0);
		// Ersetzung von Zeichen in Wörten mit Bindestrich
		checkPreProcess("Ai-ie zum Pivaien-5chifs\n", "Alle zum Piraten-Schiff\n", dict, silben, spellcheck, 3);
		// Wörter mit zusätzlichem Großbuchstaben am Anfang
		checkPreProcess("PVIraten VPiraten VPixaten", "Piraten Piraten Piraten", dict, silben, spellcheck, 3);
		checkPreProcess("ZAiie AZiie", "Alle Alle", dict, silben, spellcheck, 2);
		// Brüche
		checkPreProcess("Um 1/2 12 Uhr", "Um ½ 12 Uhr\n", dict, silben, spellcheck, 1);
		// <@pagebreak/> soll immer am Anfang eines Absatzes stehen
		checkPreProcess("Zeile 1\n<@pagebreak/>\n\nZeile 2\n", "Zeile 1\n\n<@pagebreak/>\nZeile 2\n", dict, silben, spellcheck, 0);
		// aber nicht vor einer Überschrift
		checkPreProcess("Zeile 1\n<@pagebreak/>\n\n<h2>Zeile 2</h2>\n", "Zeile 1\n\n<@pagebreak/>\n\n<h2>Zeile 2</h2>\n", dict, silben, spellcheck, 0);
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

	private void checkPreProcess(String line, String expected, Bag<String> dict, Bag<String> silben, Map<String, String> spellcheck, int expectedCount)
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
			assertEquals(expectedCount, count, "count");
		}
	}
}
