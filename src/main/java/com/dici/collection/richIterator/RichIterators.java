package com.dici.collection.richIterator;

import static com.dici.check.Check.notNull;
import static java.util.stream.Collectors.toList;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import com.dici.files.TokenParser;
import com.dici.io.IOUtils;
import com.google.common.base.Throwables;

public class RichIterators {
	private RichIterators() { }
	
	@SafeVarargs
	public static <T> RichIterator<T> of(T... elts)                            { return new ArrayRichIterator<>(elts); }
	public static <T> RichIterator<T> fromCollection(Collection<T> collection) { return wrap(collection.iterator())  ; }
	public static <T> GroupedRichIterator<T> from2DArray(T[][] arr) { 
		return GroupedRichIterator.create(new ArrayRichIterator<>(arr).map(ArrayRichIterator::new));
	}
	
	public static RichIterator<Character> characters(String s) { return wrap(s.chars().mapToObj(i -> (char) i).iterator()); }
	
	public static RichIterator<String> fromLines(File f) {
		BufferedReader br = null;
		try {
			br = Files.newBufferedReader(f.toPath());
			final BufferedReader source = br;
			return new FromResourceRichIterator<>(source) {
				@Override protected String tryReadNext() throws IOException { return source.readLine(); }
			};
		} catch (IOException e) {
			IOUtils.closeQuietly(br);
			throw Throwables.propagate(e);
		}
	}

	public static RichIterator<String> tokens(File file, String token) { return TokenParser.parse(file, token); }
	
	public static RichIterator<Character> characters(File file) {
	    Reader reader = null;
	    try {
	        reader = new FileReader(file);
	        final Reader source = reader;
    	    return new RichIterator<>() {
                private char[] chars    = new char[2048];
                private int    index    = 0;
                private int    readUpTo = 0;
    	        
                @Override
                protected boolean hasNextInternal() throws Exception {
                    if (index < readUpTo) return true;
                    readUpTo = source.read(chars);
                    index    = 0;
                    return readUpTo != -1;
                }
                
                @Override protected Character nextInternal ()                    { return chars[index++]; }
                @Override protected void      closeInternal() throws IOException { source.close()       ; }
            };
	    } catch (IOException e) {
			IOUtils.closeQuietly(reader);
			throw Throwables.propagate(e);
        }
	}
	
	@SuppressWarnings("resource")
	public static <T> RichIterator<T> fromSerializedRecords(File f, Class<T> clazz) {
		FileInputStream   fis = null; 
		ObjectInputStream ois = null;
		try {
			fis = new FileInputStream(f);
			ois = new ObjectInputStream(fis);
			final ObjectInputStream source = ois;
			return new LookAheadRichIterator<T>(new FromResourceRichIterator<T>(fis, ois) {
				@Override
				public T tryReadNext() throws IOException {
					try {
					    T cast = clazz.cast(source.readObject());
						return cast;
					} catch (ClassNotFoundException e) {
						throw new IOException(e);
					} 
				}
			});
		} catch (EOFException e) { 
			return emptyIterator();  
		} catch (IOException e) {
			IOUtils.closeAllQuietly(List.of(fis, ois));
			throw new UncheckedIOException(e);
		}
	}

	public static <T> RichIterator<T> singleton(T elt) { return wrap(Collections.singleton(notNull(elt)).iterator()); }
	
	public static <T> RichIterator<T> wrap(Iterator<T> it) {
		if (it instanceof RichIterator) return (RichIterator<T>) it;
		return new RichIterator<T>() {
			@Override protected boolean hasNextInternal() { return it.hasNext()         ; }
			@Override protected T       nextInternal   () { return it.next()            ; }
			@Override protected void    closeInternal  () { IOUtils.closeIfCloseable(it); }
		};
	}
	
	public static <T> RichIterator<T> prepend(T value, Iterator<T> it) {
		return singleton(notNull(value)).concat(wrap(it));
	}
	
	@SafeVarargs
	public static <T> RichIterator<T> concatIterators(Iterator<T>... iterators) {
		return new ConcatenatedRichIterators<>(Stream.of(iterators).map(RichIterators::wrap).collect(toList()));
	}
	
	public static <T> RichIterator<T> emptyIterator() { return wrap(Collections.emptyIterator()); }
}


