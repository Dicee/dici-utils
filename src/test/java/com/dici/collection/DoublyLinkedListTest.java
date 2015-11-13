package com.dici.collection;

import static com.dici.collection.CollectionUtils.listOf;
import static com.dici.collection.CollectionUtils.setOf;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.dici.collection.DoublyLinkedList.Cursor;

public class DoublyLinkedListTest {
    private DoublyLinkedList<Integer> list;

    @Before
    public void setUp() {
        list = new DoublyLinkedList<>(listOf(1, 2, 3));
    }
    
    @Test
    public void fromCollectionConstructor_empty() {
        assertThat(new DoublyLinkedList<>(emptyList()), equalTo(emptyList()));
    }
    
    @Test
    public void fromCollectionConstructor_singleton() {
        assertThat(new DoublyLinkedList<>(listOf(1)), equalTo(listOf(1)));
    }

    @Test
    public void fromCollectionConstructor_multipleElements() {
        assertThat(new DoublyLinkedList<>(listOf(1, 2, 3)), equalTo(listOf(1, 2, 3)));
    }
    
    @Test 
    public void emptyConstuctor() {
        assertThat(new DoublyLinkedList<>(), equalTo(emptyList()));
    }
    
    @Test 
    public void addAll_atTheEnd_emptyCollection() {
        list.addAll(emptyList());
        assertThat(list, equalTo(listOf(1, 2, 3)));
    }
    
    @Test 
    public void addAll_atTheEnd_nonEmptyCollection() {
        list.addAll(listOf(4, 5));
        assertThat(list, equalTo(listOf(1, 2, 3, 4, 5)));
    }
    
    @Test 
    public void addAll_atTheEndWithIndex_nonEmptyCollection() {
        list.addAll(3, listOf(4, 5));
        assertThat(list, equalTo(listOf(1, 2, 3, 4, 5)));
    }
    
    @Test 
    public void addAll_atTheHead_emptyCollection() {
        list.addAll(0, emptyList());
        assertThat(list, equalTo(listOf(1, 2, 3)));
    }
    
    @Test 
    public void addAll_atTheHead_nonEmptyCollection() {
        list.addAll(0, listOf(-1, 0));
        assertThat(list, equalTo(listOf(-1, 0, 1, 2, 3)));
    }
    
    @Test 
    public void addAll_atTheMiddle_nonEmptyCollection() {
        list.addAll(1, listOf(3, 2));
        assertThat(list, equalTo(listOf(1, 3, 2, 2, 3)));
    }
    
    @Test(expected = IndexOutOfBoundsException.class) 
    public void addAll_outOfBounds() {
        list.addAll(4, listOf(4, 5));
    }
    
    @Test
    public void reverse() {
        assertThat(list.reverse(), equalTo(listOf(3, 2, 1)));
        assertThat(list.reverse(), equalTo(listOf(1, 2, 3)));
    }
    
    @Test
    public void isEmpty() {
        assertThat(new DoublyLinkedList<>().isEmpty(), is(true));
        assertThat(new DoublyLinkedList<>(listOf(1, 2)).isEmpty(), is(false));
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
        assertThat(list.containsAll(listOf(3, 1)), is(true));
        assertThat(list.containsAll(listOf(1, 3, 4)), is(false));
    }
    
    @Test
    public void closestCursor_1() {
        DoublyLinkedList<Integer> list = new DoublyLinkedList<>(listOf(1, 2, 3, 4, 5, 6, 7));
        Cursor<Integer> cursor = list.closestCursor(2);
        assertThat(cursor.next(), is(1));
    }
    
    @Test
    public void closestCursor_2() {
        DoublyLinkedList<Integer> list = new DoublyLinkedList<>(listOf(1, 2, 3, 4, 5, 6, 7));
        Cursor<Integer> cursor = list.closestCursor(5);
        assertThat(cursor.next(), is(7));
    }
    
    @Test
    public void subList_emptyList() {
        assertThat(list.subList(2, 2), equalTo(emptyList()));
    }
    
    @Test
    public void subList_size() {
        assertThat(list.subList(0, 2).size(), is(2));
    }

    @Test
    public void subList_reflectsRemoval() {
        DoublyLinkedList<Integer> list = new DoublyLinkedList<>(listOf(1, 2, 3, 4, 5));
        DoublyLinkedList<Integer> subList = list.subList(1, 4);
        subList.remove(0);
        // the reverse are meant to prove the links are correct in both ways
        assertThat(list, equalTo(listOf(1, 3, 4, 5)));
        assertThat(list.reverse(), equalTo(listOf(5, 4, 3, 1)));
        assertThat(subList, equalTo(listOf(3, 4)));
        assertThat(subList.reverse(), equalTo(listOf(4, 3)));
    }
    
    @Test 
    public void subList_reflectsInsertion() {
        DoublyLinkedList<Integer> list = new DoublyLinkedList<>(listOf(1, 2, 3, 4, 5));
        DoublyLinkedList<Integer> subList = list.subList(1, 4);
        subList.add(0, -2);
        subList.add(-2);
        // the reverse are meant to prove the links are correct in both ways
        assertThat(list, equalTo(listOf(1, -2, 2, 3, 4, -2, 5)));
        assertThat(list.reverse(), equalTo(listOf(5, -2, 4, 3, 2, -2, 1)));
        assertThat(subList, equalTo(listOf(-2, 2, 3, 4, -2)));
        assertThat(subList.reverse(), equalTo(listOf(-2, 4, 3, 2, -2)));
    }
    
    @Test
    public void retainAll() {
        list.retainAll(setOf(1, 3, 5));
        assertThat(list, equalTo(listOf(1, 3)));
    }
    
    @Test(expected = IllegalStateException.class)
    public void iterator_remove_throwExceptionIfCalledTwice() {
        Iterator<Integer> it = list.iterator();
        it.remove();
        it.remove();
    }
    
    @Test
    public void iterator_remove() {
        Iterator<Integer> it = list.iterator();
        it.remove();
        assertThat(it.next(), is(2));
        assertThat(it.next(), is(3));
        assertThat(it.hasNext(), is(false));
    }
    
    @Test
    public void iterator_remove_reflectsRemoval() {
        Iterator<Integer> it = list.iterator();
        assertThat(it.next(), is(1));
        it.remove();
        assertThat(it.next(), is(3));
        assertThat(it.hasNext(), is(false));
        assertThat(list, equalTo(listOf(1, 3)));
        assertThat(list.reverse(), equalTo(listOf(3, 1)));
    }
    
    @Test
    public void lastIndexOf() {
        List<Integer> originalList = listOf(1, 2, 3, 4, 1, 2, 1, 0);
        
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
        assertThat(list, equalTo(listOf(2, 3)));
        assertThat(list.first.data, is(2));
        assertThat(list.size(), is(2));
    }
    
    @Test
    public void removeLast() {
        assertThat(list.remove(2), is(3));
        assertThat(list, equalTo(listOf(1, 2)));
        assertThat(list.last.data, is(2));
        assertThat(list.size(), is(2));
    }
    
//    @Test
//    public void dontForgetToTest_listIterator() {
//        assert(false);
//    }
 }
