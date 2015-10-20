package com.dici.collection.richIterator;

import static com.dici.collection.richIterator.RichIteratorMatcher.iteratorEqualTo;
import static com.dici.collection.richIterator.RichIterators.emptyIterator;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

public class DropRichIteratorTest {
    private RichIterator<Integer> it;

    @Before
    public void setUp() {
        this.it = RichIterators.of(3, 8, 5, 6, 7, 9, 1, 15);
    }
    
    @Test
    public void drop_emptyIterator() {
        assertThat(emptyIterator().drop(5), iteratorEqualTo(emptyIterator()));
    }

    @Test
    public void drop_moreThanHasElements() {
        assertThat(RichIterators.of(1, 2, 3).drop(5), iteratorEqualTo(emptyIterator()));
    }
    
    @Test
    public void drop_basicCase() {
        assertThat(it.drop(5), iteratorEqualTo(RichIterators.of(9, 1, 15)));
    }
    
    @Test
    public void dropUntil() {
        assertThat(it.dropUntil(i -> i == 5), iteratorEqualTo(RichIterators.of(5, 6, 7, 9, 1, 15)));
    }
    
    @Test
    public void dropUntilNever() {
        assertThat(it.dropUntil(i -> false), iteratorEqualTo(emptyIterator()));
    }
    
    @Test
    public void dropWhile() {
        assertThat(it.dropWhile(i -> i < 9), iteratorEqualTo(RichIterators.of(9, 1, 15)));
    }
    
    @Test
    public void dropWhileTrue() {
        assertThat(it.dropWhile(i -> true), iteratorEqualTo(emptyIterator()));
    }
}