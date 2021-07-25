package org.github.cradloff.scanutils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class CaseInsensitiveComparatorTest {
	@Test public void compare() {
		CaseInsensitiveComparator cmp = new CaseInsensitiveComparator();
		assertThat(cmp.compare("gleich", "gleich")).isEqualTo(0);
		assertThat(cmp.compare("Anton", "Berta")).isLessThan(0);
		assertThat(cmp.compare("kleiner", "Kleiner")).isLessThan(0);
		assertThat(cmp.compare("Berta", "Anton")).isGreaterThan(0);
		assertThat(cmp.compare("Größer", "größer")).isGreaterThan(0);
	}
}
