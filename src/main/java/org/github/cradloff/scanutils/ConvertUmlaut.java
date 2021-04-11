package org.github.cradloff.scanutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.List;

public class ConvertUmlaut {
	public static void main(String[] args) throws IOException {
		long start = System.currentTimeMillis();
		if (args.length != 1) {
			System.out.println("Aufruf ConvertUmlaut <Verzeichnis>");
			return;
		}

		File dir = new File(args[0]);
		if (! dir.isDirectory()) {
			System.out.println(args[0] + " ist kein Verzeichnis!");
			return;
		}

		convertDirectory(dir);
		System.out.printf("Zeit: %,dms%n", (System.currentTimeMillis() - start));
	}

	private static void convertDirectory(File dir) throws IOException {
		File files[] = dir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				// alle Dateien mit Endung .dic, .md umstellen
				String filename = pathname.getName();
				return pathname.isDirectory() && ! filename.startsWith(".")
						|| pathname.isFile() && (filename.endsWith(".md") || filename.endsWith(".dic"));
			}
		});
		for (File f : files) {
			if (f.isFile()) {
				convertUmlaut(f);
			} else {
				convertDirectory(f);
			}
		}
	}

	private static void convertUmlaut(File input) throws IOException {
		long start = System.currentTimeMillis();
		System.out.println("Verarbeite Datei " + input.getPath());

		// Datei umbenennen
		File backup = FileAccess.roll(input);
		try (Reader in = new FileReader(backup);
				PrintWriter out = new PrintWriter(input);
				) {
			boolean modified = convertUmlaut(in, out);
			if (! modified) {
				input.delete();
				backup.renameTo(input);
			}

			System.out.printf("Zeit: %,dms%n", (System.currentTimeMillis() - start));
		}
	}

	private static boolean convertUmlaut(Reader in, PrintWriter out) throws IOException {
		boolean modified[] = { false };
		try (BufferedReader reader = new BufferedReader(in);) {
			reader.lines().forEach(line -> {
				// Zeile in Token zerlegen
				List<String> s = TextUtils.split(line);
				modified[0] = convertUmlaut(s) | modified[0];

				for (String token : s) {
					out.write(token);
				}
				out.println();
			});
		}
		return modified[0];
	}

	private static boolean convertUmlaut(List<String> line) {
		boolean modified = false;
		for (int i = 0; i < line.size(); i++) {
			String token = line.get(i);
			if (token.startsWith("Ae")) {
				modified = true;
				token = token.replaceFirst("Ae", "Ä");
			} else if (token.startsWith("Oe")) {
				modified = true;
				token = token.replaceFirst("Oe", "Ö");
			} else if (token.startsWith("Ue")) {
				modified = true;
				token = token.replaceFirst("Ue", "Ü");
			}
			line.set(i, token);
		}
		return modified;
	}
}