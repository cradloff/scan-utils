package org.github.cradloff.scanutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import org.github.cradloff.scanutils.LineProcessor.Result;

/**
 * Ersetzt falsch geschriebene Worte durch die korrekte Version.
 * Die Klasse benötigt ein Wörterbuch (german.dic) und eine Datei mit Rechtschreib-Korrekturen (rechtschreibung.csv)
 * im Verzeichnis der Eingabedatei oder darüber.
 * Das Wörterbuch kann z.B. von hier bezogen werden: https://sourceforge.net/projects/germandict/
 * Die Rechtschreib-Korrekturen müssen jeweils den Suchbegriff und die Ersetzung in einer Zeile, durch Tab getrennt, enthalten.
 */
public class PreProcess {
	static class Parameter {
		private int level = 4;
		private List<File> inputs = new ArrayList<>();

		public static Parameter parse(String[] args) {
			Parameter param = new Parameter();
			for (String arg : args) {
				if (arg.startsWith("-")) {
					param.level = Integer.parseInt(arg.substring(1));
				} else {
					File input = FileAccess.checkExists(arg);
					if (input != null) {
						param.inputs.add(input);
					}
				}
			}

			return param;
		}

		public int getLevel() {
			return level;
		}

		public List<File> getInputs() {
			return inputs;
		}
	}

	/** leere Liste als Markierung für das Dateiende */
	private static final List<String> EOF = new ArrayList<>();
	/** Zeile mit einem Pagebreak */
	private static final List<String> PAGEBREAK = TextUtils.split("<@pagebreak/>");
	private Parameter params;

	public PreProcess(Parameter params) {
		this.params = params;
	}

	public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
		if (args.length < 1) {
			System.out.println("Aufruf: PreProcess [Optionen] <Dateiname(n)>");
			return;
		}

		Parameter params = Parameter.parse(args);
		// keine Dateien gefunden?
		if (params.getInputs().isEmpty()) {
			return;
		}

		// CSV-Datei mit Rechtschreib-Fehlern einlesen
		File basedir = FileAccess.basedir(params.getInputs().get(0));
		Map<String, String> map = FileAccess.readRechtschreibungCSV(basedir);
		// Wörterbuch einlesen
		Set<String> dict = FileAccess.readDict(basedir, "german.dic");
		Set<String> silben = FileAccess.readDict(basedir, "silben.dic");

		// alle Dateien verarbeiten
		File logfile = new File(basedir, "changes.log");
		try (PrintWriter log = new PrintWriter(logfile)) {
			for (File input : params.getInputs()) {
				preProcess(input, params, log, map, dict, silben);
			}
		}
	}

	private static void preProcess(File input, Parameter params, PrintWriter log, Map<String, String> map, Set<String> dict, Set<String> silben)
			throws IOException, InterruptedException, ExecutionException {
		long start = System.currentTimeMillis();
		System.out.println("Verarbeite Datei " + input.getPath());

		// Datei umbenennen
		File backup = FileAccess.roll(input);
		try (Reader in = new FileReader(backup);
				Writer out = new FileWriter(input);
				) {
			int count = new PreProcess(params).preProcess(in, out, log, map, dict, silben);

			System.out.printf("Anzahl ersetzter Wörter: %,d, Zeit: %,dms%n",
					count, (System.currentTimeMillis() - start));
		}
	}

	private List<String> nextLine(BufferedReader reader) throws IOException {
		String line = reader.readLine();
		List<String> tokens;
		if (line == null) {
			tokens = EOF;
		} else {
			// Zeile in Token zerlegen
			tokens = TextUtils.split(line);
		}

		return tokens;
	}

	public int preProcess(Reader in, Writer out, PrintWriter log, Map<String, String> map, Set<String> dict, Set<String> silben) throws IOException, InterruptedException, ExecutionException {
		// klein geschriebene Wörter auch in Groß-Schreibweise hinzufügen
		SortedSet<String> ciDict = new TreeSet<>(TextUtils.addUpperCase(dict));
		BufferedReader reader = new BufferedReader(in);
		List<String> line = nextLine(reader);
		List<String> prevLine = Arrays.asList("BOF");
		List<String> nextLine;
		int count = 0;
		// Pro Prozessor ein Thread
		int cpus = Runtime.getRuntime().availableProcessors();
		ExecutorService executor = Executors.newFixedThreadPool(cpus);
		List<Future<Result>> results = new ArrayList<>();
		do {
			/*
			 * Die Zeilen werden zunächst vorverarbeitet
			 */
			nextLine = nextLine(reader);

			// ggf. Pagebreak nach unten verschieben
			if (line.equals(PAGEBREAK) && ! prevLine.isEmpty() && nextLine.isEmpty() && nextLine != EOF) {
				line = nextLine;
				nextLine = PAGEBREAK;
			}

			// Leerzeilen überspringen
			if (line.isEmpty()) {
				results.add(LineProcessor.EMPTY_LINE);
			} else {
				// 7er etc. ersetzen
				count += replaceSeven(line);
				count += replaceSpecial(line);
				// Brüche ersetzen
				count += replaceFraction(line);
				// Worttrennung am Zeilenende zusammenfassen
				if (mergeLinebreak(line, nextLine)) {
					// ist die Folge-Zeile jetzt leer?
					if (nextLine.isEmpty()) {
						// Zeile überspringen
						nextLine = nextLine(reader);
					}
				}

				/*
				 * dann mit mehreren Threads verarbeitet
				 */
				results.add(executor.submit(new LineProcessor(params, line, map, ciDict, silben)));
			}

			prevLine = line;
			line = nextLine;
		}
		while (line != EOF);

		/*
		 * Jetzt die Zeilen ausgeben
		 */
		PrintWriter writer = new PrintWriter(out);
		for (Future<Result> future : results) {
			Result result = future.get();

			count += result.count();
			writer.println(result.line());
			for (String entry : result.log()) {
				log.println(entry);
			}
		}
		executor.shutdown();

		return count;
	}

	private static final Pattern SEVEN = Pattern.compile(".*\\D[72]$");
	private static final Pattern SEVEN_PLUS = Pattern.compile(".*\\D[72][ilt1]$");
	static int replaceSeven(List<String> line) {
		int count = 0;
		String nextWord = "";
		boolean tag = false;
		for (int i = line.size() - 1; i >= 0; i--) {
			// '7' am Wortende durch '?' ersetzen
			String word = line.get(i);
			if (TextUtils.endOfTag(word)) {
				tag = true;
			} else if (TextUtils.startOfTag(word)) {
				tag = false;
			} else if (tag) {
				continue;
			}

			if (SEVEN_PLUS.matcher(word).matches()) {
				line.set(i, word.substring(0, word.length() - 2));
				line.add(i + 1, "?!");
				count++;
			} else if (SEVEN.matcher(word).matches()) {
				line.set(i, word.substring(0, word.length() - 1));
				line.add(i + 1, "?");
				count++;
			}
			// ? gefolgt von i oder l
			else if (word.equals("?")
					&& (nextWord.equals("i") || nextWord.equals("l")
							|| nextWord.equals("t") || nextWord.equals("1"))) {
				line.remove(i + 1);
				line.set(i, "?!");
				count++;
			}
			nextWord = word;
		}

		return count;
	}

	static int replaceSpecial(List<String> line) {
		// Größer- und Kleinerzeichen außer in Tags ersetzen
		// Position des letzten Kleinerzeichens merken
		int count = 0;
		int lessThan = -1;
		for (int i = 0; i < line.size(); i++) {
			String token = line.get(i);
			if (TextUtils.startOfTag(token)) {
				// haben wir schon ein Kleinerzeichen gefunden?
				if (lessThan >= 0) {
					// das letzte Kleinerzeichen ersetzen
					line.set(lessThan, "ch");
					count++;
				}
				lessThan = i;
			} else if (TextUtils.endOfTag(token)) {
				// sind wir in einem Tag?
				if (lessThan >= 0) {
					lessThan = -1;
				} else if (i > 0) {
					// das letzte Größerzeichen ersetzen
					line.set(i, "ck");
					count++;
				}
			} else if ("{".equals(token)) {
				line.set(i, "sch");
				count++;
			}
		}
		if (lessThan >= 0) {
			// das letzte Kleinerzeichen ersetzen
			line.set(lessThan, "ch");
			count++;
		}

		// wenn Sonderzeichen ersetzt wurden, dann die aufeinanderfolgenden Texte zusammenfassen
		if (count > 0) {
			for (int i = line.size() - 2; i >= 0; i--) {
				if (TextUtils.isWord(line.get(i)) && TextUtils.isWord(line.get(i + 1))) {
					String s = line.remove(i + 1);
					line.set(i, line.get(i) + s);
				}
			}
		}

		return count;
	}

	// Map für Brüche: (Zähler, Nenner -> Bruch)
	private static final Map<String, Map<String, String>> FRACTIONS;
	static {
		FRACTIONS = new HashMap<>();
		Map<String, String> map = new HashMap<>();
		map.put("2", "½");
		map.put("3", "⅓");
		map.put("4", "¼");
		map.put("5", "⅕");
		map.put("6", "⅙");
		map.put("7", "⅐");
		map.put("8", "⅛");
		map.put("9", "⅑");
		map.put("10", "⅒");
		FRACTIONS.put("1", map);
		map = new HashMap<>();
		map.put("3", "⅔");
		map.put("5", "⅖");
		FRACTIONS.put("2", map);
		map = new HashMap<>();
		map.put("4", "¾");
		map.put("5", "⅗");
		map.put("8", "⅜");
		FRACTIONS.put("3", map);
		map = new HashMap<>();
		map.put("5", "⅘");
		FRACTIONS.put("4", map);
		map = new HashMap<>();
		map.put("6", "⅚");
		map.put("8", "⅝");
		FRACTIONS.put("5", map);
		map = new HashMap<>();
		map.put("8", "⅞");
		FRACTIONS.put("7", map);
	}
	static int replaceFraction(List<String> words) {
		int count = 0;
		for (int i = words.size() - 2; i > 0; i--) {
			if (words.get(i).equals("/")) {
				// Wörter vor und nach dem Slash in der Map suchen
				String zähler = words.get(i - 1);
				Map<String, String> map = FRACTIONS.get(zähler);
				if (map != null) {
					String nenner = words.get(i + 1);
					String bruch = map.get(nenner);
					if (bruch != null) {
						// Zeichenketten durch Bruch ersetzen
						words.set(i - 1, bruch);
						remove(words, i, 2);
						count++;
					}
				}
			}
		}
		return count;
	}

	static void remove(List<?> values, int index, int count) {
		for (int i = 0; i < count; i++) {
			values.remove(index);
		}
	}

	private boolean mergeLinebreak(List<String> line, List<String> nextLine) {
		boolean merged = false;
		// Worttrennung am Zeilenende?
		String token = line.get(line.size() - 1);
		if (TextUtils.endsWithDash(token) && Character.isAlphabetic(token.codePointAt(0))
				&& ! nextLine.isEmpty() && Character.isAlphabetic(nextLine.get(0).charAt(0))) {
			// Wörter zusammenfügen
			token += nextLine.remove(0);
			line.set(line.size() - 1, token);
			// Satzzeichen in die aktuelle Zeile übernehmen
			while (! nextLine.isEmpty() && TextUtils.isSatzzeichen(nextLine.get(0))) {
				line.add(nextLine.remove(0));
			}
			// Leerzeichen am Zeilenanfang entfernen.
			while (! nextLine.isEmpty() && " ".equals(nextLine.get(0))) {
				nextLine.remove(0);
			}
			merged = true;
		}

		return merged;
	}
}
