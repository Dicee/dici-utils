package com.dici.collection.richIterator;

import com.dici.exceptions.ExceptionUtils.ThrowingFunction;

public interface IteratorTransformation<X, Y> extends ThrowingFunction<RichIterator<X>, RichIterator<Y>> { 
	public static <X> IteratorTransformation<X, X> identity() { return it -> it; };
}
