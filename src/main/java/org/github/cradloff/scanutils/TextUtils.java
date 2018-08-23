package org.github.cradloff.scanutils;

public class TextUtils {

	public static String satzzeichenErsetzen(String line) {
		String result = line;
		// mehrfache Gedankenstriche zusammenfassen
		result = result.replaceAll("[-—]{2,}", "—");
	
		// << und >> in « und » umwandeln
		result = result.replaceAll("<<", "«");
		result = result.replaceAll(">>", "»");
		// ,, durch » ersetzen
		result = result.replaceAll(",,", "»");
	
		// gerade Anführungszeichen durch typographische ersetzen
		result = result.replace('\'', '’');
	
		// mittleren Punkt durch normalen Punkt ersetzen
		result = result.replace('·', '.');
	
		return result;
	}

}
