package org.github.cradloff.scanutils;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;
import org.github.cradloff.scanutils.CheckCase.Satzanfang;
import org.junit.Test;

public class CheckCaseTest {

	@Test public void testFixCase() {
		checkFixCase("", "Zu Anfang und zu klein.", "Zu Anfang und zu klein.", 0);
		checkFixCase("", "zu anfang Und Zu klein.", "Zu Anfang und zu klein.", 4);
		checkFixCase("", "ende. Zu Anfang Und Zu klein.", "Ende. Zu Anfang und zu klein.", 3);
		checkFixCase("", "»am Anfang.«", "»Am Anfang.«", 1);
		checkFixCase("den dunklen Strich", "Und den singenden Vogel", "und den singenden Vogel", 1);
		checkFixCase("den dunklen Strich.", "Und den singenden Vogel", "Und den singenden Vogel", 0);
		checkFixCase("", "über und über.", "Über und über.", 1);
		checkFixCase("", "Komm’ zu mir.", "Komm’ zu mir.", 0);
		checkFixCase("", "»Danke …« Und er setzte sich.", "»Danke …« Und er setzte sich.", 0);
	}

	private Bag<String> dict = new HashBag<>(Arrays.asList("am", "Anfang", "Ende", "Wasser", "und", "zu"));
	private static Collection<String> abkürzungen = Arrays.asList("Nr");
	private void checkFixCase(String lastLine, String line, String expected, int expectedCount) {
		List<String> lastWords = TextUtils.split(lastLine);
		List<String> words = TextUtils.split(line);
		int count = CheckCase.fixCase(lastWords, words, CheckCase.removeAmbigous(dict), abkürzungen);
		String actual = String.join("", words);
		assertEquals(expected, actual);
		assertEquals("count", expectedCount, count);
	}

	@Test public void testSatzanfang() {
		checkSatzanfang(Satzanfang.JA, "");
		checkSatzanfang(Satzanfang.JA, "Anfang");
		checkSatzanfang(Satzanfang.NEIN, "Am Anfang");
		checkSatzanfang(Satzanfang.NEIN, "Am - Anfang");
		checkSatzanfang(Satzanfang.NEIN, "Am - \"Anfang\"");
		checkSatzanfang(Satzanfang.JA, "Ende. Anfang");
		checkSatzanfang(Satzanfang.JA, "Ende! Anfang");
		checkSatzanfang(Satzanfang.JA, "Ende!! Anfang");
		checkSatzanfang(Satzanfang.JA, "Ende? Anfang");
		checkSatzanfang(Satzanfang.JA, "Ende?! Anfang");
		checkSatzanfang(Satzanfang.WEISS_NICHT, "Ende: Anfang");
		checkSatzanfang(Satzanfang.JA, "Ende. - Anfang");
		checkSatzanfang(Satzanfang.JA, "Ende. — Anfang");
		checkSatzanfang(Satzanfang.JA, "Ende —! Anfang");
		checkSatzanfang(Satzanfang.JA, "<h1>Anfang");
		checkSatzanfang(Satzanfang.JA, "Ende.", "Anfang");
		checkSatzanfang(Satzanfang.JA, "Ende. -", "Anfang");
		checkSatzanfang(Satzanfang.NEIN, "Am", "Anfang");
		checkSatzanfang(Satzanfang.NEIN, "Komm’ her");
		checkSatzanfang(Satzanfang.WEISS_NICHT, "»Vorsicht!« rief");
		checkSatzanfang(Satzanfang.WEISS_NICHT, "»Vorsicht!!« rief");
		checkSatzanfang(Satzanfang.WEISS_NICHT, "»Wirklich?« fragte");
		checkSatzanfang(Satzanfang.WEISS_NICHT, "»Wirklich?!« fragte");
		checkSatzanfang(Satzanfang.JA, "»Schluß.« Danach");
		checkSatzanfang(Satzanfang.JA, "»Schluß«. Danach");
		checkSatzanfang(Satzanfang.NEIN, "»Ich gehe,« sagte");
		checkSatzanfang(Satzanfang.NEIN, "Nun — zunächst");
		checkSatzanfang(Satzanfang.NEIN, "1. April");
		checkSatzanfang(Satzanfang.NEIN, "Nr. Fünf");
		checkSatzanfang(Satzanfang.WEISS_NICHT, "Nun —« zunächst");
		checkSatzanfang(Satzanfang.WEISS_NICHT, "ich sage: vielleicht");
		checkSatzanfang(Satzanfang.JA, "Schluß...! Danach");
		checkSatzanfang(Satzanfang.JA, "Schluß...!! Danach");
		checkSatzanfang(Satzanfang.JA, "Schluß...? Danach");
		checkSatzanfang(Satzanfang.JA, "Schluß...?! Danach");
		checkSatzanfang(Satzanfang.WEISS_NICHT, "ich sage... vielleicht");
		checkSatzanfang(Satzanfang.JA, "Schluß…! Danach");
		checkSatzanfang(Satzanfang.JA, "Schluß…!! Danach");
		checkSatzanfang(Satzanfang.JA, "Schluß…? Danach");
		checkSatzanfang(Satzanfang.JA, "Schluß…?! Danach");
		checkSatzanfang(Satzanfang.WEISS_NICHT, "ich sage… vielleicht");
		checkSatzanfang(Satzanfang.WEISS_NICHT, "ich sage…« vielleicht");
		// Tags werden ignoriert
		checkSatzanfang(Satzanfang.JA, "Ende. <em>Anfang");
		checkSatzanfang(Satzanfang.NEIN, "Am <em>Anfang");
		checkSatzanfang(Satzanfang.WEISS_NICHT, "»Vorsicht!« <em>rief");
	}

	/** prüft, ob das letzte Wort der Zeile am Satzanfang steht */
	private void checkSatzanfang(Satzanfang expected, String line) {
		checkSatzanfang(expected, "", line);
	}

	/** prüft, ob das letzte Wort der Zeile am Satzanfang steht */
	private void checkSatzanfang(Satzanfang expected, String lastLine, String line) {
		List<String> lastWords = TextUtils.split(lastLine);
		List<String> words = TextUtils.split(line);
		assertEquals(expected, CheckCase.satzanfang(lastWords, words, words.size() - 1, abkürzungen));
	}

	@Test public void testFixPunkt() {
		checkFixPunkt("Das Satzende,", "der Anfang", "Das Satzende,", 0);
		checkFixPunkt("Das Satzende,", "", "Das Satzende.", 1);
		checkFixPunkt("Das Satzende,«", "der Anfang", "Das Satzende,«", 0);
		checkFixPunkt("Das Satzende,«", "", "Das Satzende.«", 1);
		checkFixPunkt("Das Satzende", "und der Anfang", "Das Satzende", 0);
		checkFixPunkt("Das Satzende", "", "Das Satzende.", 1);
		checkFixPunkt("Das Satzende, —", "und der Anfang", "Das Satzende, —", 0);
		checkFixPunkt("Das Satzende, —", "", "Das Satzende. —", 1);
		checkFixPunkt("Das Satzende«", "und der Anfang", "Das Satzende«", 0);
		checkFixPunkt("Das Satzende«", "", "Das Satzende.«", 1);
		checkFixPunkt("Das Satzende,« —", "und der Anfang", "Das Satzende,« —", 0);
		checkFixPunkt("Das Satzende,« —", "", "Das Satzende.« —", 1);
		checkFixPunkt("<h1>Kapitel 1</h1>", "", "<h1>Kapitel 1.</h1>", 1);
		checkFixPunkt("<h2>Kapitel 2</h2>", "", "<h2>Kapitel 2.</h2>", 1);
		checkFixPunkt("<h3>Kapitel 3,</h3>", "", "<h3>Kapitel 3.</h3>", 1);
	}

	private void checkFixPunkt(String line, String nextLine, String expected, int expectedCount) {
		List<String> words = TextUtils.split(line);
		int count = CheckCase.fixPunkt(words, nextLine);
		StringBuilder actual = new StringBuilder();
		for (String word : words) {
			actual.append(word);
		}
		assertEquals(expected, actual.toString());
		assertEquals(expectedCount, count);
	}

	@Test public void testCheckCase() throws IOException {
		checkCheckCase("Zu Lande Und Zu wasser\n", "Zu Lande und zu Wasser.\n");
		checkCheckCase("am anfang. und Am ende,\n", "Am Anfang. Und am Ende.\n");
		checkCheckCase("Ein langer\nAbsatz mit\nvielen Zeilenumbrüchen\n", "Ein langer\nAbsatz mit\nvielen Zeilenumbrüchen.\n");
		// Tags werden ignoriert, dazwischen wird ersetzt
		checkCheckCase("<h1 am='anfang'>am anfang</h1>", "<h1 am='anfang'>Am Anfang.</h1>");
		checkCheckCase("<@ende am='anfang'>am anfang</@ende>", "<@ende am='anfang'>Am Anfang</@ende>");
	}

	private void checkCheckCase(String line, String expected) throws IOException {
		CheckCase cc = new CheckCase();
		StringReader in = new StringReader(line);
		StringWriter out = new StringWriter();
		cc.checkCase(in, out, dict, abkürzungen);
		String actual = out.toString();
		Assert.assertLinesEqual(expected, actual);
	}
}
