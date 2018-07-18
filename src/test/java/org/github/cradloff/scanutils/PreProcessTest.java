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
		assertEquals("bleiern\\-schwer", PreProcess.removeDashes("bleiern\\-schwer"));
	}

	@Test public void testSeven() {
		checkSeven("Wort ohne 7.", "Wort ohne 7.");
		checkSeven("Absatz 7l und 7i", "Absatz 7l und 7i");
		checkSeven("Wort7 mit7l sieben7i", "Wort? mit?! sieben?!");
	}

	private void checkSeven(String line, String expected) {
		List<String> words = PreProcess.split(line);
		words = PreProcess.replaceSeven(words);
		String actual = String.join("", words);
		assertEquals(expected, actual);
	}

	@Test public void testZu() {
		checkZu("Zu Anfang zu klein.", "Zu Anfang zu klein.");
		checkZu("Zu Anfang Zu klein.", "Zu Anfang zu klein.");
		checkZu("Ah. Zu Anfang Zu klein.", "Ah. Zu Anfang zu klein.");
	}

	private void checkZu(String line, String expected) {
		List<String> words = PreProcess.split(line);
		words = PreProcess.replaceZu(words);
		String actual = String.join("", words);
		assertEquals(expected, actual);
	}

	@Test public void testPreProcess() throws IOException {
		Map<String, String> spellcheck = new HashMap<>();
		spellcheck.put("Aiie", "Alle");
		spellcheck.put("miene", "meine");
		Set<String> dict = new HashSet<>();
		dict.add("Alle");
		dict.add("Entchen");
		checkPreProcess("Alle meine Entchen\n", "Alle meine Entchen\n", dict, spellcheck);
		// meine ist nicht im Dictionary
		checkPreProcess("Alle mei-ne Ent-chen\n", "Alle mei-ne Entchen\n", dict, spellcheck);
		checkPreProcess("Aiie miene Entchen\n", "Alle meine Entchen\n", dict, spellcheck);
		checkPreProcess("Alle7 meine7i Entchen7l\n", "Alle? meine?! Entchen?!\n", dict, spellcheck);
		checkPreProcess("Zu Wasser und Zu Lande\n", "Zu Wasser und zu Lande\n", dict, spellcheck);
		checkPreProcess("Alle miene Ent\\-chen Zu Wasser-teich!\n", "Alle meine Ent\\-chen zu Wasser-teich!\n", dict, spellcheck);
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
