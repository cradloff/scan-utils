package org.github.cradloff.scanutils;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import org.junit.Test;

/**
 * Unit test for simple PostProcess.
 */
public class PostProcessTest {
	@Test public void testDehypen() throws IOException {
		String input = "Text ohne Bindestriche.\n";
		assertEquals(input, dehyphen(input));

		input = "Text mit Binde-strich.\n";
		String expected = "Text mit Bindestrich.\n";
		assertEquals(expected, dehyphen(input));
		// m-breiter Bindestrich
		input = "Text mit Binde—strich.\n";
		assertEquals(expected, dehyphen(input));

		input = "Text mit Binde-\nstrich am Ende.\n";
		expected = "Text mit Bindestrich\nam Ende.\n";
		assertEquals(expected, dehyphen(input));
		input = "Text mit Binde—\nstrich am Ende.\n";
		assertEquals(expected, dehyphen(input));

		input = "Text mit Binde-\nStrich am Ende.\n";
		expected = "Text mit Binde-Strich\nam Ende.\n";
		assertEquals(expected, dehyphen(input));
		input = "Text mit Binde—\nStrich am Ende.\n";
		expected = "Text mit Binde—Strich\nam Ende.\n";
		assertEquals(expected, dehyphen(input));

		input = "Text mit —\nGedankenstrich.\n";
		assertEquals(input, dehyphen(input));

		input = "Text mit -—- Gedankenstrich.";
		expected = "Text mit — Gedankenstrich.\n";
		assertEquals(expected, dehyphen(input));

		input = "Text mit -- Gedankenstrich.";
		expected = "Text mit — Gedankenstrich.\n";
		assertEquals(expected, dehyphen(input));

		input = "Text mit Binde-\nstrich.\n";
		expected = "Text mit Bindestrich.\n";
		assertEquals(expected, dehyphen(input));
		input = "Text mit Binde—\nstrich.\n";
		assertEquals(expected, dehyphen(input));

		input = "Binde-\nstrich im Text.\n";
		expected = "Bindestrich\nim Text.\n";
		assertEquals(expected, dehyphen(input));
		input = "Binde—\nstrich im Text.\n";
		assertEquals(expected, dehyphen(input));

		input = "Binde-\nStrich im Text.\n";
		expected = "Binde-Strich\nim Text.\n";
		assertEquals(expected, dehyphen(input));
		input = "Binde—\nStrich im Text.\n";
		expected = "Binde—Strich\nim Text.\n";
		assertEquals(expected, dehyphen(input));

		input = "Blü-\ncherstraße";
		expected = "Blücherstraße\n";
		assertEquals(expected, dehyphen(input));
	}

	private String dehyphen(String input) throws IOException {
		PostProcess dehyphen = new PostProcess();
		Reader in = new StringReader(input);
		Writer out = new StringWriter();
		dehyphen.postProcess(in, out);

		return out.toString();
	}
}
