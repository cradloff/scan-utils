package org.github.cradloff.scanutils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

public class Assert {
	public static void assertLinesEqual(String expectedString, String actualString) {
		BufferedReader expectedLinesReader = new BufferedReader(new StringReader(expectedString));
		BufferedReader actualLinesReader = new BufferedReader(new StringReader(actualString));

		try {
			int lineNumber = 0;

			String actualLine;
			while ((actualLine = actualLinesReader.readLine()) != null) {
				String expectedLine = expectedLinesReader.readLine();
				org.junit.Assert.assertEquals("Line " + lineNumber, expectedLine, actualLine);
				lineNumber++;
			}

			if (expectedLinesReader.readLine() != null) {
				org.junit.Assert.fail("Actual string does not contain all expected lines");
			}
		} catch (IOException e) {
			org.junit.Assert.fail(e.getMessage());
		} finally {
			try {
				expectedLinesReader.close();
			} catch (IOException e) {
				org.junit.Assert.fail(e.getMessage());
			}
			try {
				actualLinesReader.close();
			} catch (IOException e) {
				org.junit.Assert.fail(e.getMessage());
			}
		}
	}
}
