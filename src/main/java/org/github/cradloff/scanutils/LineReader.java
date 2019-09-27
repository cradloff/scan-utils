package org.github.cradloff.scanutils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class LineReader {
	/** leere Liste als Markierung für das Dateiende */
	public static final List<String> EOF = new ArrayList<>();

	private int prevContext;
	private int nextContext;
	private List<List<String>> lines;
	private BufferedReader reader;

	public LineReader(Reader in) throws IOException {
		this(in, 1, 1);
	}

	public LineReader(Reader in, int prevContext, int nextContext) throws IOException {
		this.prevContext = prevContext;
		this.nextContext = nextContext;
		this.reader = new BufferedReader(in);
		init();
	}

	private void init() throws IOException {
		// Puffer füllen
		lines = new LinkedList<>();
		// eine zusätzliche Zeile, damit steht der Cursor auf EOF
		for (int i = 0; i <= prevContext; i++) {
			lines.add(EOF);
		}
		// die aktuelle plus die nachfolgenden Zeilen einlesen
		for (int i = 0; i <= nextContext; i++) {
			doReadLine();
		}
	}

	private void doReadLine() throws IOException {
		String line = reader.readLine();
		if (line != null) {
			lines.add(TextUtils.split(line));
		}
	}

	public boolean readLine() throws IOException {
		lines.remove(0);
		doReadLine();

		return hasNext(0);
	}

	public List<String> prev() {
		return prev(1);
	}

	public List<String> prev(int count) {
		return next(- count);
	}

	public List<String> current() {
		return next(0);
	}

	public List<String> next() {
		return next(1);
	}

	public List<String> next(int count) {
		int index = count + prevContext;
		if (index >= lines.size()) {
			return EOF;
		}

		return lines.get(index);
	}

	public boolean hasNext() {
		return hasNext(1);
	}

	public boolean hasNext(int count) {
		return lines.size() > prevContext + count;
	}

	public void swap(int index1, int index2) {
		int i1 = index1 + prevContext;
		int i2 = index2 + prevContext;
		List<String> tmp = lines.get(i1);
		lines.set(i1, lines.get(i2));
		lines.set(i2, tmp);
	}

	public void skip(int i) {
		lines.remove(i + prevContext);
	}
}
