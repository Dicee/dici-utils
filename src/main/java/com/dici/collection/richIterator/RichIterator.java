package com.dici.collection.richIterator;

import static com.dici.exceptions.ExceptionUtils.uncheckExceptionsAndGet;
import static com.dici.exceptions.ExceptionUtils.uncheckedBinaryOperator;
import static com.dici.exceptions.ExceptionUtils.uncheckedConsumer;
import static com.dici.exceptions.ExceptionUtils.uncheckedRunnable;
import static com.dici.exceptions.ExceptionUtils.uncheckedUnaryOperator;
import static java.util.stream.Collectors.joining;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.dici.exceptions.ExceptionUtils.ThrowingBinaryOperator;
import com.dici.exceptions.ExceptionUtils.ThrowingConsumer;
import com.dici.exceptions.ExceptionUtils.ThrowingFunction;
import com.dici.exceptions.ExceptionUtils.ThrowingPredicate;
import com.dici.exceptions.ExceptionUtils.ThrowingUnaryOperator;
import com.google.common.base.Objects;
import com.google.common.collect.Streams;
import javafx.util.Pair;

public abstract class RichIterator<X> implements Iterator<X>, Iterable<X>, Closeable, AutoCloseable {
	public static <X> RichIterator<X> iterate(X seed, ThrowingUnaryOperator<X> throwingOp) {
		UnaryOperator<X> op = uncheckedUnaryOperator(throwingOp);
		return RichIterators.wrap(new Iterator<X>() {
			private X current = seed;
			
			@Override
			public boolean hasNext() { return true; }

			@Override
			public X next() {
				X res = current;
				current = op.apply(current);
				return res;
			}
		});
	}

    private boolean                closed            = false;
    private boolean                used              = false;
    private boolean                releasedResources = false;
    private long                   count             = 0;
    private ThrowingConsumer<Long> onClose;

	public Iterator<X> iterator() {
		ensureValidState();
		setUsed();
		return this;
	}

	@Override
	public final boolean hasNext() { return !closed && !releasedResources && uncheckExceptionsAndGet(this::hasNextInternal); }

	@Override
	public final X next() {
		ensureNotClosed();
		if (!hasNext()) throw new NoSuchElementException();
		X next = uncheckExceptionsAndGet(this::nextInternal);
		if (!hasNext()) uncheckedRunnable(this::releaseResources);
		count++;
		return next;
	}
	
	@Override
	public final void close() throws IOException {
		if (closed) return;
		try {
			releaseResources();
		} catch (Exception e) {
			throw new IOException(e);
		} finally {
			closed = true;
			try {
			    if (onClose != null) onClose.accept(count);
			} catch (Exception e) {
			    throw new IOException(e);
			}
		}
	}
	
	protected final void releaseResources() throws IOException {
	    if (releasedResources) return;
	    releasedResources = true;
	    closeInternal();
	}
	
	protected abstract boolean hasNextInternal() throws Exception;
	protected abstract X nextInternal() throws Exception;
	
	/**
	 * Should not be called explicitly by a subclass. Use releaseResources instead.
	 * @throws IOException
	 */
	protected void closeInternal() throws IOException { }
	
	public final RichIterator<X> onClose(ThrowingConsumer<Long> onClose) {
		this.onClose = onClose;
		return this;
	}
	
	public final <Y> RichIterator<Y> map(ThrowingFunction<X,Y> function) { 
		ensureValidState();
		return new MappedRichIterator<>(this,function); 
	}
	
	public final <Y> RichIterator<Y> flatMap(ThrowingFunction<X,? extends Iterable<Y>> function) { 
		ensureValidState();
		return new FlatMappedRichIterator<>(this,function);
	}
	
	public final RichIterator<X> filter(ThrowingPredicate<X> predicate) { 
		ensureValidState();
		return new FilteredRichIterator<>(this,predicate); 
	}
	
	public final <K,V> PairRichIterator<K,V> mapToPair(ThrowingFunction<X,K> keyFunction, ThrowingFunction<X,V> valueFunction) {
		ensureValidState();
		return PairRichIterator.pairIterator(this,keyFunction,valueFunction);
	}
	
	public final <Y> PairRichIterator<X,Y> zip(RichIterator<Y> that) {
		ensureValidState();
		return new ZippedRichIterator<>(this,that);
	}
	
	public final PairRichIterator<Integer,X> zipWithIndex() {
		ensureValidState();
		return new ZippedRichIterator<>(RichIntIterator.counter(),this);
	}
	
	public final <Y extends X> RichIterator<X> concat(RichIterator<Y> that) {
		ensureValidState();
		return that.hasNext() ? new ConcatenatedRichIterators<>(List.of(this, that.map(y -> (X) y))) : this;
	}
	
	public final <K> PairRichIterator<K,List<X>> groupBy(ThrowingFunction<X,K> classifier) {
		ensureValidState();
		return mapToPair(classifier,ThrowingFunction.identity()).groupByKey();
	}

	public final GroupedRichIterator<X> grouped(int size) {
		ensureValidState();
		return GroupedRichIterator.create(new UniformBatchRichIterator<>(this, size));
	}
	
	public final GroupedRichIterator<X> grouped(Comparator<X> cmp) {
		ensureValidState();
		return GroupedRichIterator.create(new GroupByComparatorRichIterator<>(this, cmp));
	}
	
	public final RichIterator<RichIterator<X>> sliding(int window, int step) {
        ensureValidState();
        return new SlidingRichIterator<>(this, window, step);
    }
	
	public final RichIterator<X> take(int n) {
		ensureValidState();
		return TakeRichIterator.take(this, n);
	}
	
	public final RichIterator<X> takeWhile(ThrowingPredicate<X> predicate) {
		ensureValidState();
		return TakeRichIterator.takeWhile(this, predicate);
	}
	
	public final RichIterator<X> takeUntil(ThrowingPredicate<X> predicate) {
		ensureValidState();
		return TakeRichIterator.takeUntil(this, predicate);
	}
	
	public final RichIterator<X> drop(int n) {
	    ensureValidState();
	    return DropRichIterator.drop(this, n);
	}

	public final RichIterator<X> dropWhile(ThrowingPredicate<X> drop) {
	    ensureValidState();
	    return DropRichIterator.dropWhile(this, drop);
	}
	
	public final RichIterator<X> dropUntil(ThrowingPredicate<X> drop) {
	    ensureValidState();
	    return DropRichIterator.dropUntil(this, drop);
	}

	public final RichIterator<X> distinct() {
		ensureValidState();
		return new DistinctRichIterator<>(this);
	}
	
	public final RichIterator<X> sorted(Comparator<? super X> cmp) {
		ensureValidState();
		return new SortedRichIterator<>(this,cmp);
	}
	
	public final RichIterator<X> sorted() { return sorted(null); }

	public final Optional<X> max(Comparator<? super X> cmp) { return stream().max(cmp); }
	public final Optional<X> min(Comparator<? super X> cmp) { return stream().min(cmp); }
	
	public final <Y> Y fold(Y initialValue, BiFunction<X,Y,Y> combiner) {
		ensureValidState();
		setUsed();
		
		Y res = initialValue;
		while (hasNext()) res = combiner.apply(next(),res);
		return res;
	}
	
	public final RichIterator<X> buffered(int size) {
		ensureValidState();
		return new BufferedRichIterator<>(this, size);
	}
	
	public final boolean contains(Object o) {
	    ensureValidState();	
	    setUsed();
	    
	    while (hasNext()) 
	        if (Objects.equal(o, next())) return true;
	        
	    return false;
	}
	
	public final Optional<X> reduce(ThrowingBinaryOperator<X> binaryOp) { return stream().reduce(uncheckedBinaryOperator(binaryOp)); }
	public final void foreach(ThrowingConsumer<X> consumer)             { stream().forEach(uncheckedConsumer(consumer)); }
	public final long count()                                           { return stream().count(); }
	
	public final String mkString()                                      { return mkString("");                                                        }
	public final String mkString(String sep)                            { return mkString("",sep,"");                                                 }
	public final String mkString(String first, String sep, String last) { return first + stream().map(Object::toString).collect(joining(sep)) + last; }
	
	public final void writeToFile(File f) throws IOException {
		try (BufferedWriter bw = Files.newBufferedWriter(f.toPath())) {
			map(Object::toString).foreach(bw::write);
		}
	}
	
	public final void writeToFile(File f, String sep) throws IOException {
		try (BufferedWriter bw = Files.newBufferedWriter(f.toPath())) {
			boolean first = true;
			for (String s : map(Object::toString)) {
				if (first) {
					first = false;
					bw.write(sep);
				}
				bw.write(s);
			}
		}
	}
	
	public final X last() { return lastSafely().orElseThrow(NoSuchElementException::new); }
	
	public final Optional<X> lastSafely() {
	    ensureValidState();	
	    setUsed();
	    
	    if (!hasNext()) return Optional.empty();
	    while (true) {
	        X x = next();
	        if (!hasNext()) return Optional.of(x);
	    }
	}
	
	/**
	 * Finds the first element of the RichIterator matching a predicate
	 * @note Important : this is NOT a terminal operation in the general case. It will only close the RichIterator if all
	 * 		 its elements have been consumed during the search. Thus, you can safely repeat
	 * @param predicate
	 * @return the first element of the RichIterator matching the predicate or Optional.empty() if no match was found
	 */
	public final Optional<X> findAny(ThrowingPredicate<X> predicate) {
		ensureValidState();
		return filter(predicate).stream().findAny();
	}
	
	public final Optional<X> findFirst(ThrowingPredicate<X> predicate) {
		ensureValidState();
		return filter(predicate).stream().findFirst();
	}
	
	public final int indexWhere(ThrowingPredicate<X> predicate) {
		ensureValidState();
		return zipWithIndex().findFirst(pair -> predicate.test(pair.getValue())).map(Pair::getKey).orElse(-1);
	}
	
	public final int lastIndexWhere(ThrowingPredicate<X> predicate) {
	    ensureValidState();
	    return zipWithIndex().filter(pair -> predicate.test(pair.getValue())).last().getKey();
	}
	
	public final boolean forall(ThrowingPredicate<X> predicate) { return !exists(predicate.negate())    ; }
	public final boolean exists(ThrowingPredicate<X> predicate) { return  findAny(predicate).isPresent(); }
	
	/* Java does not have lower bounds except for wildcards nor variant types, which causes problems in expressions such as
	 *     List<Move> moves = RichIntIterator.range(1, 3).map(VerticalMove::new).toList()
	 * because it will be typed as List<VerticalMove>. As a fix, we propose to write 
	 *     List<Move> moves = RichIntIterator.range(1, 3).map(VerticalMove::new).asType(Move.class).toList()
	 * This is a shame, but as for now that's the way it is.
	 */
	public final <Y> RichIterator<Y> asType(Class<Y> clazz) {
	    ensureValidState();
	    return map(clazz::cast);
	}
	
	public final List<X> toList() { return stream().collect(Collectors.toList()); }
	public final Set<X> toSet() { return stream().collect(Collectors.toSet()); }
	public final <K,V> Map<K,V> toMap(ThrowingFunction<X,K> keyFunction, ThrowingFunction<X,V> valueFunction) { return mapToPair(keyFunction,valueFunction).toMap(); }
	
	public Stream<X> stream() { 
		ensureValidState();
		setUsed();
		return Streams.stream((Iterator<X>) this);
	}

	private void ensureValidState() {
		ensureNotClosed();
		ensureNotAlreadyUsed();
	}
	
	private void ensureNotClosed     () { if (isClosed()) throw new IllegalStateException("This iterator is already closed"      ); }
	private void ensureNotAlreadyUsed() { if (isUsed  ()) throw new IllegalStateException("This object can only be iterated once"); }

	public final boolean isClosed() { return closed; }
	
	protected void setUsed() {used = true; }
	protected boolean isUsed() { return used; }
}
