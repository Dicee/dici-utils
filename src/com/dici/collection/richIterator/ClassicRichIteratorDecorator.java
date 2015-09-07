package com.dici.collection.richIterator;

abstract class ClassicRichIteratorDecorator<X, Y> extends RichIteratorDecorator<X, Y, RichIterator<X>> {
	protected ClassicRichIteratorDecorator(RichIterator<X> it) { super(it); }
}
