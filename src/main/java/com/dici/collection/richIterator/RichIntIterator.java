package com.dici.collection.richIterator;

import java.util.Iterator;

import com.dici.check.Check;

public class RichIntIterator extends RichIteratorDecorator<Integer, Integer, RichIterator<Integer>> {
    public static RichIntIterator counter() { return counter(0); }
	public static RichIntIterator counter(int init) {
		return new RichIntIterator(new RichIterator<Integer>() {
			private Integer count = init;
			
			@Override
			protected Integer nextInternal() throws Exception {
				if (count.equals(Integer.MAX_VALUE)) throw new IllegalStateException("Integer capacity exceeded");
				return count++;
			}
			
			@Override
			protected boolean hasNextInternal() throws Exception { return !count.equals(Integer.MAX_VALUE); }
		});
	}
	
	public static RichIntIterator range(int from, int to) {
		Check.isGreaterThan(to,from,"Empty range");
		return new RichIntIterator(counter(from).takeWhile(i -> i < to));
	}

	public static RichIntIterator closedRange(int from, int until) {
		Check.isGreaterOrEqual(until,from);
		return new RichIntIterator(counter(from).takeUntil(i -> i == until));
	}

	private RichIntIterator(Iterator<Integer> it) { super(RichIterators.wrap(it)); }

	@Override protected Integer nextInternal   () throws Exception { return it.nextInternal   (); }
	@Override protected boolean hasNextInternal() throws Exception { return it.hasNextInternal(); }
}
