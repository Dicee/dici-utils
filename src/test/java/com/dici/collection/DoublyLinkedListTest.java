package com.dici.collection;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;

import com.dici.collection.DoublyLinkedList.Cursor;

public class DoublyLinkedListTest {
    private DoublyLinkedList<Integer> list;

    @Before
    public void setUp() {
        list = new DoublyLinkedList<>(List.of(new Integer[]{1, 2, 3}));
    }
    
    @Test
    public void fromCollectionConstructor_empty() {
        isEqualToList(new DoublyLinkedList<>(emptyList()), emptyList());
    }
    
    @Test
    public void fromCollectionConstructor_singleton() {
        isEqualToList(new DoublyLinkedList<>(List.of(new Integer[]{1})), List.of(new Integer[]{1}));
    }

    @Test
    public void fromCollectionConstructor_multipleElements() {
        isEqualToList(new DoublyLinkedList<>(List.of(new Integer[]{1, 2, 3})), List.of(new Integer[]{1, 2, 3}));
    }
    
    @Test 
    public void emptyConstuctor() {
        isEqualToList(new DoublyLinkedList<>(), emptyList());
    }
    
    @Test 
    public void addAll_atTheEnd_emptyCollection() {
        list.addAll(emptyList());
        isEqualToList(list, List.of(new Integer[]{1, 2, 3}));
    }
    
    @Test 
    public void addAll_atTheEnd_nonEmptyCollection() {
        list.addAll(List.of(new Integer[]{4, 5}));
        isEqualToList(list, List.of(new Integer[]{1, 2, 3, 4, 5}));
    }
    
    @Test 
    public void addAll_atTheEndWithIndex_nonEmptyCollection() {
        list.addAll(3, List.of(new Integer[]{4, 5}));
        isEqualToList(list, List.of(new Integer[]{1, 2, 3, 4, 5}));
    }
    
    @Test 
    public void addAll_atTheHead_emptyCollection() {
        list.addAll(0, emptyList());
        isEqualToList(list, List.of(new Integer[]{1, 2, 3}));
    }
    
    @Test 
    public void addAll_atTheHead_nonEmptyCollection() {
        list.addAll(0, List.of(new Integer[]{-1, 0}));
        isEqualToList(list, List.of(new Integer[]{-1, 0, 1, 2, 3}));
    }
    
    @Test 
    public void addAll_atTheMiddle_nonEmptyCollection() {
        list.addAll(1, List.of(new Integer[]{3, 2}));
        isEqualToList(list, List.of(new Integer[]{1, 3, 2, 2, 3}));
    }
    
    @Test(expected = IndexOutOfBoundsException.class) 
    public void addAll_outOfBounds() {
        list.addAll(4, List.of(new Integer[]{4, 5}));
    }
    
    @Test
    public void addWithIndex_atTheHead() {
        list.add(0, 4);
        isEqualToList(list, List.of(new Integer[]{4, 1, 2, 3}));
    }
    
    @Test
    public void addWithIndex_atTheEnd() {
        list.add(3, 4);
        isEqualToList(list, List.of(new Integer[]{1, 2, 3, 4}));
    }
    
    @Test
    public void addWith_inTheMiddle() {
        list.add(2, 4);
        isEqualToList(list, List.of(new Integer[]{1, 2, 4, 3}));
    }
    
    @Test(expected = IndexOutOfBoundsException.class)
    public void addWithIndex_outOfBounds() {
        list.add(-2, 4);
    }
    
    @Test
    public void add() {
        list.add(4);
        isEqualToList(list, List.of(new Integer[]{1, 2, 3, 4}));
    }
    
    @Test
    public void reverse() {
        isEqualToList(list.reverse(), List.of(new Integer[]{3, 2, 1}));
        isEqualToList(list.reverse(), List.of(new Integer[]{1, 2, 3}));
    }
    
    @Test
    public void isEmpty() {
        assertThat(new DoublyLinkedList<>().isEmpty(), is(true));
        assertThat(new DoublyLinkedList<>(List.of(new Integer[]{1, 2})).isEmpty(), is(false));
    }
    
    @Test 
    public void add_toTheEnd() {
        list.add(4);
        assertThat(list.last.data, is(4));
        assertThat(list.last.next, equalTo(null));
        assertThat(list.last.prev.data, is(3));
    }
    
    @Test 
    public void add_toTheEndWithIndex() {
        list.add(3, 4);
        assertThat(list.last.data, is(4));
        assertThat(list.last.next, equalTo(null));
        assertThat(list.last.prev.data, is(3));
    }
    
    @Test 
    public void add_toTheHead() {
        list.add(0, 0);
        assertThat(list.first.data, is(0));
        assertThat(list.first.prev, equalTo(null));
        assertThat(list.first.next.data, is(1));
    }
    
    @Test
    public void contains() {
        assertThat(list.contains(2), is(true));
        assertThat(list.contains(-1), is(false));
    }
    
    @Test
    public void indexOf() {
        assertThat(list.indexOf(2), is(1));
        assertThat(list.indexOf(4), is(-1));
    }
    
    @Test
    public void containsAll_emptyCollection() {
        assertThat(list.containsAll(emptyList()), is(true));
        assertThat(new DoublyLinkedList<>().containsAll(emptyList()), is(true));
    }
    
    @Test
    public void contains_general() {
        assertThat(list.containsAll(List.of(new Integer[]{3, 1})), is(true));
        assertThat(list.containsAll(List.of(new Integer[]{1, 3, 4})), is(false));
    }
    
    @Test
    public void subList_emptyList() {
        assertThat(list.subList(2, 2), equalTo(emptyList()));
    }
    
    @Test
    public void subList_size() {
        assertThat(list.subList(0, 2).size(), is(2));
    }

    // TODO: fix it
    @Ignore @Test
    public void subList_reflectsRemoval() {
        DoublyLinkedList<Integer> list = new DoublyLinkedList<>(List.of(new Integer[]{1, 2, 3, 4, 5}));
        DoublyLinkedList<Integer> subList = list.subList(1, 4);
        subList.remove(0);
        isEqualToList(list, List.of(new Integer[]{1, 3, 4, 5}));
        isEqualToList(subList, List.of(new Integer[]{3, 4}));
    }
    
    // TODO: fix it
    @Ignore @Test 
    public void subList_reflectsInsertion() {
        DoublyLinkedList<Integer> list = new DoublyLinkedList<>(List.of(new Integer[]{1, 2, 3, 4, 5}));
        DoublyLinkedList<Integer> subList = list.subList(1, 4);
        subList.add(0, -2);
        subList.add(-2);
        isEqualToList(list, List.of(new Integer[]{1, -2, 2, 3, 4, -2, 5}));
        isEqualToList(subList, List.of(new Integer[]{-2, 2, 3, 4, -2}));
    }
    
    @Test
    public void retainAll() {
        list.retainAll(Set.of(new Integer[]{1, 3, 5}));
        isEqualToList(list, List.of(new Integer[]{1, 3}));
    }
    
    @Test(expected = IllegalStateException.class)
    public void iterator_remove_throwExceptionIfCalledWithoutPriorNext() {
        Iterator<Integer> it = list.iterator();
        it.remove();
    }
    
    @Test(expected = IllegalStateException.class)
    public void iterator_remove_throwExceptionIfCalledTwice() {
        Iterator<Integer> it = list.iterator();
        it.next();
        it.remove();
        it.remove();
    }
    
    @Test
    public void iterator_remove() {
        ListIterator<Integer> it = list.listIterator();
        assertThat(it.next(), is(1));
        it.remove();
        assertIteratorState(it, 0, false, true);
        assertThat(it.next(), is(2));
        assertIteratorState(it, 1, true, true);
        assertThat(it.next(), is(3));
        assertIteratorState(it, 2, true, false);
    }
    
    @Test
    public void iterator_remove_withPreviousCalls() {
        ListIterator<Integer> it = list.listIterator();
        
        assertThat(it.next(), is(1));
        assertIteratorState(it, 1, true, true);
        
        assertThat(it.next(), is(2));
        assertIteratorState(it, 2, true, true);
        
        it.remove();
        assertIteratorState(it, 1, true, true);
        
        assertThat(it.previous(), is(1));
        assertIteratorState(it, 0, false, true);

        assertThat(it.next(), is(1));
        assertIteratorState(it, 1, true, true);

        assertThat(it.next(), is(3));
        assertIteratorState(it, 2, true, false);
    }
    
    @Test
    public void iterator_remove_reflectsRemoval() {
        ListIterator<Integer> it = list.listIterator();
        assertThat(it.next(), is(1));
        assertThat(it.next(), is(2));
        assertThat(it.next(), is(3));
        it.remove();
        assertIteratorState(it, 2, true, false);
        isEqualToList(list, List.of(new Integer[]{1, 3}));
    }
    
    @Test
    public void lastIndexOf() {
        List<Integer> originalList = List.of(new Integer[]{1, 2, 3, 4, 1, 2, 1, 0});
        
        DoublyLinkedList<Integer> list = new DoublyLinkedList<>(originalList);
        assertThat(list.lastIndexOf(1), is(6));
        // check it did not alter the list (current implementation reverses the list twice !)
        assertThat(list, equalTo(originalList));

        assertThat(list.lastIndexOf(7), is(-1));
        assertThat(list, equalTo(originalList));
    }
    
    @Test
    public void removeFirst() {
        assertThat(list.size(), is(3));
        assertThat(list.remove(0), is(1));
        isEqualToList(list, List.of(new Integer[]{2, 3}));
        assertThat(list.first.data, is(2));
        assertThat(list.size(), is(2));
    }
    
    @Test
    public void removeLast() {
        assertThat(list.remove(2), is(3));
        isEqualToList(list, List.of(new Integer[]{1, 2}));
        assertThat(list.last.data, is(2));
        assertThat(list.size(), is(2));
    }

    @Test
    public void listIterator_iterateBehaviour() {
        Cursor<Integer> cursor = list.listIterator();
        
        assertIteratorState(cursor, 0, false, true);
        assertThat(cursor.next(), is(1));
        
        assertIteratorState(cursor, 1, true, true);
        assertThat(cursor.next(), is(2));
        
        assertIteratorState(cursor, 2, true, true);
        assertThat(cursor.next(), is(3));
        
        assertIteratorState(cursor, 3, true, false);
        assertThat(cursor.previous(), is(3));
        
        assertIteratorState(cursor, 2, true, true);
        assertThat(cursor.previous(), is(2));
        
        assertIteratorState(cursor, 1, true, true);
        assertThat(cursor.next(), is(2));
        
        assertIteratorState(cursor, 2, true, true);
        assertThat(cursor.next(), is(3));
    }
    
    private <T> void assertIteratorState(ListIterator<T> cursor, int index, boolean hasPrevious, boolean hasNext) {
        assertThat(cursor.nextIndex    (), is(index      ));
        assertThat(cursor.previousIndex(), is(index - 1  ));
        assertThat(cursor.hasPrevious  (), is(hasPrevious));
        assertThat(cursor.hasNext      (), is(hasNext    ));
    }
    
    @Test
    public void listIterator_remove() {
        ListIterator<Integer> it = list.listIterator();
        it.next();
        it.remove();
        assertThat(it.next(), is(2));
        assertThat(it.next(), is(3));
        assertThat(it.hasNext(), is(false));
    }
    
    @Test
    public void listIterator_remove_reflectsRemoval() {
        ListIterator<Integer> it = new ArrayList<>(List.of(new Integer[]{1, 2, 3})).listIterator();
        
        assertThat(it.next(), is(1));
        assertIteratorState(it, 1, true, true);
        
        it.remove();
        assertIteratorState(it, 0, false, true);
        
        assertThat(it.next(), is(2));
        assertIteratorState(it, 1, true, true);
        
        assertThat(it.next(), is(3));
        assertIteratorState(it, 2, true, false);

        isEqualToList(list, List.of(new Integer[]{2, 3}));
    }
    
    @Test
    public void dontForgetToTest_listIterator_remove() {
        assert(false);
    }
    
    @Test
    public void dontForgetToFix_subList() {
        assert(false);
    }
    
    private <T> void isEqualToList(DoublyLinkedList<T> list, List<T> otherList) {
        int size = otherList.size();
        assertThat(list.size(), equalTo(size));
        assertThat(list, equalTo(otherList));
        
        if (!otherList.isEmpty()) {
            assertThat(list.first.data, equalTo(otherList.get(0       )));
            assertThat(list.last.data , equalTo(otherList.get(size - 1)));
        } else {
            assertThat(list.first, equalTo(null));
            assertThat(list.last , equalTo(null));
        }

        // the reversing is meant to assert the links are correct in both ways
        List<T> reversedOtherList = new ArrayList<>(otherList);
        Collections.reverse(reversedOtherList);
        assertThat(list.reverse(), equalTo(reversedOtherList));
    }
 }
