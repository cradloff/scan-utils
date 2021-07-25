package org.github.cradloff.scanutils;

import java.util.Comparator;

/**
 * Ähnlich dem Comparator {@link String#CASE_INSENSITIVE_ORDER} wird ohne
 * Berücksichtigung der Groß-/Kleinschreibung sortiert. Sind jedoch beide
 * Schreibweisen vorhanden, dann wird zuerst der kleingeschriebene Wert
 * ausgegeben.
 */
public class CaseInsensitiveComparator implements Comparator<String> {

	@Override
	public int compare(String s1, String s2) {
		int cmp = s1.compareToIgnoreCase(s2);
		if (cmp == 0) {
			cmp = s2.compareTo(s1);
		}

		return cmp;
	}

}
