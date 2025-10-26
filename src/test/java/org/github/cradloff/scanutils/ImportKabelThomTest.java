package org.github.cradloff.scanutils;

import static org.github.cradloff.scanutils.ImportKabelThom.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ImportKabelThomTest {
	@Test
	void testChangeQuotes() {
		assertEquals("»Anführungszeichen«", changeQuotes("„Anführungszeichen“"));
		assertEquals("schmaler — Strich", changeQuotes("schmaler – Strich"));
	}
	
	@Test
	void testChangeSpecial() {
		assertEquals("das ist ja …", changeSpecial("das ist ja. . ."));
		assertEquals("das ist ja …", changeSpecial("das ist ja. ."));
		assertEquals("das ist ja …", changeSpecial("das ist ja..."));
		assertEquals("das ist ja …", changeSpecial("das ist ja.."));
		assertEquals("Sie glauben daß …", changeSpecial("Sie glauben daß..."));
		assertEquals("Nö …", changeSpecial("Nö..."));
		assertEquals("»… und dann", changeSpecial("» ... und dann"));
		assertEquals("Trotzdem — irgendwie", changeSpecial("Trotzdem - irgendwie"));
		assertEquals("Trotzdem — irgendwie", changeSpecial("Trotzdem -- irgendwie"));
		assertEquals("Trotzdem — irgendwie", changeSpecial("Trotzdem --- irgendwie"));
		assertEquals("Max’ Ansicht", changeSpecial("Max\" Ansicht"));
		assertEquals("Max’ Ansicht", changeSpecial("Max' Ansicht"));
		assertEquals("Max’ Ansicht", changeSpecial("Max´ Ansicht"));
	}
	
	@Test
	void testEscapeDigits() {
		assertEquals("1\\. April", escapeDigits("1. April"));
		assertEquals("Am 1. April", escapeDigits("Am 1. April"));
	}
	
	@Test
	void testNonBreakingSpaces() {
		assertEquals("G.&nbsp;m.&nbsp;b.&nbsp;H.", nonBreakingSpaces("G. m. b. H."));
		assertEquals("100&nbsp;000 Mark", nonBreakingSpaces("100 000 Mark"));
	}
	
	@Test
	void testReplaceUmlaut() {
		assertEquals("Überall Öl Ärger", replaceUmlaut("Ueberall Oel Aerger"));
	}
}
