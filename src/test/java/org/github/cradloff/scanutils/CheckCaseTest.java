package org.github.cradloff.scanutils;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class CheckCaseTest {

	@Test public void testToLower() {
		checkToLower("", "Zu Anfang und zu klein.", "Zu Anfang und zu klein.", 0);
		checkToLower("", "Zu anfang Und Zu klein.", "Zu Anfang und zu klein.", 3);
		checkToLower("", "ende. Zu Anfang Und Zu klein.", "Ende. Zu Anfang und zu klein.", 3);
		checkToLower("den dunklen Strich", "Und den singenden Vogel", "und den singenden Vogel", 1);
		checkToLower("den dunklen Strich.", "Und den singenden Vogel", "Und den singenden Vogel", 0);
	}

	private Set<String> dict = new HashSet<>(Arrays.asList("Anfang", "Ende", "Wasser", "und", "zu"));
	private void checkToLower(String lastLine, String line, String expected, int expectedCount) {
		List<String> lastWords = TextUtils.split(lastLine);
		List<String> words = TextUtils.split(line);
		int count = CheckCase.fixCase(lastWords, words, dict);
		String actual = String.join("", words);
		assertEquals(expected, actual);
		assertEquals("count", expectedCount, count);
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
		List<String> lastWords = TextUtils.split(lastLine);
		List<String> words = TextUtils.split(line);
		assertEquals(expected, CheckCase.satzAnfang(lastWords, words, words.size() - 1));
	}

	@Test public void testCheckCase() throws IOException {
		checkCheckCase("Zu Lande Und Zu wasser\n", "Zu Lande und zu Wasser\n");
	}

	private void checkCheckCase(String line, String expected) throws IOException {
		CheckCase cc = new CheckCase();
		StringReader in = new StringReader(line);
		StringWriter out = new StringWriter();
		cc.checkCase(in, out, dict);
		String actual = out.toString();
		assertEquals(expected, actual);
	}
}
