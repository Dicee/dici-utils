package com.dici.collection.richIterator;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class ArrayRichIterator<T> extends RichIterator<T> {
	@NonNull private T[] arr;
	private int i = 0;

	@Override
	protected boolean hasNextInternal() { return i < arr.length; }

	@Override
	protected T nextInternal() { return arr[i++]; }
	
	@Override
	protected void closeInternal() { arr = null; }
}
