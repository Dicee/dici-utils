package com.dici.collection.toolbox;

import com.dici.collection.richIterator.RichIterators;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;

import static com.dici.strings.StringUtils.lastChar;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class CompareSortedIteratorsTest {
	private Iterator<String>	expected;
	private TextComparison		compareSorted;
	private DiffReport<String> 	report;
	
	@BeforeEach
	public void setUp() {
		this.compareSorted = new TextComparison();
		this.expected      = RichIterators.of("z", "a", "us", "xhtml");
		this.report        = new DiffReport<>();
	}
	
	@Test
	public void failsIfNotSorted() {
		Iterator<String> actual = RichIterators.of("z", "a", "xhtml", "us");
		assertThatThrownBy(() -> compareSorted.compareFully(actual, expected, report))
				.isExactlyInstanceOf(IllegalStateException.class);
	}
	
	@Test
	public void testSimpleComparison() {
		Iterator<String> actual = RichIterators.of("z", "A", "ba", "xhtml");
		compareSorted.compareFully(actual, expected, report);
		assertReportEqualsTo(report, 1, 0, 0, 4, asList(new NotEqualDiff<>("ba","us")));
	}

	@Test
	public void testUnexpected() {
		Iterator<String> actual = RichIterators.of("z", "a", "r", "r", "us", "is", "xhtml");
		compareSorted.compareFully(actual, expected, report);
		assertReportEqualsTo(report, 0, 0, 3, 7, asList(new UnexpectedElementDiff<>("r"), new UnexpectedElementDiff<>("r"), new UnexpectedElementDiff<>("is")));
	}

	@Test
	public void testMissing() {
		Iterator<String> actual = RichIterators.of("z", "us");
		compareSorted.compareFully(actual, expected, report);
		assertReportEqualsTo(report, 0, 2, 0, 4, asList(new MissingElementDiff<>("a"), new MissingElementDiff<>("xhtml")));
	}

	@Test
	public void testVariousDiffs() {
		Iterator<String> actual   = RichIterators.of("z", "b", "r", "t", "us", "is", "qa", "di", "xhtml", "emlfp");
		Iterator<String> expected = RichIterators.of("Z", "a", "r", "T", "us", "is", "qi", "qi", "naa", "xhTml");
		compareSorted.compareFully(actual, expected, report);
		assertReportEqualsTo(report, 2, 1, 1, 11, asList(new NotEqualDiff<>("b","a"), new NotEqualDiff<>("qa","qi"), new MissingElementDiff<>("naa"), new UnexpectedElementDiff<String>("emlfp")));
		assertThat(report.getEventCount(TextComparison.EQUALS_IGNORE_CASE), is(3L));
		assertThat(report.getEventCount(TextComparison.LAST_CHAR_EQUAL)   , is(1L));
	}
	
	private static void assertReportEqualsTo(DiffReport<String> report, int diffCount, int missingCount, int unexpectedCount, int totalCount, List<Diff<String>> diffs) {
		assertThat(report.getDiffCount      (), is(diffCount));
		assertThat(report.getMissingCount   (), is(missingCount));
		assertThat(report.getUnexpectedCount(), is(unexpectedCount));
		assertThat(report.getTotalCount     (), is(totalCount));
		assertThat(report.getDiffs          (), equalTo(diffs));
	} 
	
	private static class TextComparison extends CompareSortedIterators<String> {
		private static final String EQUALS_IGNORE_CASE = "equals_ignore_case";
		private static final String LAST_CHAR_EQUAL    = "last_char_equal";
	
		public TextComparison() { super((s0, s1) -> Integer.compare(s0.length(), s1.length()), TextComparison::deepCheckValidity); }
	
		private static boolean deepCheckValidity(String actual, String expected, DiffReport<String> report) { 
			boolean isValid;
			if      (isValid = actual.equalsIgnoreCase(expected))      report.reportEvent(EQUALS_IGNORE_CASE);
			else if (isValid = lastChar(actual) == lastChar(expected)) report.reportEvent(LAST_CHAR_EQUAL);
			return isValid;
		}
	}
}