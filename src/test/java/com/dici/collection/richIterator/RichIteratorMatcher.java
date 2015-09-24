package com.dici.collection.richIterator;

import static com.dici.check.Check.notNull;

import java.util.Iterator;
import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class RichIteratorMatcher<T> extends TypeSafeMatcher<RichIterator<T>> {
	private final List<T> expected;
	private List<T> actual;
	
	public static <T> RichIteratorMatcher<T> iteratorEqualTo(Iterator<T> expected) { return new	RichIteratorMatcher<>(expected); }

	private RichIteratorMatcher(Iterator<T> expected) { this.expected = RichIterators.wrap(notNull(expected)).toList(); }

	@Override 
	public void describeTo(Description description) { 
		description.appendValue(expected)
				   .appendText("\n\tbut: was ")
				   .appendValue(actual);
	}
	
	@Override
	protected boolean matchesSafely(RichIterator<T> it) { return expected.equals(actual = it.toList()); }
}
