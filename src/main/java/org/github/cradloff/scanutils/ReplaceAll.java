package org.github.cradloff.scanutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Map;

import org.github.cradloff.scanutils.PreProcess.Parameter;
import org.github.cradloff.scanutils.PrepareText.TextReader;

/**
 * Ersetzt in einer Datei alle Vorkommen von bestimmten Suchausdrücken durch passende Ersetzungen.
 * Die Suchausdrücke und Ersetzungen werden aus der Datei replacements.csv eingelesen.
 * Aufruf: <code>ReplaceAll [Dateiname(en)]</code>
 */
public class ReplaceAll {
	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.out.println("Aufruf: ReplaceAll <Dateiname(n)>");
			return;
		}
		
		Parameter params = Parameter.parse(args);
		File replacementsFile = FileAccess.find(params.getInputs().get(0), "replacements.csv");
		if (replacementsFile == null) {
			System.out.println("Datei replacements.csv ist nicht vorhanden!");
			return;
		}
		
		Map<String, String> replacements = FileAccess.readCSV(replacementsFile, "\t");
		for (File input : params.getInputs()) {
			System.out.println("Verarbeite Datei " + input.getPath());
			File backup = FileAccess.roll(input);
			try (Reader in = new FileReader(backup);
					PrintWriter out = new PrintWriter(input);
					PrepareText.TextReader reader = new TextReader(new BufferedReader(in));) {
				String line;
				while ((line = reader.readLine()) != null) {
					line = PrepareText.replaceOnce(line, replacements);
					out.println(line);
				}
			}
		}
	}
}
