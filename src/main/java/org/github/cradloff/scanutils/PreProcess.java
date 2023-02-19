package org.github.cradloff.scanutils;

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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.SortedBag;
import org.apache.commons.collections4.bag.TreeBag;
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
		private int level = 6;
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

	/** Zeile mit einem Pagebreak */
	private static final List<String> PAGEBREAK = TextUtils.split("<@pagebreak/>");
	private static final Pattern[] PATTERN_UEBERSCHRIFT = {
			Pattern.compile("<"),
			Pattern.compile("h\\d"),
			Pattern.compile(">"),
	};
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
		Bag<String> dict = FileAccess.readDict(basedir, "german.dic");
		Bag<String> silben = FileAccess.readDict(basedir, "silben.dic");

		// alle Dateien verarbeiten
		File logfile = new File(basedir, "changes.log");
		try (PrintWriter log = new PrintWriter(logfile)) {
			for (File input : params.getInputs()) {
				preProcess(input, params, log, map, dict, silben);
			}
		}
	}

	private static void preProcess(File input, Parameter params, PrintWriter log, Map<String, String> map, Bag<String> dict, Bag<String> silben)
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

	public int preProcess(Reader in, Writer out, PrintWriter log, Map<String, String> map, Bag<String> dict, Bag<String> silben) throws IOException, InterruptedException, ExecutionException {
		// klein geschriebene Wörter auch in Groß-Schreibweise hinzufügen
		SortedBag<String> ciDict = new TreeBag<>(TextUtils.addUpperCase(dict));
		SortedSet<String> invDict = TextUtils.inverse(dict);
		SortedSet<String> treeView = new TreeSet<>(ciDict.uniqueSet());
		LineReader reader = new LineReader(in, 1, 2);
		int count = 0;
		// Pro Prozessor ein Thread
		int cpus = Runtime.getRuntime().availableProcessors();
		ExecutorService executor = Executors.newFixedThreadPool(cpus);
		List<Future<Result>> results = new ArrayList<>();
		while (reader.readLine()) {
			/*
			 * Die Zeilen werden zunächst vorverarbeitet
			 */
			List<String> line = reader.current();

			// ggf. Pagebreak nach unten verschieben
			if (line.equals(PAGEBREAK)) {
				if (! reader.prev().isEmpty() && reader.hasNext() && reader.next().isEmpty()) {
					reader.swap(0, 1);
					line = reader.current();
				}
				// kommt nach dem Pagebreak eine Überschrift, muss eine Leerzeile eingefügt werden
				else if (reader.hasNext() && TextUtils.matches(reader.next(), 0, PATTERN_UEBERSCHRIFT)) {
					reader.add(List.of());
				}
			}

			// Leerzeilen überspringen
			if (line.isEmpty()) {
				results.add(LineProcessor.EMPTY_LINE);
			} else {
				count += satzzeichenErsetzen(line, ciDict);
				count += replaceSpecial(line);
				// Brüche ersetzen
				count += replaceFraction(line);
				// Leerzeichen entfernen/einfügen
				count += checkWhitespace(line);
				// Worttrennung am Zeilenende zusammenfassen
				if (mergeLinebreak(reader, ciDict)) {
					// ist die Folge-Zeile jetzt leer?
					if (reader.next().isEmpty()) {
						// Zeile überspringen
						reader.skip(1);
					}
				}

				/*
				 * dann mit mehreren Threads verarbeitet
				 */
				results.add(executor.submit(new LineProcessor(params, line, map, ciDict, invDict, treeView, silben)));
			}
		}

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
	private static final Pattern SEVEN_PLUS = Pattern.compile(".*\\D[72k][ilt1]$");
	private static final Pattern SPACE = Pattern.compile(" ");
	private static final Pattern ILT1 = Pattern.compile("^[ilt1]$");
	private static final Pattern[] PATTERN_FRAGEZEICHEN_AUSRUFEZEICHEN = {
			Pattern.compile("\\?"), Pattern.compile("[ilt1]") };
	private static final Pattern[][] PATTERN_FRAGEZEICHEN_AUSRUFEZEICHEN_QUOTE = {
			{ Pattern.compile("\\?"), SPACE, Pattern.compile("![»«]") },
			{ Pattern.compile("\\?"), SPACE, Pattern.compile("1"), Pattern.compile("[»«]") },
	};
	private static final Pattern[][] PATTERN_AUSRUFEZEICHEN_AUSRUFEZEICHEN_QUOTE = {
			{ Pattern.compile("[1!]"), Pattern.compile("1"), Pattern.compile("[»«]") },
			{ Pattern.compile("[1!]"), SPACE, Pattern.compile("![»«]") },
			{ Pattern.compile("[1!]"), SPACE, Pattern.compile("1"), Pattern.compile("[»«]") },
	};
	private static final Pattern[][] PATTERN_HELLIP_AUSRUFEZEICHEN = {
			{ Pattern.compile("…"), Pattern.compile("1") },
			{ Pattern.compile("…"), SPACE, Pattern.compile("1") },
	};
	private static final Pattern[][] PATTERN_HELLIP_FRAGEZEICHEN_AUSRUFEZEICHEN = {
			{ Pattern.compile("…"), SPACE, Pattern.compile("\\?!") },
			{ Pattern.compile("…\\?"), ILT1 },
			{ Pattern.compile("…"), SPACE, Pattern.compile("\\?"), ILT1 },
	};
	private static final Pattern[][] PATTERN_HELLIP_AUSRUFEZEICHEN_AUSRUFEZEICHEN = {
			{ Pattern.compile("…"), Pattern.compile("[!1]{2}") },
			{ Pattern.compile("…"), SPACE, Pattern.compile("[!1]{2}") },
			{ Pattern.compile("…!"), Pattern.compile("1") },
			{ Pattern.compile("…"), SPACE, Pattern.compile("!"), Pattern.compile("1") },
	};
	static int satzzeichenErsetzen(List<String> line, SortedBag<String> ciDict) {
		int count = 0;
		boolean tag = false;
		if (TextUtils.endsWith(line, " ", "-")) {
			line.set(line.size() - 1, "—");
			count++;
		}
		if (TextUtils.startsWith(line, "-", " ")) {
			line.set(0, "—");
			count++;
		}
		for (int i = line.size() - 1; i >= 0; i--) {
			// '7' am Wortende durch '?' ersetzen
			String word = line.get(i);
			// keine Ersetzungen in Html-Tags
			if (TextUtils.endOfTag(word)) {
				tag = true;
			} else if (TextUtils.startOfTag(word)) {
				tag = false;
			} else if (tag) {
				continue;
			}
			// bekannte Wörter in Ruhe lassen
			else if (ciDict.contains(word)) {
				continue;
			}

			if (SEVEN_PLUS.matcher(word).matches() && ! word.endsWith("kt")) {
				line.set(i, word.substring(0, word.length() - 2));
				line.add(i + 1, "?!");
				count++;
			} else if (SEVEN.matcher(word).matches()) {
				line.set(i, word.substring(0, word.length() - 1));
				// "7 1", "2 i", ...
				if (TextUtils.matches(line, i + 1, SPACE, ILT1)) {
					line.set(i + 1, "?!");
					line.remove(i + 2);
				} else {
					line.add(i + 1, "?");
				}
				count++;
			}
			// ? gefolgt von i oder l
			count += TextUtils.replace(line, i, PATTERN_FRAGEZEICHEN_AUSRUFEZEICHEN, "?!");
			// ? 1« durch ?!« ersetzen
			for (Pattern[] patterns : PATTERN_FRAGEZEICHEN_AUSRUFEZEICHEN_QUOTE) {
				count += TextUtils.replace(line, i, patterns, "?!«");
			}
			// ! 1« durch !!« ersetzen
			for (Pattern[] patterns : PATTERN_AUSRUFEZEICHEN_AUSRUFEZEICHEN_QUOTE) {
				count += TextUtils.replace(line, i, patterns, "!!«");
			}
			// ... 1 durch ...! ersetzen
			for (Pattern[] pattern : PATTERN_HELLIP_AUSRUFEZEICHEN) {
				count += TextUtils.replace(line, i, pattern, "…!");
			}
			// ... 11 durch ...!! ersetzen
			for (Pattern[] pattern : PATTERN_HELLIP_AUSRUFEZEICHEN_AUSRUFEZEICHEN) {
				count += TextUtils.replace(line, i, pattern, "…!!");
			}
			// ... ?! durch ...?! ersetzen
			for (Pattern[] pattern : PATTERN_HELLIP_FRAGEZEICHEN_AUSRUFEZEICHEN) {
				count += TextUtils.replace(line, i, pattern, "…?!");
			}
			// einzelnen Bindestrich durch breiten Strich ersetzen
			if (TextUtils.regionMatches(line, i - 1, " ", "-", " ")) {
				line.set(i, "—");
				count++;
			}
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
			if (TextUtils.endOfTag(token)) {
				// sind wir in einem Tag?
				if (lessThan >= 0) {
					lessThan = -1;
				} else if (i > 0) {
					// das aktuelle Größerzeichen ersetzen
					line.set(i, token.replace(">", "ck"));
					count++;
				}
			}
			if (TextUtils.startOfTag(token)) {
				// haben wir schon ein Kleinerzeichen gefunden?
				if (lessThan >= 0) {
					// das letzte Kleinerzeichen ersetzen
					line.set(lessThan, line.get(lessThan).replace("<", "ch"));
					count++;
				}
				lessThan = i;
			}
			if ("{".equals(token)) {
				line.set(i, "sch");
				count++;
			}
		}
		if (lessThan >= 0) {
			// das letzte Kleinerzeichen ersetzen
			line.set(lessThan, line.get(lessThan).replace("<", "ch"));
			count++;
		}

		// wenn Sonderzeichen ersetzt wurden, dann die Zeile neu splitten
		if (count > 0) {
			StringBuilder sb = new StringBuilder(100);
			for (int i = 0; i < line.size(); i++) {
				sb.append(line.get(i));
			}
			List<String> newLine = TextUtils.split(sb);
			line.clear();
			line.addAll(newLine);
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
				String pre = zähler.substring(0, zähler.length() - 1);
				zähler = zähler.substring(zähler.length() - 1);
				Map<String, String> map = FRACTIONS.get(zähler);
				if (map != null) {
					String nenner = words.get(i + 1);
					for (Entry<String, String> entry : map.entrySet()) {
						if (nenner.equals(entry.getKey())) {
							words.set(i - 1, pre);
							words.set(i, entry.getValue());
							words.set(i + 1, nenner.substring(entry.getKey().length()));
							count++;
						}
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

	/** Satzzeichen, die immer nach einem Wort stehen */
	private static final String SATZZEICHEN_NACH_WORT = ".,;:!?";
	enum State { WHITESPACE, OTHER, SATZZEICHEN }
	static int checkWhitespace(List<String> line) {
		int count = 0;
		State state = State.OTHER;
		for (int i = 0; i < line.size(); i++) {
			String token = line.get(i);
			if (TextUtils.isWhitespace(token)) {
				state = State.WHITESPACE;
			} else if (isSatzzeichenNachWort(token)) {
				if (state == State.WHITESPACE) {
					line.remove(--i);
					count++;
				}
				state = State.SATZZEICHEN;
			} else {
				state = State.OTHER;
			}
		}

		return count;
	}
	private static boolean isSatzzeichenNachWort(String token) {
		return SATZZEICHEN_NACH_WORT.indexOf(token.charAt(0)) >= 0
				// als zweites Zeichen ist nur ! oder « zulässig
				&& (token.length() == 1 || token.charAt(1) == '!' || token.charAt(1) == '«');
	}

	/** Schmierzeichen am Zeilenanfang */
	private static final Set<String> SCHMIERZEICHEN_ZEILENBEGINN = new HashSet<>(Arrays.asList(",", "»", "«"));
	static boolean mergeLinebreak(LineReader reader, Bag<String> ciDict) {
		List<String> line = reader.current();
		if (line.isEmpty() || ! reader.hasNext()) {
			return false;
		}

		List<String> nextLine = reader.next();
		// die Folgezeile ist ein Pagebreak?
		if (nextLine.equals(PAGEBREAK) && reader.hasNext(2)) {
			nextLine = reader.next(2);
		}

		if (nextLine.isEmpty()) {
			return false;
		}

		// Worttrennung am Zeilenende?
		boolean merged = false;
		String token = line.get(line.size() - 1);
		if (TextUtils.endsWithDash(token) && startsWithLetter(token)) {
			// die Folge-Zeile beginnt mit einem Buchstaben oder einem Schmierzeichen?
			if (! nextLine.isEmpty()
					&& (startsWithLetter(nextLine.get(0))
							|| SCHMIERZEICHEN_ZEILENBEGINN.contains(nextLine.get(0)))) {
				// Bindestrich entfernen
				line.set(line.size() - 1, token.substring(0, token.length() - 1));
				merge(line, nextLine);
				merged = true;
			}
		}
		// ggf. steht ein Buchstabe am Zeilenende für einen Bindestrich
		else if (endsWithDashLike(token) && startsWithLetter(token)
				&& ! nextLine.isEmpty()
				&& (startsWithLetter(nextLine.get(0)))) {
			// hier sind die Hürden höher: nur wenn beide Wörter zusammengeschrieben im
			// Wörterbuch vorkommen, werden sie vereinigt
			String token1 = token.substring(0, token.length() - 1);
			String token2 = nextLine.get(0);
			if (! ciDict.contains(token)
					&& ciDict.contains(token1 + token2)) {
				line.set(line.size() - 1, token1);
				merge(line, nextLine);
				merged = true;
			}
		}
		// manchmal kommt ein Quote statt einem Bindestrich
		else if ((TextUtils.endsWithOneOf(line, "«", "»", "«-", "»-")) && line.size() > 1) {
			String token1 = line.get(line.size() - 2);
			String token2 = nextLine.get(0);
			if (ciDict.contains(token1 + token2)) {
				line.remove(line.size() - 1);
				merge(line, nextLine);
				merged = true;
			}
		}
		// stehen beide Wörter *nicht* im Wörterbuch aber zusammen schon, wird auch zusammengefasst
		if (! merged
				&& startsWithLetter(token)
				&& ! nextLine.isEmpty()
				&& (startsWithLetter(nextLine.get(0)))) {
			String nextToken = nextLine.get(0);
			if (! ciDict.contains(token) && ! ciDict.contains(nextToken)
					&& ciDict.contains(token + nextToken)) {
				merge(line, nextLine);
				merged = true;
			}
		}

		return merged;
	}

	private static boolean startsWithLetter(String token) {
		return Character.isAlphabetic(token.codePointAt(0));
	}

	private static boolean endsWithDashLike(String s) {
		return s.length() > 1
				&& isDashLike(s.charAt(s.length() - 1))
				&& s.charAt(s.length() - 2) != '\\';
	}

	private static boolean isDashLike(char ch) {
		return ch == 's' || ch == 'e' || ch == 'v' || ch == 'r';
	}

	private static void merge(List<String> line1, List<String> line2) {
		// Wörter zusammenfügen
		String token = line1.get(line1.size() - 1);
		String nextToken = line2.remove(0);
		// Schmierzeichen am Zeilenanfang überspringen
		if (SCHMIERZEICHEN_ZEILENBEGINN.contains(nextToken)) {
			nextToken = line2.remove(0);
		}
		token += nextToken;
		line1.set(line1.size() - 1, token);
		// Satzzeichen in die aktuelle Zeile übernehmen
		while (! line2.isEmpty() && TextUtils.isSatzzeichen(line2.get(0))) {
			line1.add(line2.remove(0));
		}
		// Leerzeichen am Zeilenanfang entfernen.
		while (! line2.isEmpty() && " ".equals(line2.get(0))) {
			line2.remove(0);
		}
	}
}
