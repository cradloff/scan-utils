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
		checkSplit("Alle mei-ne Ent-chen", "Alle", " ", "mei-ne", " ", "Ent-chen");
		checkSplit("Alle, meine 'Entchen'", "Alle", ",", " ", "meine", " ", "'", "Entchen", "'");
		checkSplit("wollen wir7", "wollen", " ", "wir7");
		checkSplit("wollen wir?", "wollen", " ", "wir", "?");
		checkSplit("wollen wir?!", "wollen", " ", "wir", "?", "!");
		checkSplit("er war bleiern\\-schwerfällig ...", "er", " ", "war", " ", "bleiern\\-schwerfällig", " ", ".", ".", ".");
	}

	private void checkSplit(String line, String... wordsExcpected) {
		List<String> words = PreProcess.split(line);
		assertEquals(Arrays.asList(wordsExcpected), words);
	}

	@Test public void removeDashes() {
		assertEquals("Wort", PreProcess.removeDashes("Wort"));
		assertEquals("Wort", PreProcess.removeDashes("Wo-rt"));
		assertEquals("Wort", PreProcess.removeDashes("W-o--r-t"));
		assertEquals("Wort-", PreProcess.removeDashes("Wort-"));
		assertEquals("bleiern\\-schwerfällig", PreProcess.removeDashes("bleiern\\-schwerfällig"));
	}

	@Test public void testSeven() {
		checkSeven("Wort ohne 7.", "Wort ohne 7.");
		checkSeven("Absatz 7l und 7i", "Absatz 7l und 7i");
		checkSeven("Wort7 mit7l sieben7i", "Wort? mit?! sieben?!");
		checkSeven("57 Wörter mit 7 Silben7", "57 Wörter mit 7 Silben?");
	}

	private void checkSeven(String line, String expected) {
		List<String> words = PreProcess.split(line);
		words = PreProcess.replaceSeven(words);
		String actual = String.join("", words);
		assertEquals(expected, actual);
	}

	@Test public void testToLower() {
		checkToLower("", "Zu Anfang und zu klein.", "Zu Anfang und zu klein.");
		checkToLower("", "Zu Anfang Und Zu klein.", "Zu Anfang und zu klein.");
		checkToLower("", "Ende. Zu Anfang Und Zu klein.", "Ende. Zu Anfang und zu klein.");
		checkToLower("den dunklen Strich", "Und den singenden Vogel", "und den singenden Vogel");

	}

	private void checkToLower(String lastLine, String line, String expected) {
		List<String> lastWords = PreProcess.split(lastLine);
		List<String> words = PreProcess.split(line);
		words = PreProcess.toLower(lastWords, words);
		String actual = String.join("", words);
		assertEquals(expected, actual);
	}

	@Test public void testSatzAnfang() {
		checkSatzAnfang(true, "", "");
		checkSatzAnfang(true, "", "Anfang");
		checkSatzAnfang(false, "", "Am Anfang");
		checkSatzAnfang(false, "", "Am - Anfang");
		checkSatzAnfang(false, "", "Am - \"Anfang\"");
		checkSatzAnfang(true, "", "Ende. Anfang");
		checkSatzAnfang(true, "", "Ende! Anfang");
		checkSatzAnfang(true, "", "Ende? Anfang");
		checkSatzAnfang(true, "", "Ende?! Anfang");
		checkSatzAnfang(true, "", "Ende: Anfang");
		checkSatzAnfang(true, "", "Ende. - Anfang");
		checkSatzAnfang(true, "", "Ende. - Anfang");
		checkSatzAnfang(true, "", "<h1>Anfang");
		checkSatzAnfang(true, "Ende.", "Anfang");
		checkSatzAnfang(true, "Ende. -", "Anfang");
		checkSatzAnfang(false, "Am", "Anfang");
	}

	private void checkSatzAnfang(boolean expected, String lastLine, String line) {
		List<String> lastWords = PreProcess.split(lastLine);
		List<String> words = PreProcess.split(line);
		assertEquals(expected, PreProcess.satzAnfang(lastWords, words, words.size() - 1));
	}

	@Test public void testPreProcess() throws IOException {
		Map<String, String> spellcheck = new HashMap<>();
		spellcheck.put("Aiie", "Alle");
		spellcheck.put("miene", "meine");
		Set<String> dict = new HashSet<>();
		dict.add("Alle");
		dict.add("Entchen");
		dict.add("schwerfällig");
		dict.add("zu");
		checkPreProcess("Alle meine Entchen\n", "Alle meine Entchen\n", dict, spellcheck);
		// meine ist nicht im Dictionary
		checkPreProcess("Alle mei-ne Ent-chen\n", "Alle mei-ne Entchen\n", dict, spellcheck);
		checkPreProcess("Aiie miene Entchen\n", "Alle meine Entchen\n", dict, spellcheck);
		checkPreProcess("Alle7 meine7i Entchen7l\n", "Alle? meine?! Entchen?!\n", dict, spellcheck);
		checkPreProcess("Zu Wasser und Zu Lande\n", "Zu Wasser und zu Lande\n", dict, spellcheck);
		checkPreProcess("Alle miene Ent-chen Zu Wasser-teich!\n", "Alle meine Entchen zu Wasser-teich!\n", dict, spellcheck);
		checkPreProcess("er war bleiern\\\\-schwerfällig ...\n", "er war bleiern\\\\-schwerfällig ...\n", dict, spellcheck);
	}

	private void checkPreProcess(String line, String expected, Set<String> dict, Map<String, String> spellcheck) throws IOException {
		PreProcess pp = new PreProcess();
		StringReader in = new StringReader(line);
		StringWriter out = new StringWriter();
		pp.preProcess(in, out, spellcheck, dict);
		String actual = out.toString();
		assertEquals(expected, actual);
	}
}
