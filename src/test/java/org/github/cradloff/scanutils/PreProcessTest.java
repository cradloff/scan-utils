package org.github.cradloff.scanutils;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class PreProcessTest {
	@Test public void testSplit() {
		check("Alle meine Entchen", "Alle", " ", "meine", " ", "Entchen");
		check(" Alle  meine  Entchen ", " ", "Alle", "  ", "meine", "  ", "Entchen", " ");
		check("Alle mei-ne Ent-chen", "Alle", " ", "mei-ne", " ", "Ent-chen");
		check("Alle, meine 'Entchen'", "Alle", ",", " ", "meine", " ", "'", "Entchen", "'");
	}

	private void check(String line, String... wordsExcpected) {
		List<String> words = PreProcess.split(line);
		assertEquals(Arrays.asList(wordsExcpected), words);
	}

	@Test public void removeDashes() {
		assertEquals("Wort", PreProcess.removeDashes("Wort"));
		assertEquals("Wort", PreProcess.removeDashes("Wo-rt"));
		assertEquals("Wort", PreProcess.removeDashes("W-o--r-t"));
		assertEquals("Wort-", PreProcess.removeDashes("Wort-"));
	}
}
