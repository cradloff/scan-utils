package org.github.cradloff.scanutils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.TreeBag;

/**
 * Vergleicht die Wörter einer Datei mit einem Wörterbuch. Nicht gefundene
 * Wörter werden in einer Datei ausgegeben.
 */
public class SpellCheck {
	public static void main(String... args) throws IOException {
		long start = System.currentTimeMillis();
		if (args.length != 1) {
			System.out.println("Aufruf: SpellCheck <Dateiname>");

			return;
		}

		File file = new File(args[0]);
		if (! file.exists()) {
			System.out.printf("Datei %s nicht gefunden!%n", args[0]);

			return;
		}

		// Wörterbuch einlesen
		Set<String> dict = FileAccess.readDict(file, "german.dic");
		dict = TextUtils.addUpperCase(dict);
		// Wörter aus Rechtschreibhilfe hinzufügen
		Map<String, String> rechtschreibung = FileAccess.readRechtschreibungCSV(file);
		dict.addAll(rechtschreibung.values());

		// Datei prüfen
		File spellcheck = new File(FileAccess.basedir(file), "spellcheck.txt");
		System.out.printf("überprüfe Datei %s%n", file);
		System.out.printf("schreibe nicht gefundene Wörter nach %s%n", spellcheck);
		try (Writer fw = new FileWriter(spellcheck);
				PrintWriter out = new PrintWriter(fw)) {
			// alle Wörter einlesen
			Bag<String> words = new TreeBag<>();
			CreateDictionary.readWords(file, words);
			// Wörter aus Wörterbuch entfernen
			for (String word : dict) {
				words.remove(word);
			}

			// und ausgeben
			for (String word : words.uniqueSet()) {
				out.printf("%s\t%,d%n", word, words.getCount(word));
			}

			System.out.printf("Anzahl nicht gefundener Wörter: %,d, Zeit: %,dms%n",
					words.uniqueSet().size(), (System.currentTimeMillis() - start));
		}
	}
}
