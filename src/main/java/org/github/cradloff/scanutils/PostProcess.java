package org.github.cradloff.scanutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;

/**
 * Fasst Silben-getrennte Wörter am Zeilenende zusammen. Bindestriche im Text werden entfernt, wenn vor
 * und nach dem Bindestrich ein Kleinbuchstabe kommt.
 */
public class PostProcess
{
	public static void main(String[] args) throws IOException {
		long start = System.currentTimeMillis();
		if (args.length != 1) {
			System.out.println("Aufruf: PostProcess <Dateiname>");
			return;
		}

		File input = new File(args[0]);
		if (! input.exists()) {
			System.out.println("Datei " + args[0] + " nicht gefunden!");
			return;
		}

		System.out.println("Verarbeite Datei " + args[0]);

		// Datei umbenennen
		String backup = args[0].substring(0, args[0].lastIndexOf('.')) + ".bak";
		input.renameTo(new File(backup));
		try (Reader in = new FileReader(new File(backup));
				Writer out = new FileWriter(input);) {
			new PostProcess().postProcess(in, out);
		}

		System.out.printf("Zeit: %,dms%n", (System.currentTimeMillis() - start));
	}

	public void postProcess(Reader in, Writer out) throws IOException {
		BufferedReader reader = new BufferedReader(in);
		PrintWriter writer = new PrintWriter(out);
		String second = reader.readLine();
		do {
			String first = second;
			second = reader.readLine();
			if (second != null) {
				first = first.trim();
				second = second.trim();
				// Bindestrich am Zeilen-Ende?
				if (first.matches(".*\\p{IsAlphabetic}[-—]$")) {
					String hyphen = first.substring(first.length() - 1);
					int index = first.lastIndexOf(' ');
					String suffix;
					if (index < 0) {
						index = 0;
					}
					suffix = first.substring(index, first.length() - 1);
					first = first.substring(0, index);
					index = second.indexOf(' ');
					String prefix;
					if (index < 0) {
						prefix = second;
						second = reader.readLine();
					} else {
						prefix = second.substring(0, index);
						second = second.substring(index + 1);
					}
					if (prefix.matches("\\p{javaLowerCase}.*")) {
						first = first + suffix + prefix;
					} else {
						first = first + suffix + hyphen + prefix;
					}
				}
			}

			// Bindestriche im Text entfernen
			first = first.replaceAll("(\\p{javaLowerCase})[-—](\\p{javaLowerCase})", "$1$2");
			first = TextUtils.satzzeichenErsetzen(first);

			writer.println(first);
		}
		while (second != null);
	}
}
