package org.github.cradloff.scanutils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

public class CheckQuotesTest {
	@Test public void checkQuotes() throws IOException {
		// ok
		checkCheckQuotes("Satz ohne Quotes.\n", 0);
		checkCheckQuotes("Satz mit »Quotes«.\n", 0);
		checkCheckQuotes("Satz mit „Quotes“.\n", 0);
		checkCheckQuotes("»Satz mit „Zeilenumbrüchen“\nund Quotes.«\n", 0);
		checkCheckQuotes("»Satz mit »verschachtelten« Quotes«.\n", 0);
		checkCheckQuotes("»Satz mit „verschachtelten“ Quotes«.\n", 0);

		// zu wenige schließende Quotes
		checkCheckQuotes("Satz mit »Quotes.\n", 1);
		checkCheckQuotes("Satz mit „Quotes.\n", 1);
		checkCheckQuotes("„Satz mit »Quotes.\n", 2);
		checkCheckQuotes("»Satz mit „Quotes.\n", 2);
		checkCheckQuotes("»Satz mit „Zeilenumbrüchen“\nund Quotes.\n", 1);

		// zu wenige öffnende Quotes
		checkCheckQuotes("Satz mit Quotes«.\n", 1);
		checkCheckQuotes("Satz mit Quotes“.\n", 1);
		checkCheckQuotes("Satz« mit Quotes.“\n", 2);
		checkCheckQuotes("Satz“ mit Quotes.«\n", 2);
		checkCheckQuotes("Satz mit „Zeilenumbrüchen“\nund Quotes.«\n", 1);
	}

	private void checkCheckQuotes(String lines, int expectedErrors) throws IOException {
		try (StringReader in = new StringReader(lines);
				PrintWriter out = new PrintWriter(new ByteArrayOutputStream())) {
			int errors = CheckQuotes.checkQuotes(in, out);
			assertEquals(expectedErrors, errors, "Fehler");
		}
	}
}
