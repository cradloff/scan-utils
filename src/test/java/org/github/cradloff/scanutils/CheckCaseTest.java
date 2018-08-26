package org.github.cradloff.scanutils;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class CheckCaseTest {

	@Test public void testToLower() {
		checkToLower("", "Zu Anfang und zu klein.", "Zu Anfang und zu klein.", 0);
		checkToLower("", "Zu Anfang Und Zu klein.", "Zu Anfang und zu klein.", 2);
		checkToLower("", "Ende. Zu Anfang Und Zu klein.", "Ende. Zu Anfang und zu klein.", 2);
		checkToLower("den dunklen Strich", "Und den singenden Vogel", "und den singenden Vogel", 1);
		checkToLower("den dunklen Strich.", "Und den singenden Vogel", "Und den singenden Vogel", 0);
	}

	private Set<String> lower = new HashSet<>(Arrays.asList("und", "zu"));
	private void checkToLower(String lastLine, String line, String expected, int expectedCount) {
		List<String> lastWords = TextUtils.split(lastLine);
		List<String> words = TextUtils.split(line);
		int count = CheckCase.toLower(lastWords, words, lower);
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
}
