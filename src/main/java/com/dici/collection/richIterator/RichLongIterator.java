package com.dici.collection.richIterator;

import java.util.Iterator;

import com.dici.check.Check;

public class RichLongIterator extends RichIteratorDecorator<Long, Long, RichIterator<Long>> {
    public static RichLongIterator counter() { return counter(0); }
	public static RichLongIterator counter(long init) {
		return new RichLongIterator(new RichIterator<Long>() {
        	private Long count = init;
        	
        	@Override
        	protected Long nextInternal() throws Exception {
        		if (count.equals(Long.MAX_VALUE)) throw new IllegalStateException("Long capacity exceeded");
        		return count++;
        	}
        	
        	@Override
        	protected boolean hasNextInternal() throws Exception { return !count.equals(Long.MAX_VALUE); }
        });
	}

	private RichLongIterator(Iterator<Long> it) { super(RichIterators.wrap(it)); }
	
	public static RichLongIterator range(long from, long to) {
		Check.isGreaterThan(to,from,"Empty range");
		return new RichLongIterator(counter(from).takeWhile(i -> i < to));
	}

	public static RichLongIterator closedRange(long from, long until) {
		Check.isGreaterOrEqual(until,from);
		return new RichLongIterator(counter(from).takeUntil(i -> i == until));
	}

    @Override protected boolean hasNextInternal() throws Exception { return it.hasNext(); }
    @Override protected Long    nextInternal   () throws Exception { return it.next   (); }
}
