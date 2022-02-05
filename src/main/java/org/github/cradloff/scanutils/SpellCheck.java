package org.github.cradloff.scanutils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.TreeBag;

/**
 * Vergleicht die Wörter einer Datei mit einem Wörterbuch. Nicht gefundene
 * Wörter werden in einer Datei ausgegeben.
 */
public class SpellCheck {
	public static void main(String... args) throws IOException {
		long start = System.currentTimeMillis();
		if (args.length < 1) {
			System.out.println("Aufruf: SpellCheck [-<threshold>] [-f] <Dateiname(n)>");

			return;
		}

		int threshold = 1;
		boolean fromFile = false;
		List<String> filenames = new ArrayList<>();
		for (String param : args) {
			if (param.matches("-\\d+")) {
				threshold = Integer.parseInt(param.substring(1));
			} else if (param.equals("-f")) {
				fromFile = true;
			} else if (fromFile) {
				filenames.addAll(FileAccess.readLines(new File(param)));
			} else {
				filenames.add(param);
			}
		}

		List<File> inputs = FileAccess.checkExists(filenames.toArray(new String[0]));
		// keine Dateien gefunden?
		if (inputs.isEmpty()) {
			System.out.println("keine Dateien gefunden");
			return;
		}

		// Wörterbuch einlesen
		File basedir = FileAccess.basedir(new File("."));
		Bag<String> dict = FileAccess.readDict(basedir, "german.dic");
		dict = TextUtils.addUpperCase(dict);

		// Dateien prüfen
		File spellcheck = new File(basedir, "spellcheck.log");
		System.out.printf("schreibe nicht gefundene Wörter nach %s%n", spellcheck);
		try (Writer fw = new FileWriter(spellcheck);
				PrintWriter out = new PrintWriter(fw)) {
			Bag<String> words = new TreeBag<>(new CaseInsensitiveComparator());
			for (File input : inputs) {
				System.out.printf("überprüfe Datei %s%n", input);
				// alle Wörter einlesen
				CreateDictionary.readWords(input, words);
			}

			// Wörter aus Wörterbuch entfernen
			for (String word : dict) {
				words.remove(word);
			}

			// und ausgeben
			int count = 0;
			for (String word : words.uniqueSet()) {
				if (words.getCount(word) >= threshold) {
					count++;
					out.printf("%s\t%,d%n", word, words.getCount(word));
				}
			}

			System.out.printf("Anzahl nicht gefundener Wörter: %,d, Zeit: %,dms%n",
					count, (System.currentTimeMillis() - start));
		}
	}
}
