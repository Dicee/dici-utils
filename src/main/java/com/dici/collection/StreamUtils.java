package com.dici.collection;

import static com.dici.exceptions.ExceptionUtils.uncheckedRunnable;

import java.util.Iterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.dici.collection.richIterator.RichIterator;

public class StreamUtils {
	private StreamUtils() { }
	
	public static <T> Stream<T> iteratorToStream(Iterator<T> it) {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it,0),false);
	}
	
	public static <T> Stream<T> iteratorToStream(RichIterator<T> it) {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it,0),false).onClose(uncheckedRunnable(it::close));
	}
}
