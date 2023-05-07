package org.github.cradloff.scanutils;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

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
				assertEquals(expectedLine, actualLine, "Line " + lineNumber);
				lineNumber++;
			}

			if (expectedLinesReader.readLine() != null) {
				fail("Actual string does not contain all expected lines\n"
						+ "expected: \"" + expectedString + "\"\n"
						+ "actual: \"" + actualString + "\"");
			}
		} catch (IOException e) {
			fail(e.getMessage());
		} finally {
			try {
				expectedLinesReader.close();
			} catch (IOException e) {
				fail(e.getMessage());
			}
			try {
				actualLinesReader.close();
			} catch (IOException e) {
				fail(e.getMessage());
			}
		}
	}
}
