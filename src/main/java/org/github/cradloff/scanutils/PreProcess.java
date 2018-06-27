package org.github.cradloff.scanutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ersetzt falsch geschriebene Worte durch die korrekte Version.
 */
public class PreProcess
{
	public static void main(String[] args) throws IOException {
		long start = System.currentTimeMillis();
		if (args.length != 1) {
			System.out.println("Aufruf: PreProcess <Dateiname>");
			return;
		}

		File input = new File(args[0]);
		if (! input.exists()) {
			System.out.println("Datei " + args[0] + " nicht gefunden!");
			return;
		}

		System.out.println("Verarbeite Datei " + args[0]);

		// CSV-Datei mit Rechtschreib-Fehlern einlesen
		Map<String, String> map = readCSV(input.getAbsoluteFile().getParentFile());
		Set<String> dict = readDict();

		// Datei umbenennen
		String backup = args[0].substring(0, args[0].lastIndexOf('.')) + ".bak";
		input.renameTo(new File(backup));
		try (Reader in = new FileReader(new File(backup));
				Writer out = new FileWriter(input);) {
			int count = new PreProcess().preProcess(in, out, map, dict);

			System.out.printf("Anzahl ersetzter Wörter: %,d, Zeit: %,dms%n",
					count, (System.currentTimeMillis() - start));
		}
	}

	private static Set<String> readDict() throws IOException {
		Set<String> dict = new HashSet<>();
		try (InputStream in = PreProcess.class.getResourceAsStream("/german/german.dic");
				Reader reader = new InputStreamReader(in, Charset.forName("ISO-8859-15"));
				BufferedReader br = new BufferedReader(reader)) {
			for (String line = br.readLine(); line != null; line = br.readLine()) {
				dict.add(line);
			}
		}
		return dict;
	}

	private static Map<String, String> readCSV(File dir) throws IOException {
		if (dir == null) {
			throw new FileNotFoundException();
		}

		// Die Datei rekursiv nach oben suchen
		File file = new File(dir, "rechtschreibung.csv");
		if (file.exists()) {
			Map<String, String> map = new HashMap<>();
			try (FileReader fr = new FileReader(file);
					BufferedReader br = new BufferedReader(fr);) {
				for (String line = br.readLine(); line != null; line = br.readLine()) {
					line = line.trim();
					if (! line.isEmpty()) {
						String[] s = line.split("\t");
						map.put(s[0], s[1]);
					}
				}
			}

			return map;
		}

		return readCSV(dir.getParentFile());
	}

	public int preProcess(Reader in, Writer out, Map<String, String> map, Set<String> dict) throws IOException {
		BufferedReader reader = new BufferedReader(in);
		PrintWriter writer = new PrintWriter(out);
		String line = reader.readLine();
		int count = 0;
		do {
			List<String> s = split(line);
			for (int i = 0; i < s.size(); i++) {
				String t = s.get(i);
				String word = t.replaceAll("-", "");
				if (map.containsKey(t)) {
					count++;
					writer.print(map.get(t));
				} else if (map.containsKey(word)) {
					count++;
					writer.print(map.get(word));
				} else if (dict.contains(word) && i != s.size() - 1) {
					writer.print(word);
				} else {
					writer.print(t);
				}
			}

			writer.println();
			line = reader.readLine();
		}
		while (line != null);

		return count;
	}

	private enum State { WHITESPACE, WORD, OTHER }
	public static List<String> split(String line) {
		StringBuilder sb = new StringBuilder();
		List<String> result = new ArrayList<>();
		State state = State.WHITESPACE;
		for (int i = 0; i < line.length(); i++) {
			char ch = line.charAt(i);
			State newState;
			if (Character.isWhitespace(ch)) {
				newState = State.WHITESPACE;
			}
			// Bindestrich wird wie Buchstabe behandelt
			else if (Character.isLetter(ch) || ch == '-') {
				newState = State.WORD;
			} else {
				newState = State.OTHER;
			}
			if (state != newState && i > 0) {
				result.add(sb.toString());
				sb.setLength(0);
			}
			sb.append(ch);
			state = newState;
		}
		if (sb.length() > 0) {
			result.add(sb.toString());
		}

		return result;
	}
}
