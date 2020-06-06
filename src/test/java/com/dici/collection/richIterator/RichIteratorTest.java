package com.dici.collection.richIterator;

import static com.dici.collection.richIterator.RichIteratorMatcher.iteratorEqualTo;
import static com.dici.collection.richIterator.RichIteratorTestUtils.observable;
import static com.dici.collection.richIterator.RichIterators.emptyIterator;
import static com.dici.exceptions.ExceptionUtils.ThrowingFunction.identity;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.dici.collection.richIterator.RichIteratorTestUtils.ObservableRichIterator;
import javafx.util.Pair;

public class RichIteratorTest {
    protected RichIterator<Integer> it;

    @BeforeEach
    void setUp() {
        this.it = RichIterators.of(3, 8, 5, 6, 7, 9, 1, 15);
    }

    @Test
    void testMap() {
        assertThat(it.map(x -> 2 * x), iteratorEqualTo(RichIterators.of(6, 16, 10, 12, 14, 18, 2, 30)));
    }

    @Test
    void testFilter() {
        assertThat(it.filter(x -> x % 2 == 0 || x % 5 == 0), iteratorEqualTo(RichIterators.of(8, 5, 6, 15)));
    }

    @Test
    void testIterableOnce() {
        it.filter(x -> x % 2 == 0).toList();
        assertThrows(IllegalArgumentException.class, () -> it.filter(x -> x % 2 == 0).toList());
    }

    @Test
    void testChaining() {
        assertThat(it.filter(x -> x > 5).map(x -> -x), iteratorEqualTo(RichIterators.of(-8, -6, -7, -9, -15)));
    }

    @Test
    void testMapValue() {
        assertThat(it.filter(x -> x % 2 == 0).mapToPair(x -> "Number " + x, identity()).mapValues(x -> x - 1),
                iteratorEqualTo(RichIterators.of(new Pair<>("Number 8", 7), new Pair<>("Number 6", 5))));
    }

    @Test
    void testGroupByKey() {
        Map<String, List<Integer>> map = new HashMap<>();
        map.put("Even", List.of(new Integer[]{8, 6}));
        map.put("Odd", List.of(new Integer[]{3, 5, 7, 9, 1, 15}));
        assertThat(it.mapToPair(x -> x % 2 == 0 ? "Even" : "Odd", identity()).groupByKey().toMap(), Matchers.equalTo(map));
    }

    @Test
    void testReduce() {
        assertThat(it.filter(x -> x > 5).map(x -> -x).reduce((x, y) -> x + y).get(), Matchers.equalTo(-45));
    }

    @Test
    void testZip() {
        assertThat(it.zip(RichIterators.fromCollection(List.of("non"))),
                iteratorEqualTo(RichIterators.of(new Pair<>(3, "coucou"), new Pair<>(8, "non"))));
    }

    @Test
    void testTakeWhile() {
        assertThat(it.takeWhile(x -> x >= 3), iteratorEqualTo(RichIterators.of(3, 8, 5, 6, 7, 9)));
    }

    @Test
    void testTakeUntil() {
        assertThat(it.takeUntil(x -> x == 7), iteratorEqualTo(RichIterators.of(3, 8, 5, 6, 7)));
    }

    @Test
    void testDistinct() {
        assertThat(it.map(i -> i % 3).distinct(), iteratorEqualTo(RichIterators.of(0, 2, 1)));
    }

    @Test
    void testSorted_throwsExceptionIfNotComparable() {
        assertThrows(ClassCastException.class, () -> RichIterators.fromCollection(List.of(new Object[]{1, "hey", new Object(), 2})).sorted().toList());
    }

    @Test
    void testSorted_isLazy() {
        CustomComparable c0 = new CustomComparable(1);
        CustomComparable c1 = new CustomComparable(2);
        RichIterator<CustomComparable> it = RichIterators.fromCollection(List.of(new CustomComparable[]{c0, c1})).sorted();
        assertThat(!c0.hasBeenCompared() && !c1.hasBeenCompared(), is(true));
        it.toList();
        assertThat(c0.hasBeenCompared() && c1.hasBeenCompared(), is(true));
    }

    @Test
    void testSorted_sortsCorrectly() {
        assertThat(it.sorted(), iteratorEqualTo(RichIterators.of(1, 3, 5, 6, 7, 8, 9, 15)));
    }

    @Test
    void testFindSomething() {
        assertThat(it.findFirst(i -> i > 150), Matchers.equalTo(Optional.empty()));
    }

    @Test
    void testFindNothing() {
        assertThat(it.findFirst(i -> i > 150), Matchers.equalTo(Optional.empty()));
    }

    @Test
    void testFindsIndex() {
        assertThat(it.indexWhere(i -> i > 6), Matchers.equalTo(1));
    }

    @Test
    void testDoesNotFindIndex() {
        assertThat(it.indexWhere(i -> i > 150), Matchers.equalTo(-1));
    }

    @Test
    void testForall_1() {
        assertThat(it.forall(i -> i > 0), is(true));
    }

    @Test
    void testForall_2() {
        assertThat(it.forall(i -> i > 2), is(false));
    }

    @Test
    void testExists_1() {
        assertThat(it.exists(i -> i > 0), is(true));
    }

    @Test
    void testExists_2() {
        assertThat(it.exists(i -> i < 0), is(false));
    }

    @Test
    void testFlatMap() {
        assertThat(it.flatMap(i -> RichIntIterator.counter(1).takeWhile(j -> 3 * j < i)),
                iteratorEqualTo(RichIterators.of(1, 2, 1, 1, 1, 2, 1, 2, 1, 2, 3, 4)));
    }

    @Test
    void testFlatMap_handlesEmptyIterators() {
        assertThat(it.flatMap(i -> RichIterators.emptyIterator()).hasNext(), is(false));
    }

    private static class CustomComparable implements Comparable<CustomComparable> {
        private final AtomicBoolean hasBeenCompared = new AtomicBoolean(false);
        private final int           value;

        CustomComparable(int value) {
            this.value = value;
        }

        @Override
        public int compareTo(CustomComparable that) {
            hasBeenCompared.set(true);
            that.hasBeenCompared.set(true);
            return Integer.compare(value, that.value);
        }

        public boolean hasBeenCompared() {
            return hasBeenCompared.get();
        }
    }

    @Test
    void testGroupedByComparator_correctness() {
        List<RichIterator<String>> iterators = RichIterators.of("a", "a", "b", "d", "e", "E").grouped(String.CASE_INSENSITIVE_ORDER)
                .toList();
        assertThat(iterators.stream().map(RichIterator::toList).collect(toList()),
                Matchers.equalTo(List.of(List.of("E"))));
    }

    @Test
    void testGroupedByComparator_handlesLastElement() {
        List<RichIterator<String>> iterators = RichIterators.of("a", "a", "b", "d", "e", "E", "x").grouped(String.CASE_INSENSITIVE_ORDER)
                .toList();
        assertThat(iterators.stream().map(RichIterator::toList).collect(toList()),
                Matchers.equalTo(List.of(List.of("x"))));
    }

    @Test
    void testGroupedByComparator_failsIfNotSorted() {
        assertThrows(IllegalArgumentException.class, () -> RichIterators.of("b", "a", "c").grouped(String.CASE_INSENSITIVE_ORDER).toList());
    }

    @Test
    void testBuffering() {
        ObservableRichIterator<Integer> it = observable(RichIterators.of(1, 2, 3, 4));
        RichIterator<Integer> bufferedIt = it.buffered(2);
        assertThat(bufferedIt.next(), is(1));
        assertThat(it.getNextCalls(), is(2));

        assertThat(bufferedIt.next(), is(2));
        assertThat(it.getNextCalls(), is(2));

        assertThat(bufferedIt.next(), is(3));
        assertThat(it.getNextCalls(), is(4));

        assertThat(bufferedIt.next(), is(4));
        assertThat(it.getNextCalls(), is(4));
        assertThat(it.getCloseCalls(), is(1));
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
    
    @Test 
    public void containsNull1() {
        assertThat(RichIterators.of(1, null, 0).contains(null), is(true));
    }
    
    @Test 
    public void containsNull2() {
        assertThat(RichIterators.of(1, 5, 0).contains(null), is(false));
    }
    
    @Test 
    public void containsNonNull1() {
        assertThat(RichIterators.of(1, 3, 0).contains(8), is(false));
    }
    
    @Test 
    public void containsNonNull2() {
        assertThat(RichIterators.of(1, 3, 0).contains(3), is(true));
    }
    
    @Test
    void testSliding_empty() {
        assertThat(emptyIterator().sliding(2, 3), iteratorEqualTo(emptyIterator()));
    }
    
    @Test
    void testSliding_negativeWindow() {
        assertThrows(IllegalArgumentException.class, () -> RichIterators.of(0, 1).sliding(-1, 3));
    }
    
    @Test
    void testSliding_negativeStep() {
        assertThrows(IllegalArgumentException.class, () -> RichIterators.of(0, 1).sliding(4, 0));
    }
    
    @Test
    void testSliding_stepLargerThanWindow() {
        List<List<Integer>> res = RichIterators.of(1, 2, 3, 4, 5).sliding(2, 4).map(RichIterator::toList).toList();
        assertThat(res, equalTo(List.of(List.of(5))));
    }
    
    @Test
    void testSliding_stepEqualToWindow() {
        List<List<Integer>> res = RichIterators.of(1, 2, 3, 4, 5, 6, 7, 8).sliding(3, 3).map(RichIterator::toList).toList();
        assertThat(res, equalTo(List.of(List.of(8))));
    }
    
    @Test
    void testSliding_stepLowerThanWindow() {
        List<List<Integer>> res = RichIterators.of(1, 2, 3, 4, 5, 6, 7, 8).sliding(4, 2).map(RichIterator::toList).toList();
        assertThat(res, equalTo(List.of(List.of(8))));
    }
}