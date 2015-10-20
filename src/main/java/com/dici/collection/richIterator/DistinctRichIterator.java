package com.dici.collection.richIterator;

import static com.dici.check.Check.notNull;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

class DistinctRichIterator<X> extends NullableRichIterator<X> {
	private final Set<X>			elts	= new HashSet<>();
	private final RichIterator<X>	it;

	public DistinctRichIterator(RichIterator<X> it) { this.it = notNull(it); }

	@Override
	protected X nextOrNull() throws Exception {
		while (it.hasNext()) {
			X next = it.next();
			if (elts.add(next)) return next;
		}
		return null;
	}

	@Override protected void closeInternal() throws IOException { it.releaseResources(); }
	@Override protected void setUsed() { it.setUsed(); super.setUsed(); }
}
