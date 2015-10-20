package com.dici.collection.richIterator;

import static com.dici.check.Check.notNull;
import static com.dici.function.Predicates.countdownPredicate;

import java.util.Iterator;

import com.dici.exceptions.ExceptionUtils.ThrowingPredicate;

class TakeRichIterator<X> extends NullableRichIterator<X> {
    static <X> RichIterator<X> take(Iterator<X> it, int n) {
        return new TakeRichIterator<X>(it, countdownPredicate(n), true);
    }
    
    static <X> RichIterator<X> takeWhile(Iterator<X> it, ThrowingPredicate<X> take) {
        return new TakeRichIterator<X>(it, take, false);
    }

    static <X> RichIterator<X> takeUntil(RichIterator<X> it, ThrowingPredicate<X> dontTake) {
        return new TakeRichIterator<X>(it, dontTake.negate(), true);
    }
    
    private final RichIterator<X>      it;
    private final ThrowingPredicate<X> take;
    private boolean                    takeNext = true;
    private boolean                    takeLast;

    private TakeRichIterator(Iterator<X> it, ThrowingPredicate<X> take, boolean takeLast) { 
        this.it       = notNull(RichIterators.wrap(it));
        this.take     = notNull(take); 
        this.takeLast = takeLast;
    }

    @Override
    protected X nextOrNull() throws Exception {
        if (!takeNext || !it.hasNext()) return null;
        X next = it.next();
        return (takeNext = take.test(next)) || takeLast ? next : null;     
    }
    
    @Override protected void setUsed() { it.setUsed(); super.setUsed(); }
}