package org.github.cradloff.scanutils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TextUtilsTest {
	@Test public void testSatzzeichenErsetzen() {
		assertEquals("", TextUtils.satzzeichenErsetzen(""));
		assertEquals("»Hier. Da ist’s.«", TextUtils.satzzeichenErsetzen(",,Hier· Da ist's.«"));
		assertEquals("»Hier — dort —«", TextUtils.satzzeichenErsetzen(">>Hier -- dort -—<<"));
	}
}
