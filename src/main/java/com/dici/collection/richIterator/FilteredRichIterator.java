package com.dici.collection.richIterator;

import com.dici.exceptions.ExceptionUtils.ThrowingPredicate;

final class FilteredRichIterator<X> extends RichIteratorDecorator<X, X, LookAheadRichIterator<X>> {
	private final ThrowingPredicate<X> predicate;

	public FilteredRichIterator(RichIterator<X> it, ThrowingPredicate<X> predicate) { 
		super(new LookAheadRichIterator<>(it));
		this.predicate = predicate;
	}

	@Override
	protected boolean hasNextInternal() throws Exception {
		while (it.peek() != null) {
			if (predicate.test(it.peek())) return true;
			it.next();
		}
		return false;
	}

	@Override
	protected X nextInternal() { return it.next(); }
}
