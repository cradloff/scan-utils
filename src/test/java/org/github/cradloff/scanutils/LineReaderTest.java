package org.github.cradloff.scanutils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

public class LineReaderTest {
	private LineReader lineReader;

	@Before public void setUp() throws IOException {
		StringReader reader = new StringReader("eins\nzwei\ndrei\nvier\nfünf\n");
		lineReader = new LineReader(reader, 1, 2);
	}

	@Test public void lineReader() throws IOException {
		assertTrue(lineReader.readLine());
		assertSame(LineReader.EOF, lineReader.prev());
		assertEquals(Arrays.asList("eins"), lineReader.current());
		assertEquals(Arrays.asList("zwei"), lineReader.next());
		assertEquals(Arrays.asList("drei"), lineReader.next(2));
		assertTrue(lineReader.hasNext());
		assertTrue(lineReader.hasNext(2));

		assertTrue(lineReader.readLine());
		assertEquals(Arrays.asList("eins"), lineReader.prev());
		assertEquals(Arrays.asList("zwei"), lineReader.current());
		assertEquals(Arrays.asList("drei"), lineReader.next());
		assertEquals(Arrays.asList("vier"), lineReader.next(2));
		assertTrue(lineReader.hasNext());
		assertTrue(lineReader.hasNext(2));

		assertTrue(lineReader.readLine());
		assertEquals(Arrays.asList("zwei"), lineReader.prev());
		assertEquals(Arrays.asList("drei"), lineReader.current());
		assertEquals(Arrays.asList("vier"), lineReader.next());
		assertEquals(Arrays.asList("fünf"), lineReader.next(2));
		assertTrue(lineReader.hasNext());
		assertTrue(lineReader.hasNext(2));

		assertTrue(lineReader.readLine());
		assertEquals(Arrays.asList("drei"), lineReader.prev());
		assertEquals(Arrays.asList("vier"), lineReader.current());
		assertEquals(Arrays.asList("fünf"), lineReader.next());
		assertSame(LineReader.EOF, lineReader.next(2));
		assertTrue(lineReader.hasNext());
		assertFalse(lineReader.hasNext(2));

		assertTrue(lineReader.readLine());
		assertEquals(Arrays.asList("vier"), lineReader.prev());
		assertEquals(Arrays.asList("fünf"), lineReader.current());
		assertSame(LineReader.EOF, lineReader.next());
		assertSame(LineReader.EOF, lineReader.next(2));
		assertFalse(lineReader.hasNext());
		assertFalse(lineReader.hasNext(2));

		assertFalse(lineReader.readLine());
	}
}
