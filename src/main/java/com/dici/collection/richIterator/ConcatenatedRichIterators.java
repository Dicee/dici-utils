package com.dici.collection.richIterator;

import static com.dici.check.Check.notNull;

import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

class ConcatenatedRichIterators<X> extends RichIterator<X> {
	private final Deque<RichIterator<X>> iterators;

	@SafeVarargs
	public ConcatenatedRichIterators(RichIterator<X>... iterators) {
		this(List.of(iterators));
	}
	
	public ConcatenatedRichIterators(Collection<RichIterator<X>> iterators) {
		this.iterators = new LinkedList<>(notNull(iterators));
	}

	@Override
	protected boolean hasNextInternal() throws Exception {
		while (!iterators.isEmpty() && !iterators.peek().hasNext()) iterators.pop();
		if (iterators.isEmpty()) return false;
		return iterators.peek().hasNext();
	}

	@Override
	protected X nextInternal() throws Exception {
		X res = iterators.peek().next();
		if (!iterators.peek().hasNext()) { iterators.pop().releaseResources(); }
		return res;
	}
}
