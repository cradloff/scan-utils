package org.github.cradloff.scanutils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/** Extrahiert den Text aus Kobo-Annotations */
public class KoboAnnotationExtractor {
	public static void main(String... args) throws Exception {
		// Parameter prüfen
		if (args.length < 1) {
			System.err.println("Aufruf: KoboAnnotationExtractor <Dateiname(n)>");

			return;
		}

		List<File> inputs = FileAccess.checkExists(args);
		// keine Dateien gefunden?
		if (inputs.isEmpty()) {
			return;
		}

		String lastFile = "n/a";
		try (PrintStream out = new PrintStream(new FileOutputStream("annotations.log"))) {
			for (File annotation : inputs) {
				System.out.println("Parse Datei " + annotation);

				// Annotations parsen
				final List<Record> records = parse(annotation);

				// Annotations ausgeben
				System.out.println("Schreibe annotations.log");
				Collections.sort(records);

				out.println(records.get(0).getTitle());
				for (Record record : records) {
					if (!lastFile.equals(record.getFilename())) {
						lastFile = record.getFilename();
						out.println();
						out.println(lastFile);
						out.println();
					}
					out.println(" - " + record.getTargetText());
					if (record.getContentText() != null) {
						out.println(" + " + record.getContentText());
					}
				}
			}
		}
		System.out.println("fertig!");
	}

	private static List<Record> parse(File annotation) throws ParserConfigurationException, SAXException, IOException {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		SAXParser parser = factory.newSAXParser();
		final List<Record> records = new ArrayList<>();
		parser.parse(annotation, new DefaultHandler() {
			String start, targetText, contentText, title;
			boolean text = false, target = false, content = false;
			StringBuilder sb = new StringBuilder();

			@Override
			public void startElement(String uri, String localName, String qName, Attributes attributes)
					throws SAXException {
				if ("title".equals(localName)) {
					text = true;
					title = null;
				} else if ("annotation".equals(localName)) {
					targetText = null;
					contentText = null;
				} else if ("fragment".equals(localName)) {
					start = attributes.getValue("start");
				} else if ("text".equals(localName)) {
					text = true;
					sb.setLength(0);
				} else if ("target".equals(localName)) {
					target = true;
				} else if ("content".equals(localName)) {
					content = true;
				}
			}

			@Override
			public void endElement(String uri, String localName, String qName) throws SAXException {
				if ("title".equals(localName)) {
					text = false;
					title = sb.toString();
				} else if ("text".equals(localName)) {
					text = false;
					if (target) {
						targetText = sb.toString();
					} else if (content) {
						contentText = sb.toString();
					}
				} else if ("target".equals(localName)) {
					target = false;
				} else if ("content".equals(localName)) {
					content = false;
				} else if ("annotation".equals(localName)) {
					Record record = new Record(title, start, targetText, contentText);
					records.add(record);
				}
			}

			@Override
			public void characters(char[] ch, int offset, int length) throws SAXException {
				if (text) {
					sb.append(ch, offset, length);
				}
			}
		});
		return records;
	}

	static class Record implements Comparable<Record> {
		private String title;
		private String filename;
		private Point point;
		private String targetText;
		private String contentText;

		public Record(String title, String start, String targetText, String contentText) {
			this.title = title;
			String s[] = start.split("#");
			this.filename = s[0];
			this.point = new Point(s[1]);
			this.targetText = targetText;
			this.contentText = contentText;
		}

		public String getTitle() {
			return title;
		}

		public String getFilename() {
			return filename;
		}

		public String getStart() {
			return filename + "#" + point;
		}

		public String getTargetText() {
			return targetText;
		}

		public String getContentText() {
			return contentText;
		}

		@Override
		public int compareTo(Record o) {
			int c = 0;
			c = filename.compareTo(o.filename);
			if (c == 0) {
				c = point.compareTo(o.point);
			}

			return c;
		}
	}

	/** Adresse innerhalb einer Datei */
	static class Point implements Comparable<Point> {
		private int coord[];

		public Point(String s) {
			Pattern pattern = Pattern.compile("point\\(/(.*)\\)");
			Matcher matcher = pattern.matcher(s);
			matcher.find();
			String u = matcher.group(1);
			String t[] = u.split("[:/]");
			coord = new int[t.length];
			for (int i = 0; i < t.length; i++) {
				coord[i] = Integer.parseInt(t[i]);
			}
		}

		@Override
		public int compareTo(Point o) {
			int c = 0;
			for (int i = 0; i < coord.length && i < o.coord.length && c == 0; i++) {
				c = coord[i] - o.coord[i];
			}

			if (c == 0) {
				c = coord.length - o.coord.length;
			}

			return c;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("point(");
			for (int i = 0; i < coord.length - 1; i++) {
				sb.append('/').append(coord[i]);
			}
			sb.append(':').append(coord[coord.length - 1]).append(')');

			return sb.toString();
		}

	}
}
