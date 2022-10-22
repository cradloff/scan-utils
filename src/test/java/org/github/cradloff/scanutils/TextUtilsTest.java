package org.github.cradloff.scanutils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;
import org.junit.Test;

public class TextUtilsTest {
	@Test public void testSplit() {
		checkSplit("Alle meine Entchen", "Alle", " ", "meine", " ", "Entchen");
		checkSplit(" Alle  meine  Entchen ", " ", "Alle", "  ", "meine", "  ", "Entchen", " ");
		checkSplit("A1le mei-ne Ent—chen", "A1le", " ", "mei-ne", " ", "Ent—chen");
		checkSplit("Alle, meine 'Entchen’", "Alle", ",", " ", "meine", " ", "'Entchen’");
		checkSplit("Alle meine Ent-", "Alle", " ", "meine", " ", "Ent-");
		checkSplit(">>Alle ,,meine,, €ntchen?!<<", ">>", "Alle", " ", ",,", "meine", ",,", " ", "€ntchen", "?!", "<<");
		checkSplit("»Alle „meine“ €ntchen?!«", "»", "Alle", " ", "„", "meine", "“", " ", "€ntchen", "?!«");
		checkSplit("auf- und abwärts", "auf", "-", " ", "und", " ", "abwärts");
		checkSplit("Insel-Cafee", "Insel", "-", "Cafee");
		checkSplit("wollen wir7", "wollen", " ", "wir7");
		checkSplit("wóllen wir?", "wóllen", " ", "wir", "?");
		checkSplit("wøllen wir?!", "wøllen", " ", "wir", "?!");
		checkSplit("wollen wir…", "wollen", " ", "wir", "…");
		checkSplit("er war bleiern\\-schwerfällig ...", "er", " ", "war", " ", "bleiern\\-schwerfällig", " ", "...");
		checkSplit("Und -—- Dann", "Und", " ", "-—-", " ", "Dann");
		checkSplit("Und dann -—-", "Und", " ", "dann", " ", "-—-");
		checkSplit("Mode£lauto und -schiff", "Mode£lauto", " ", "und", " ", "-schiff");
		checkSplit("Dann ſagte ſie", "Dann", " ", "ſagte", " ", "ſie");
		checkSplit("<h1>1. Kapitel.</h1>", "<", "h1", ">", "1", ".", " ", "Kapitel", ".", "</", "h1", ">");
		checkSplit("<@pagebreak 3/>", "<@", "pagebreak", " ", "3", "/>");
		checkSplit("Âlle mein& €ntchen", "Âlle", " ", "mein&", " ", "€ntchen");
		checkSplit("Þiræten-$ch|ﬀ vørauſ", "Þiræten", "-", "$ch|ﬀ", " ", "vørauſ");
		checkSplit("(runde), [eckige] und {geschweifte} Klammern", "(", "runde", "),", " ", "[", "eckige", "]", " ", "und", " ", "{", "geschweifte", "}", " ", "Klammern");
	}

	private void checkSplit(String line, String... wordsExcpected) {
		List<String> words = TextUtils.split(line);
		assertEquals(Arrays.asList(wordsExcpected), words);
	}

	@Test public void isDash() {
		assertTrue(TextUtils.isDash('-'));
		assertTrue(TextUtils.isDash('—'));
		assertFalse(TextUtils.isDash('.'));
		assertFalse(TextUtils.isDash('+'));
	}

	@Test public void endsWithDash() {
		assertFalse(TextUtils.endsWithDash(""));
		assertFalse(TextUtils.endsWithDash("Text ohne Dash"));
		assertTrue(TextUtils.endsWithDash("Text mit Dash -"));
		assertTrue(TextUtils.endsWithDash("Text mit Dash —"));
		assertFalse(TextUtils.endsWithDash("Text mit escaped Dash \\-"));
		assertFalse(TextUtils.endsWithDash("Text mit escaped Dash \\—"));
	}

	@Test public void removeDashes() {
		assertEquals("Wort", TextUtils.removeDashes("Wort"));
		assertEquals("Wort", TextUtils.removeDashes("Wo-rt"));
		assertEquals("Wort", TextUtils.removeDashes("W-o--r-t"));
		assertEquals("Wort", TextUtils.removeDashes("W-o—r-t"));
		assertEquals("Wort-", TextUtils.removeDashes("Wort-"));
		assertEquals("bleiern\\-schwerfällig", TextUtils.removeDashes("bleiern\\-schwerfällig"));
	}

	@Test public void isSatzzeichen() {
		assertTrue(TextUtils.isSatzzeichen("."));
		assertTrue(TextUtils.isSatzzeichen("…"));
		assertTrue(TextUtils.isSatzzeichen(","));
		assertTrue(TextUtils.isSatzzeichen(";"));
		assertTrue(TextUtils.isSatzzeichen(":"));
		assertTrue(TextUtils.isSatzzeichen("-"));
		assertTrue(TextUtils.isSatzzeichen("»"));
		assertTrue(TextUtils.isSatzzeichen("«"));
		assertTrue(TextUtils.isSatzzeichen(".…,;:-»«"));
		assertFalse(TextUtils.isSatzzeichen(""));
		assertFalse(TextUtils.isSatzzeichen("a"));
		assertFalse(TextUtils.isSatzzeichen("»a«"));
		// Leerzeichen gelten nicht als Satzzeichen
		assertFalse(TextUtils.isSatzzeichen(".,;: -»«"));
	}

	@Test public void testSatzzeichenErsetzen() {
		assertEquals("", TextUtils.satzzeichenErsetzen(""));
		assertEquals("Hier. Da ist’s.", TextUtils.satzzeichenErsetzen("Hier· Da ist's."));
		assertEquals("»Hier — dort — und — wo — anders —«", TextUtils.satzzeichenErsetzen(">>Hier -- dort -+ und -— wo == anders -=<<"));
		assertEquals("Apostroph unten, Komma", TextUtils.satzzeichenErsetzen("Apostroph unten‚ Komma"));
		assertEquals("… und … so … weiter …", TextUtils.satzzeichenErsetzen("... und .…. so …. weiter ......"));
		assertEquals("… und … so … fort.", TextUtils.satzzeichenErsetzen(".-. und .,. so ,. fort."));
		assertEquals("und Sie, Mr., kommen her", TextUtils.satzzeichenErsetzen("und Sie, Mr., kommen her"));
		// Zeilen, die nur aus Bindestrichen oder Gleichheitszeichen bestehen, werden ignoriert (Markierung für Überschrift)
		assertEquals("--", TextUtils.satzzeichenErsetzen("--"));
		assertEquals("==", TextUtils.satzzeichenErsetzen("=="));
	}

	@Test public void isWord() {
		assertTrue(TextUtils.isWord("Wort"));
		assertTrue(TextUtils.isWord("ist’s"));
		assertTrue(TextUtils.isWord("ſie"));
		assertFalse(TextUtils.isWord("123"));
		assertFalse(TextUtils.isWord(""));
		assertFalse(TextUtils.isWord("«"));
		assertFalse(TextUtils.isWord(" "));
	}

	@Test public void isAlphaNumeric() {
		assertTrue(TextUtils.isAlphaNumeric("123"));
		assertTrue(TextUtils.isAlphaNumeric("Wort"));
		assertTrue(TextUtils.isAlphaNumeric("ist’s"));
		assertFalse(TextUtils.isAlphaNumeric(""));
		assertFalse(TextUtils.isAlphaNumeric("«"));
		assertFalse(TextUtils.isAlphaNumeric(" "));
	}

	@Test public void textBefore() {
		//                                   0 2   4    6   8  10  12 13
		List<String> line = TextUtils.split("… Ein Satz mit 1a und A2?!");
		assertFalse(TextUtils.textBefore(line, 0));
		assertFalse(TextUtils.textBefore(line, 1));
		assertFalse(TextUtils.textBefore(line, 2));
		assertFalse(TextUtils.textBefore(line, 14));
		assertTrue(TextUtils.textBefore(line, 3));
		assertTrue(TextUtils.textBefore(line, 9));
		assertTrue(TextUtils.textBefore(line, 13));
	}

	@Test public void textAfter() {
		//                                   0 2   4    6   8  10  12 13
		List<String> line = TextUtils.split("… Ein Satz mit 1a und A2?!");
		assertFalse(TextUtils.textAfter(line, -1));
		assertFalse(TextUtils.textAfter(line, 0));
		assertFalse(TextUtils.textAfter(line, 2));
		assertFalse(TextUtils.textAfter(line, 12));
		assertFalse(TextUtils.textAfter(line, 13));
		assertTrue(TextUtils.textAfter(line, 1));
		assertTrue(TextUtils.textAfter(line, 7));
		assertTrue(TextUtils.textAfter(line, 11));
	}

	@Test public void isWhitespace() {
		assertTrue(TextUtils.isWhitespace(""));
		assertTrue(TextUtils.isWhitespace(" "));
		assertTrue(TextUtils.isWhitespace(" \t\r\n "));
		assertFalse(TextUtils.isWhitespace("."));
		assertFalse(TextUtils.isWhitespace("a"));
		assertFalse(TextUtils.isWhitespace("5"));
	}

	@Test public void endsWith() {
		List<String> line = Arrays.asList("a", " ", "c");
		assertTrue(TextUtils.endsWith(line, "c"));
		assertTrue(TextUtils.endsWith(line, " ", "c"));
		assertTrue(TextUtils.endsWith(line, "a", " ", "c"));
		assertFalse(TextUtils.endsWith(line, "a"));
		assertFalse(TextUtils.endsWith(line, " "));
		assertFalse(TextUtils.endsWith(line, "a", "c"));
		assertFalse(TextUtils.endsWith(line, "x", "a", " ", "c"));
	}

	@Test public void endsWithOneOf() {
		List<String> line = Arrays.asList("a", " ", "c");
		assertTrue(TextUtils.endsWithOneOf(line, "c"));
		assertTrue(TextUtils.endsWithOneOf(line, "b", "c", "d"));
		assertFalse(TextUtils.endsWithOneOf(line, "a", " ", "b"));
	}

	@Test public void startsWith() {
		List<String> line = Arrays.asList("a", " ", "c");
		assertTrue(TextUtils.startsWith(line, "a"));
		assertTrue(TextUtils.startsWith(line, "a", " "));
		assertTrue(TextUtils.startsWith(line, "a", " ", "c"));

		assertFalse(TextUtils.startsWith(line, " "));
		assertFalse(TextUtils.startsWith(line, "c"));
		assertFalse(TextUtils.startsWith(line, "a", " ", "c", "d"));
	}

	@Test public void regionMatches() {
		List<String> line = Arrays.asList("a", " ", "c");
		assertTrue(TextUtils.regionMatches(line, 0, "a"));
		assertTrue(TextUtils.regionMatches(line, 0, "a", " ", "c"));
		assertTrue(TextUtils.regionMatches(line, 1, " ", "c"));
		assertTrue(TextUtils.regionMatches(line, 2, "c"));

		assertFalse(TextUtils.regionMatches(line, 1, "a"));
		assertFalse(TextUtils.regionMatches(line, -1, "a"));
		assertFalse(TextUtils.regionMatches(line, 3, "a"));
	}

	@Test public void startOfTag() {
		assertTrue(TextUtils.startOfTag("<"));
		assertTrue(TextUtils.startOfTag("</"));
		assertTrue(TextUtils.startOfTag("<@"));
		assertTrue(TextUtils.startOfTag("</@"));
		// Satzzeichen vor dem Kleinerzeichen werden ignoriert
		assertTrue(TextUtils.startOfTag("?!</@"));

		assertFalse(TextUtils.startOfTag(""));
		assertFalse(TextUtils.startOfTag("<?!"));
		assertFalse(TextUtils.startOfTag("<<"));
		assertFalse(TextUtils.startOfTag("<<<"));
		assertFalse(TextUtils.startOfTag("?!<<"));
	}

	@Test public void endOfTag() {
		assertTrue(TextUtils.endOfTag(">"));
		assertTrue(TextUtils.endOfTag("/>"));
		assertTrue(TextUtils.endOfTag("\">"));
		assertTrue(TextUtils.endOfTag("\"/>"));
		// Satzzeichen nach dem Größerzeichen ignorieren
		assertTrue(TextUtils.endOfTag(">?!"));
		assertTrue(TextUtils.endOfTag(">..."));

		assertFalse(TextUtils.endOfTag(""));
		assertFalse(TextUtils.endOfTag("?!>"));
		assertFalse(TextUtils.endOfTag(">>"));
		assertFalse(TextUtils.endOfTag(">>>"));
		assertFalse(TextUtils.endOfTag(">>?!"));
	}

	@Test public void toUpperCase() {
		assertEquals("Groß", TextUtils.toUpperCase("Groß"));
		assertEquals("Groß", TextUtils.toUpperCase("groß"));
		assertEquals("Über", TextUtils.toUpperCase("über"));
		assertEquals("O’Hara", TextUtils.toUpperCase("o’hara"));
		assertEquals("O’", TextUtils.toUpperCase("o’"));
	}

	@Test public void testAddUpperCase() {
		Bag<String> dict = new HashBag<>();
		dict.add("klein", 10);
		dict.add("Groß", 5);
		dict.add("beides", 3);
		dict.add("Beides", 4);
		dict.add("über", 2);

		Bag<String> dict2 = TextUtils.addUpperCase(dict);
		assertThat(dict)
			.hasSize(24)
			.contains("klein", "Groß", "beides", "Beides", "über")
			.doesNotContain("Klein", "groß", "Über");
		assertThat(dict2)
			.hasSize(26)
			.contains("klein", "Klein", "Groß", "beides", "Beides", "über", "Über")
			.doesNotContain("groß");
		assertThat(dict2.getCount("klein")).isEqualTo(10);
		assertThat(dict2.getCount("Klein")).isEqualTo(1);
		assertThat(dict2.getCount("über")).isEqualTo(2);
		assertThat(dict2.getCount("Über")).isEqualTo(1);
	}

	@Test public void testMatches() {
		List<String> line = List.of("Ein", " ", "kurzer", " ", "Satz");
		Pattern pattern1 = Pattern.compile("E[i].*");
		Pattern pattern2 = Pattern.compile("^k[u]rzer$");
		Pattern pattern3 = Pattern.compile(".*tz");
		Pattern patternSpace = Pattern.compile(" ");
		assertTrue(TextUtils.matches(line, 0, pattern1));
		assertTrue(TextUtils.matches(line, 0, pattern1, patternSpace, pattern2, patternSpace, pattern3));
		assertTrue(TextUtils.matches(line, 2, pattern2, patternSpace, pattern3));
		assertTrue(TextUtils.matches(line, 4, pattern3));
		// Pattern passt nicht
		assertFalse(TextUtils.matches(line, 1, pattern1));
		assertFalse(TextUtils.matches(line, 0, pattern1, pattern2));
		assertFalse(TextUtils.matches(line, 2, pattern1));
		assertFalse(TextUtils.matches(line, 4, pattern1));
		// Anzahl der Tokens passt nicht
		assertFalse(TextUtils.matches(line, 0, pattern1, patternSpace, pattern2, patternSpace, pattern3, patternSpace));
		assertFalse(TextUtils.matches(line, 5, patternSpace));
	}

	@Test public void testReplace() {
		Pattern[] patterns = { Pattern.compile("…"), Pattern.compile(" "), Pattern.compile("!"), Pattern.compile("1") };
		checkReplace("Hierher … !1", 2, patterns, "…!!", "Hierher …!!", 1);
		// falsche Position
		checkReplace("Hierher … !1", 5, patterns, "…!!", "Hierher … !1", 0);
	}

	private void checkReplace(String line, int pos, Pattern[] patterns, String replacement, String expectedLine, int expectedCount) {
		List<String> words = TextUtils.split(line);
		int count = TextUtils.replace(words, pos, patterns, replacement);
		String actual = String.join("", words);
		assertEquals(expectedLine, actual);
		assertEquals("count", expectedCount, count);
	}
}
