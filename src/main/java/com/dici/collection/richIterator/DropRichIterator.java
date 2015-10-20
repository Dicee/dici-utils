package com.dici.collection.richIterator;

import static com.dici.check.Check.notNull;
import static com.dici.function.Predicates.countdownPredicate;

import com.dici.exceptions.ExceptionUtils.ThrowingPredicate;

class DropRichIterator<X> extends NullableRichIterator<X> {
    static <X> RichIterator<X> drop(RichIterator<X> it, int n) {
        return new DropRichIterator<X>(it, countdownPredicate(n));
    }
    
    static <X> RichIterator<X> dropWhile(RichIterator<X> it, ThrowingPredicate<X> drop) {
        return new DropRichIterator<X>(it, drop);
    }

    static <X> RichIterator<X> dropUntil(RichIterator<X> it, ThrowingPredicate<X> dontDrop) {
        return new DropRichIterator<X>(it, dontDrop.negate());
    }
    
    private final RichIterator<X>      it;
    private final ThrowingPredicate<X> drop;
    private boolean                    dropped = false;   
    
    private DropRichIterator(RichIterator<X> it, ThrowingPredicate<X> drop) { 
        this.it   = notNull(it);
        this.drop = notNull(drop); 
    }

    @Override
    protected X nextOrNull() throws Exception {
        if (dropped) return consumeSafely();
        while (it.hasNext()) {
            X next = it.next();
            if (!drop.test(next)) {
                dropped = true;
                return next;
            }
        }
        return null;
    }

    private X consumeSafely() { return it.hasNext() ? it.next() : null; }
    
    @Override protected void setUsed() { it.setUsed(); super.setUsed(); }
}