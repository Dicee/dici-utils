package com.dici.collection;

import static com.dici.check.Check.notNull;
import static com.dici.collection.DoublyLinkedList.ListNode.biLinkNext;
import static java.util.Collections.singletonList;

import java.util.ArrayList;
import java.util.Arrays;
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
    public enum Way {
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
        private static <T> void checkInBounds(Integer index, DoublyLinkedList<T> list) { 
            int size = list.size();
            if (index == -1 && size == 0) return;
            Check.isBetween(0, index, size, new IndexOutOfBoundsException("Index out of bounds: " + index + " (size = " + size + ")")); 
        }

        private DoublyLinkedList<T> list;
        private ListNode<T> current;
        private final Way way;
        
        private int index;
        private boolean isRemovable;
        
        public Cursor(DoublyLinkedList<T> list, Way way) { this(list, notNull(list).endIndex(notNull(way)), way); }
        
        public Cursor(DoublyLinkedList<T> list, int index, Way way) {
            this.list        = notNull(list);
            this.way         = notNull(way);
            this.isRemovable = false;
            initCurrent(list, index);
        }
        
        private void initCurrent(DoublyLinkedList<T> list, int index) {
            checkInBounds(index, list);
            this.index   = index;
            this.current = list.closestEnd(index);
            Way way = this.current == list.first ? Way.FORWARD : Way.BACKWARD;
            System.err.println(way + " " + list.closestIndex(index));
            for (int i = list.closestIndex(index); i > 0; i--) current = next(way);
            System.err.println("------------------- end " + current + "-----------------------------");
        }
        
        @Override
        public T next() {
            if (!hasNext()) throw new NoSuchElementException();
            isRemovable = true;
            T data         = current.data;
            current        = successor();
            index++;
            return data;
        }
        
        @Override
        public T previous() { 
            if (!hasPrevious()) throw new NoSuchElementException();
            isRemovable = true;
            if (hasNext()) current = predecessor();
            index--;
            return current.data; 
        }

        @Override
        public void remove() {
            if (!isRemovable) throw new IllegalStateException("Cannot call remove on an iterator twice in a row");
            if (!hasPrevious()) throw new NoSuchElementException();
            list.removeNode(predecessor());
            isRemovable = false;
            index--;
        }

        private ListNode<T> successor  () { return notNull(next(way           )); }
        private ListNode<T> predecessor() { return notNull(next(way.opposite())); }
        private ListNode<T> next(Way way) {
            switch (way) {
                case FORWARD : return successorOrItself(current, current.next);
                case BACKWARD: return successorOrItself(current, current.prev);  
                default      : throw new EnumConstantNotPresentException(Way.class, way.name());
            }
        }

        private ListNode<T> successorOrItself(ListNode<T> current, ListNode<T> successorOrNull) {
            return successorOrNull == null ? current : successorOrNull;
        }

        @Override public boolean hasNext    () { return nextIndex() != -1 && nextIndex() < list.size(); }
        @Override public boolean hasPrevious() { return previousIndex() >= 0; }

        public ListNode<T>   currentNode  (   ) { return current            ; }
        @Override public int nextIndex    (   ) { return index              ; }
        @Override public int previousIndex(   ) { return index - 1          ; }
        @Override public void set         (T t) { current.data = t          ; }
        @Override public void add         (T t) { list.addToNode(current, t); }
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
    
    public int endIndex(Way way) { return isEmpty() ? -1 : defaultWay == way ? 0 : size() - 1; }
    public int closestIndex(int index) { return index <= size()/2 ? index : size() - 1 - index; }
    public ListNode<T> closestEnd(int index) { return index <= size()/2 ? first : last; }
    
    @Override public boolean add(T t) { return addToNode(last, t); }
    @Override public void add(int index, T t) { addAll(index, singletonList(t)); }

    private boolean addToNode(ListNode<T> node, T t) {
        ListNode<T> newNode = new ListNode<>(t, notNull(node), node.next);
        if (node == last) last = newNode;
        size++;
        return true;
    }
    
    @Override
    public boolean addAll(Collection<? extends T> collection) { return addAll(last, collection); }

    @Override
    public boolean addAll(int index, Collection<? extends T> collection) { 
        // first.prev might not be null in the case of a subList
        return addAll(index == 0 ? first.prev : getNode(index - 1), collection); 
    }
    
    private boolean addAll(ListNode<T> node, Collection<? extends T> collection) {
        if (collection.isEmpty()) return true;

        size += collection.size();

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
        return listIterator(index).currentNode();
    }

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
    public Cursor<T> forwardCursor() { return new Cursor<>(this, defaultWay); }
    public Cursor<T> backwardCursor() { return new Cursor<>(this, defaultWay.opposite()); }

    @Override
    public int lastIndexOf(Object o) {
        // Nyahahahahahahahahahaha
        int index = reverse().indexOf(o);
        reverse();
        return index == -1 ? -1 : size - 1 - index;
    }

    @Override public Cursor<T> listIterator(         ) { return listIterator(0)                      ; }
    @Override public Cursor<T> listIterator(int index) { return new Cursor<>(this, index, defaultWay); }

    @Override
    public boolean remove(Object o) {
        Cursor<T> cursor = forwardCursor();
        while (cursor.hasNext()) { 
            if (Objects.equal(cursor.currentNode().data, o)) {
                removeNode(cursor.currentNode());
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
        size--;
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
            boolean remove = !collection.contains(cursor.next());
            hasRemoved     = hasRemoved || remove;
            if (remove) cursor.remove();
        }
        return hasRemoved;
    }

    @Override
    public T set(int index, T t) {
        Cursor<T> cursor = listIterator(index);
        T data = cursor.currentNode().data;
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
    
    public static void main(String[] args) {
        ListIterator<Integer> listIterator = new ArrayList<>(Arrays.asList(1, 2, 3)).listIterator(0);
       System.out.println(listIterator.next());
       System.out.println(listIterator.previous());
       System.out.println(listIterator.next());
       System.out.println(listIterator.previous());
       System.out.println(listIterator.next());
       System.out.println(listIterator.previous());
//        System.out.println(listIterator.nextIndex());
//        listIterator.remove();
//        System.out.println(listIterator.nextIndex());
//        System.out.println(listIterator.previousIndex());
//        listIterator.next();
//        System.out.println(listIterator.previousIndex());
//        System.out.println(listIterator.next());
    }
}