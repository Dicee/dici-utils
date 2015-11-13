package com.dici.collection;

import static com.dici.check.Check.notNull;
import static com.dici.collection.DoublyLinkedList.ListNode.biLinkNext;
import static java.util.Collections.singletonList;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import com.dici.check.Check;
import com.dici.strings.StringUtils;
import com.google.common.base.Objects;

/**
 * A doubly linked list which, in addition to implementing all java.util.List operations exposes its internals for
 * a highly flexible use. Use at your own risk.
 * @author Dici
 * @param <T> type of the data
 */
public class DoublyLinkedList<T> implements List<T> {
    public static enum Way { 
        FORWARD, BACKWARD;
    
        public Way opposite() {
            switch (this) {
                case FORWARD : return BACKWARD;
                case BACKWARD: return FORWARD;   
                default      : throw new EnumConstantNotPresentException(Way.class, name());
            }
        }
    }

    public static class ListNode<T> {
        public static <T> void biLinkNext(ListNode<T> node, ListNode<T> toLink) { biLink(node, toLink, Way.FORWARD) ; }
        public static <T> void biLinkPrev(ListNode<T> node, ListNode<T> toLink) { biLink(node, toLink, Way.BACKWARD); }
        public static <T> void biLink(ListNode<T> node, ListNode<T> toLink, Way way) {
            notNull(node).link(toLink, way);
            if (toLink != null) toLink.link(node, way.opposite());
        }
        
        public T data;
        public ListNode<T> next;
        public ListNode<T> prev;
        
        public ListNode(T data) { this(data, null, null); }
        public ListNode(T data, ListNode<T> prev, ListNode<T> next) {
            this.data = data;
            this.prev = prev;
            this.next = next;
            biLinkPrev(this, prev);
            biLinkNext(this, next);
        } 
        
        public void link(ListNode<T> node, Way way) {
            switch (way) {
                case FORWARD : next = node; break;
                case BACKWARD: prev = node; break;  
                default      : throw new EnumConstantNotPresentException(Way.class, way.name());
            }
        }

        public ListNode<T> copy() {
            return new ListNode<>(this.data, this.prev, this.next);
        }

        @Override public String toString() { return "ListNode(" + data + ")"; }
    }
    
    public static class Cursor<T> implements ListIterator<T> {
        private DoublyLinkedList<T> list;
        private ListNode<T> first;
        private ListNode<T> current;
        private ListNode<T> goal;
        private final Way  way;
        
        private int index;
        private boolean hasBeenRemoved;
        private boolean hasEof;
        
        public Cursor(DoublyLinkedList<T> list, ListNode<T> first, Way way) {
            this.list    = notNull(list);
            this.first   = first;
            this.current = first;
            this.goal    = goal(list, way);
            this.way     = notNull(way);
        }
        
        private ListNode<T> goal(DoublyLinkedList<T> list, Way way) { return list.defaultWay == way ? list.last : list.first; }

        @Override
        public T next() {
            hasBeenRemoved = false;
            if (!hasNext()) throw new NoSuchElementException();
            hasEof  = current == goal; 
            T data  = current.data;
            if (hasNext()) current = successor();
            index++;
            return data;
        }

        @Override
        public void remove() {
            if (hasBeenRemoved) throw new IllegalStateException("Cannot call remove on an iterator twice in a row");
            if (!hasNext()) throw new NoSuchElementException();
            list.removeNode(current);
            current = successor();
            hasBeenRemoved = true;
        }

        private ListNode<T> successor() {
            switch (way) {
                case FORWARD : return current.next;
                case BACKWARD: return current.prev;  
                default      : throw new EnumConstantNotPresentException(Way.class, way.name());
            }
        }

        @Override public boolean hasNext    () { return !hasEof; }
        @Override public boolean hasPrevious() { return current != first && current.prev != null; }
        
        @Override
        public T previous() { 
            if (!hasPrevious()) throw new NoSuchElementException();
            hasEof = false;
            return current.prev.data; 
        }

        public ListNode<T> current() { 
            if (hasBeenRemoved) throw new IllegalStateException("The current element has been removed and should not "
                    + "be accessed until next is called again");
            return current; 
        }

        @Override public int nextIndex    (   ) { return index + 1                        ; }
        @Override public int previousIndex(   ) { return index - 1                        ; }
        @Override public void set         (T t) { current.data = t                        ; }
        @Override public void add         (T t) { new ListNode<>(t, current, current.next); }
    }
    
    public ListNode<T> first;
    public ListNode<T> last;
    
    private int size;
    private Way defaultWay = Way.FORWARD;
    
    public DoublyLinkedList() { this(null, null, 0); }
    
    public DoublyLinkedList(ListNode<T> first, ListNode<T> last, int size) {
        Check.isFalse(first == null ^ last == null, "The first and last nodes are either both null (empty list) or both non-null");
        this.first = first;
        this.last  = last;
        Check.isTrue(size > 0 || first == null && last == null && size >= 0);
        this.size  = size;
    }
    
    public DoublyLinkedList(Collection<? extends T> collection) {
        Iterator<? extends T> it = notNull(collection).iterator();
        
        if (it.hasNext()) this.first = new ListNode<>(it.next());
        
        ListNode<T> current = this.first;
        while (it.hasNext()) current = new ListNode<>(it.next(), current, null);
        
        this.last = current;
        this.size = collection.size();
    }
    
    @Override public boolean add(T t) { return addAll(singletonList(t)); }
    @Override public void add(int index, T t) { addAll(index, singletonList(t)); }

    @Override
    public boolean addAll(Collection<? extends T> collection) { return addAll(last, collection); }

    @Override
    public boolean addAll(int index, Collection<? extends T> collection) { 
        // first.prev might not be null in the case of a subList
        return addAll(index == 0 ? first.prev : getNode(index - 1), collection); 
    }
    
    private boolean addAll(ListNode<T> node, Collection<? extends T> collection) {
        if (collection.isEmpty()) return true;
        
        DoublyLinkedList<T> newList = new DoublyLinkedList<T>(collection);
        if (size == 0) return copyList(newList);
        
        boolean isSubList = node != null && node.next == first;

        biLinkNext(newList.last, node == null ? first : node.next);
        if (node != null) biLinkNext(node, newList.first);
        
        // case of an insertion at head or at the head of a subList
        if (node == null || isSubList) first = newList.first;
        if (node == last             ) last  = newList.last;
        
        return true;
    }

    private boolean copyList(Collection<? extends T> collection) {
        DoublyLinkedList<T> newList = new DoublyLinkedList<T>(collection);
        first = newList.first;
        last  = newList.last;
        return true;
    }
    
    public DoublyLinkedList<T> reverse() {
        ListNode<T> tmp = first;
        first      = last;
        last       = tmp;
        defaultWay = defaultWay.opposite();
        return this;
    }

    @Override
    public void clear() { first = last = null; }

    @Override
    public boolean contains(Object o) { return indexOf(o) != -1; }

    @Override
    public boolean containsAll(Collection<?> collection) { 
        for (Object o : collection)
            if (!contains(o)) return false;
        return true;
    }

    @Override
    public T get(int index) { return getNode(index).data; }
    
    public ListNode<T> getNode(int index) {
        checkInBounds(index);
        Cursor<T> cursor = closestCursor(index);
        for (int i = closestIndex(index); i > 0; i--) cursor.next();
        return cursor.current();
    }

    private void checkInBounds(int index) { Check.isBetween(0, index, size, new IndexOutOfBoundsException("Index out of bounds: " + index + " (size = " + size + ")")); }
    
    public Cursor<T> closestCursor(int index) {
        return index <= size/2 ? new Cursor<>(this, first, defaultWay) : new Cursor<>(this, last, defaultWay.opposite()); 
    }
    
    private int closestIndex(int index) { return index <= size/2 ? index : size - 1 - index; }
    
    @Override
    public int indexOf(Object o) {
        int index = 0;
        for (Object x : this) { 
            if (Objects.equal(o, x)) return index;
            index++;
        }
        return -1;
    }

    @Override public boolean isEmpty() { return size == 0; }
    @Override public Iterator<T> iterator() { return forwardCursor(); }
    public Cursor<T> forwardCursor() { return new Cursor<>(this, first, defaultWay); }
    public Cursor<T> backwardCursor() { return new Cursor<>(this, last, defaultWay.opposite()); }

    @Override
    public int lastIndexOf(Object o) {
        // Nyahahahahahahahahahaha
        int index = reverse().indexOf(o);
        reverse();
        return index == -1 ? -1 : size - 1 - index;
    }

    @Override public Cursor<T> listIterator(         ) { return listIterator(0)                               ; }
    @Override public Cursor<T> listIterator(int index) { return new Cursor<>(this, getNode(index), defaultWay); }

    @Override
    public boolean remove(Object o) {
        Cursor<T> cursor = forwardCursor();
        while (cursor.hasNext()) { 
            if (Objects.equal(cursor.current().data, o)) {
                removeNode(cursor.current());
                return true;
            }
            cursor.next();
        }
        return false;
    }

    @Override public T remove(int index) { return removeNode(getNode(index)); }
    
    private T removeNode(ListNode<T> node) {
        if (node == first) first = node.next;
        if (node == last ) last  = node.prev; 
        if (node.prev != null) biLinkNext(node.prev, node.next);
        return node.data;
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        boolean hasRemoved = false;
        for (Object o : collection) hasRemoved = remove(o) || hasRemoved;
        return hasRemoved;
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        boolean hasRemoved = false;
        
        Cursor<T> cursor = forwardCursor();
        while (cursor.hasNext()) {
            boolean remove = !collection.contains(cursor.current().data);
            hasRemoved     = hasRemoved || remove;
            if (remove) cursor.remove();
            cursor.next();
        }
        return hasRemoved;
    }

    @Override
    public T set(int index, T t) {
        Cursor<T> cursor = listIterator(index);
        T data = cursor.current().data;
        cursor.set(t);
        return data;
    }

    @Override public int size() { return size; }

    @Override
    public DoublyLinkedList<T> subList(int inf, int sup) {
        Check.isBetween(inf, sup, size + 1, new IndexOutOfBoundsException("Index out of bounds: " + sup + ", should be in between " + inf + " and " + size + " (excluded)"));
        if (sup == inf) return new DoublyLinkedList<>();
        ListNode<T> first = getNode(inf);
        ListNode<T> last  = sup == inf ? first : getNode(sup - 1);
        return new DoublyLinkedList<>(first, last, sup - inf);
    }

    @Override public Object[] toArray() { return toArray(new Object[size]); }

    @SuppressWarnings("unchecked")
    @Override
    public <S> S[] toArray(S[] arr) {
        int i = 0;
        for (T t : this) arr[i++] = (S) t;
        return arr;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o)                  return true;
        if (!(o instanceof Collection)) return false;
        Iterator<?> it     = iterator();
        Iterator<?> thatIt = ((Collection<?>) o).iterator();
        while (it.hasNext() && thatIt.hasNext())
            if (!Objects.equal(it.next(), thatIt.next())) return false;
        return it.hasNext() == thatIt.hasNext();
    }
    
    @Override
    public int hashCode() {
        return 0;
//        throw new UnsupportedOperationException();
    }
    
    @Override public String toString() { return StringUtils.join("[", ", ", "]", this); }
}