package com.dici.collection.richIterator;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.util.List;

import com.dici.io.IOUtils;

public abstract class FromResourceRichIterator<X> extends NullableRichIterator<X> {
	private final List<Closeable> resources;

	public FromResourceRichIterator(Closeable... resources) { this.resources = List.of(resources); }
	
	/**
	 * Reads the next element of the iterator.
	 * @return the next value. A null value signals the end of the iterator.
	 * @throws EOFException potentially returned if the iterator has reached its end. Alternatively, the method will return null.
	 * @throws IOException
	 */
	protected abstract X tryReadNext() throws EOFException, IOException;

	@Override 
	protected X nextOrNull() throws Exception {
		try {
			return tryReadNext();
		} catch (EOFException e) {
			return null;
		} catch (Exception e) {
			throw e;
		}
	}

	@Override 
	protected final void closeInternal() throws IOException { IOUtils.closeAllQuietly(resources); }
}
