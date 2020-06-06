package com.dici.collection.richIterator;

import javafx.util.Pair;

class ZippedRichIterator<X,Y> extends PairRichIterator<X,Y> {
	public ZippedRichIterator(RichIterator<X> left, RichIterator<Y> right) {
		super(new RichIterator<>() {
			@Override
			protected boolean hasNextInternal() { return left.hasNext() && right.hasNext(); }

			@Override
			protected Pair<X,Y> nextInternal() { return new Pair<>(left.next(),right.next()); }
		});
	}
}
