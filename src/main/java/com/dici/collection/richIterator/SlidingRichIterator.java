package com.dici.collection.richIterator;

import static com.dici.check.Check.isPositive;
import static com.dici.collection.CollectionUtils.copyAsList;

import java.util.Deque;
import java.util.LinkedList;

public class SlidingRichIterator<X> extends ClassicRichIteratorDecorator<X, RichIterator<X>> {
    private final Deque<X> slide = new LinkedList<>();
    private final int window; 
    private final int step;
    
    protected SlidingRichIterator(RichIterator<X> it, int window, int step) { 
        super(it);
        this.window = isPositive(window);
        this.step   = isPositive(step  ); 
    }

    @Override
    protected boolean hasNextInternal() throws Exception {  return it.hasNext() || !slide.isEmpty(); }

    @Override
    protected RichIterator<X> nextInternal() throws Exception {
        // first iteration
        if (slide.isEmpty()) 
            for (int i = 0; i < window && it.hasNext(); i++) slide.addLast(it.next());
        
        RichIterator<X> res = RichIterators.fromCollection(copyAsList(slide));
        if (slide.size() < window) slide.clear();
        else {
            for (int i = 0; i < step && it.hasNext()    ; i++) slide.addLast(it.next());
            for (int i = 0; i < step && !slide.isEmpty(); i++) slide.pop();
        }
        
        return res;
    }
}