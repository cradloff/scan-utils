package org.github.cradloff.scanutils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.List;
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
		if (args.length < 1) {
			System.out.println("Aufruf: SpellCheck <Dateiname(n)>");

			return;
		}

		List<File> inputs = FileAccess.checkExists(args);
		// keine Dateien gefunden?
		if (inputs.isEmpty()) {
			return;
		}

		// Wörterbuch einlesen
		File basedir = FileAccess.basedir(inputs.get(0));
		Set<String> dict = FileAccess.readDict(basedir, "german.dic");
		dict = TextUtils.addUpperCase(dict);
		// Wörter aus Rechtschreibhilfe hinzufügen
		Map<String, String> rechtschreibung = FileAccess.readRechtschreibungCSV(basedir);
		dict.addAll(rechtschreibung.values());

		// Dateien prüfen
		File spellcheck = new File(basedir, "spellcheck.log");
		System.out.printf("schreibe nicht gefundene Wörter nach %s%n", spellcheck);
		try (Writer fw = new FileWriter(spellcheck);
				PrintWriter out = new PrintWriter(fw)) {
			Bag<String> words = new TreeBag<>();
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
			for (String word : words.uniqueSet()) {
				out.printf("%s\t%,d%n", word, words.getCount(word));
			}

			System.out.printf("Anzahl nicht gefundener Wörter: %,d, Zeit: %,dms%n",
					words.uniqueSet().size(), (System.currentTimeMillis() - start));
		}
	}
}
