package com.dici.collection.richIterator;

import java.util.Comparator;
import java.util.Iterator;
import java.util.stream.Collectors;

class SortedRichIterator<T> extends ClassicRichIteratorDecorator<T,T> {
	private Iterator<T>	elts;
	private Comparator<? super T> cmp;
	
	public SortedRichIterator(RichIterator<T> it) { this(it, null); }
	public SortedRichIterator(RichIterator<T> it, Comparator<? super T> cmp) { 
		super(it); 
		this.cmp = cmp;
	}

	@Override
	protected boolean hasNextInternal() { return (elts != null && elts.hasNext()) || it.hasNext(); }

	@Override
	protected T nextInternal() {
		if (elts == null) initElts();
		return elts.next();
	}
	
	// override behaviour from parent class because we need to call RichIterator.stream on the underlying iterator
	@Override 
	protected void setUsed() { 
	    initElts();
	    super.setUsed(); 
	}
	
    private void initElts() throws AssertionError {
        elts = (cmp == null ? it.stream().sorted() : it.stream().sorted(cmp)).collect(Collectors.toList()).iterator();
	    if (!elts.hasNext()) throw new AssertionError("The underlying RichIterator declares it has a next element whereas it really has not");
    }
}