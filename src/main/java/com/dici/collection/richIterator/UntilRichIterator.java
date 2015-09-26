package com.dici.collection.richIterator;

import static com.dici.check.Check.notNull;
import com.dici.exceptions.ExceptionUtils.ThrowingPredicate;

public class UntilRichIterator<X> extends ClassicRichIteratorDecorator<X, X> {
	private boolean found;
	private final ThrowingPredicate<X>	predicate;
	
	public UntilRichIterator(RichIterator<X> it, ThrowingPredicate<X> predicate) {
		super(it);
		this.predicate = notNull(predicate);
	}

	@Override
	protected boolean hasNextInternal() throws Exception { return !found && it.hasNext(); }

	@Override
	protected X nextInternal() throws Exception {
		X next = it.next();
		found  = predicate.test(next);
		return next;
	}
}