package org.github.cradloff.scanutils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class TextUtilsTest {
	@Test public void testSplit() {
		checkSplit("Alle meine Entchen", "Alle", " ", "meine", " ", "Entchen");
		checkSplit(" Alle  meine  Entchen ", " ", "Alle", "  ", "meine", "  ", "Entchen", " ");
		checkSplit("Alle mei-ne Ent—chen", "Alle", " ", "mei-ne", " ", "Ent—chen");
		checkSplit("Alle, meine 'Entchen’", "Alle", ",", " ", "meine", " ", "'Entchen’");
		checkSplit("Alle meine Ent-", "Alle", " ", "meine", " ", "Ent-");
		checkSplit(">>Alle ,,meine,, Entchen?!<<", ">>", "Alle", " ", ",,", "meine", ",,", " ", "Entchen", "?!<<");
		checkSplit("auf- und abwärts", "auf", "-", " ", "und", " ", "abwärts");
		checkSplit("Insel-Cafee", "Insel", "-", "Cafee");
		checkSplit("wollen wir7", "wollen", " ", "wir7");
		checkSplit("wollen wir?", "wollen", " ", "wir", "?");
		checkSplit("wollen wir?!", "wollen", " ", "wir", "?!");
		checkSplit("er war bleiern\\-schwerfällig ...", "er", " ", "war", " ", "bleiern\\-schwerfällig", " ", "...");
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
		assertEquals("»Hier — dort —«", TextUtils.satzzeichenErsetzen(">>Hier -- dort -—<<"));
	}

	@Test public void isWord() {
		assertTrue(TextUtils.isWord("Wort"));
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

	@Test public void testAddUpperCase() {
		Set<String> dict = new HashSet<>();
		dict.add("klein");
		dict.add("Groß");
		dict.add("beides");
		dict.add("Beides");

		Set<String> dict2 = TextUtils.addUpperCase(dict);
		assertEquals(4, dict.size());
		assertEquals(5, dict2.size());
		assertTrue(dict2.containsAll(dict));
		assertTrue(dict2.contains("Klein"));
		assertFalse(dict2.contains("groß"));
	}
}
