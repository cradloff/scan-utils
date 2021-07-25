package org.github.cradloff.scanutils;

import java.io.File;
import java.io.IOException;

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.TreeBag;

public class UpdateDictionary {
	public static void main(String... args) throws IOException {
		long start = System.currentTimeMillis();
		if (args.length > 1) {
			System.out.println("Aufruf: UpdateDictionary <Verzeichnis>");

			return;
		}

		String dirName = args.length == 0 ? "." : args[0];
		File baseDir = new File(dirName);
		if (! baseDir.exists()) {
			System.out.printf("Verzeichnis %s ist nicht vorhanden!%n", dirName);

			return;
		}
		if (! baseDir.isDirectory()) {
			System.out.printf("%s ist kein Verzeichnis!%n", dirName);

			return;
		}

		// vorhandenes Wörterbuch einlesen
		File file = new File(baseDir, "german.dic");
		Bag<String> german = FileAccess.readDict(file);

		// nach Markdown-Dateien suchen
		Bag<String> dictionary = new TreeBag<>();
		CreateDictionary.addWords(baseDir, dictionary);

		// Häufigkeiten aktualisieren
		german = updateStatistics(german, dictionary);

		FileAccess.writeDictionary(file, german);
		System.out.printf("Wörterbuch %s aktualisiert (%,d Wörter, %,d ms)%n", file.getPath(), german.uniqueSet().size(),
				(System.currentTimeMillis() - start));
	}

	private static Bag<String> updateStatistics(Bag<String> german, Bag<String> dictionary) {
		Bag<String> result = new TreeBag<>(new CaseInsensitiveComparator());
		for (String word : german.uniqueSet()) {
			result.add(word, dictionary.getCount(word));
		}

		return result;
	}

}
