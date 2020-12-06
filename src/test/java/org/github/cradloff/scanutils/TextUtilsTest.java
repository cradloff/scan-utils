package org.github.cradloff.scanutils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.Assertions;
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
		checkSplit("er war bleiern\\-schwerfällig ...", "er", " ", "war", " ", "bleiern\\-schwerfällig", " ", "...");
		checkSplit("Und -—- Dann", "Und", " ", "-—-", " ", "Dann");
		checkSplit("Und dann -—-", "Und", " ", "dann", " ", "-—-");
		checkSplit("Mode£lauto und -schiff", "Mode£lauto", " ", "und", " ", "-schiff");
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
		assertTrue(TextUtils.isSatzzeichen(","));
		assertTrue(TextUtils.isSatzzeichen(";"));
		assertTrue(TextUtils.isSatzzeichen(":"));
		assertTrue(TextUtils.isSatzzeichen("-"));
		assertTrue(TextUtils.isSatzzeichen("»"));
		assertTrue(TextUtils.isSatzzeichen("«"));
		assertTrue(TextUtils.isSatzzeichen(".,;:-»«"));
		assertFalse(TextUtils.isSatzzeichen("a"));
		assertFalse(TextUtils.isSatzzeichen("»a«"));
		// Leerzeichen gelten nicht als Satzzeichen
		assertFalse(TextUtils.isSatzzeichen(".,;: -»«"));
	}

	@Test public void testSatzzeichenErsetzen() {
		assertEquals("", TextUtils.satzzeichenErsetzen(""));
		assertEquals("Hier. Da ist’s.", TextUtils.satzzeichenErsetzen("Hier· Da ist's."));
		assertEquals("»Hier — dort — wo — anders —«", TextUtils.satzzeichenErsetzen(">>Hier -- dort -— wo == anders -=<<"));
		assertEquals("Apostroph unten, Komma", TextUtils.satzzeichenErsetzen("Apostroph unten‚ Komma"));
	}

	@Test public void isWord() {
		assertTrue(TextUtils.isWord("Wort"));
		assertTrue(TextUtils.isWord("ist’s"));
		assertFalse(TextUtils.isWord("«"));
		assertFalse(TextUtils.isWord(" "));
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

	@Test public void startOfTag() {
		assertTrue(TextUtils.startOfTag("<"));
		assertTrue(TextUtils.startOfTag("</"));
		assertTrue(TextUtils.startOfTag("<@"));
		assertTrue(TextUtils.startOfTag("</@"));
		// Satzzeichen vor dem Kleinerzeichen werden ignoriert
		assertTrue(TextUtils.startOfTag("?!</@"));

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

		assertFalse(TextUtils.endOfTag("?!>"));
		assertFalse(TextUtils.endOfTag(">>"));
		assertFalse(TextUtils.endOfTag(">>>"));
		assertFalse(TextUtils.endOfTag(">>?!"));
	}

	@Test public void toUpperCase() {
		assertEquals("Groß", TextUtils.toUpperCase("Groß"));
		assertEquals("Groß", TextUtils.toUpperCase("groß"));
		assertEquals("Ueber", TextUtils.toUpperCase("über"));
	}

	@Test public void testAddUpperCase() {
		Set<String> dict = new HashSet<>();
		dict.add("klein");
		dict.add("Groß");
		dict.add("beides");
		dict.add("Beides");
		// Umlaute werden aufgelöst
		dict.add("über");

		Set<String> dict2 = TextUtils.addUpperCase(dict);
		Assertions.assertThat(dict)
			.hasSize(5)
			.contains("klein", "Groß", "beides", "Beides", "über");
		Assertions.assertThat(dict2)
			.hasSize(7)
			.contains("klein", "Klein", "Groß", "beides", "Beides", "über", "Ueber");

	}
}
