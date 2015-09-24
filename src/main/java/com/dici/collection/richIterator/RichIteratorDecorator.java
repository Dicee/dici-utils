package com.dici.collection.richIterator;

import static com.dici.check.Check.notNull;

import java.io.IOException;

public abstract class RichIteratorDecorator<X, Y, ITERATOR extends RichIterator<X>> extends RichIterator<Y> {
	protected final ITERATOR it;
	protected RichIteratorDecorator(ITERATOR it) { this.it = notNull(it); }
	@Override protected void closeInternal() throws IOException { it.close(); }
}
