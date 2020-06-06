package com.dici.collection.richIterator;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class DistinctRichIterator<X> extends NullableRichIterator<X> {
	@NonNull private final RichIterator<X> it;
	private final Set<X> elts = new HashSet<>();

	@Override
	protected X nextOrNull() {
		while (it.hasNext()) {
			X next = it.next();
			if (elts.add(next)) return next;
		}
		return null;
	}

	@Override protected void closeInternal() throws IOException { it.releaseResources(); }
	@Override protected void setUsed() { it.setUsed(); super.setUsed(); }
}
