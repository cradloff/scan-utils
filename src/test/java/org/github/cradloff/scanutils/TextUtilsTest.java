package org.github.cradloff.scanutils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class TextUtilsTest {
	@Test public void testSatzzeichenErsetzen() {
		assertEquals("", TextUtils.satzzeichenErsetzen(""));
		assertEquals("»Hier. Da ist’s.«", TextUtils.satzzeichenErsetzen(",,Hier· Da ist's.«"));
		assertEquals("»Hier — dort —«", TextUtils.satzzeichenErsetzen(">>Hier -- dort -—<<"));
		assertEquals("»Hier... — dort —«", TextUtils.satzzeichenErsetzen(".,Hier... -- dort -—<<"));
		assertEquals("»Hier... — dort —«", TextUtils.satzzeichenErsetzen(",.Hier... -- dort -—<<"));
		assertEquals("»Hier... — dort —«", TextUtils.satzzeichenErsetzen("..Hier... -- dort -—<<"));
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
