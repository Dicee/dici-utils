package com.dici.math;

import com.dici.check.Check;
import com.dici.collection.ArrayUtils;

import java.util.*;

import static com.dici.check.Check.notNull;
import static com.dici.collection.ArrayUtils.reverse;
import static com.dici.collection.ArrayUtils.swap;
import static com.dici.math.Permutation.*;

public class PermutationGenerator implements Iterable<Permutation> {
	private final Permutation start;
	private final Permutation goal;
	private final Comparator<Integer> cmp;
	
	public PermutationGenerator(Permutation seed)                   { this(seed, false); }
	public PermutationGenerator(Permutation seed, boolean reversed) { this(seed, reversed ? identity(seed.length()) : reverseIdentity(seed.length())); }
	public PermutationGenerator(Permutation seed, Permutation goal) {
		Check.areEqual(notNull(seed).length(), notNull(goal).length(), "Inconsistent permutation length");
		this.cmp   = seed.compareTo(goal) > 0 ? Comparator.reverseOrder() : Integer::compare;
		this.goal  = goal;
		this.start = seed;
	}

	@Override
	public Iterator<Permutation> iterator() { return iterator(Optional.empty()); }

	public <T, C extends Collection<T>> Iterator<T[]> generatePermutations(C collection, Class<T> clazz) {
		return generatePermutations(collection.toArray(ArrayUtils.ofDim(clazz, collection.size())));
	}

	public <T> Iterator<T[]> generatePermutations(T[] data) { 
		Iterator<Permutation> it = iterator(Optional.of(data));
		return new Iterator<T[]>() {
			@Override public boolean hasNext() { return it.hasNext()   ; }
			@Override public T[]     next   () { it.next(); return data; }
		};
	}
	
	private <T> Iterator<Permutation> iterator(Optional<T[]> data) {
		return new Iterator<Permutation>() {
			private Integer[]	current = Arrays.copyOf(start.perm, start.perm.length);
			private boolean		init	= true;
			private int			index	= 0;
			private boolean		ended   = updateHasNext();
			
			@Override
			public boolean hasNext() { return !ended; }

			@Override
			public Permutation next() {
				if (ended) throw new NoSuchElementException();
				
				if (init) {
					init = false;
					return new Permutation(current);
				}
				
				int n = current.length;
				Permutation result;
				
				if (index != 0 && sortedRight(index)) {
					while (index > 0 && cmp.compare(current[index - 1], current[index]) > 0) index--;
					int k = index - 1;
					while (index < n - 1 && cmp.compare(current[k], current[index + 1]) < 0) index++;
					
					swap(current, index, k);
					reverse(current, k + 1, current.length - 1);
					data.ifPresent(arr -> swap(arr, index, k));
					data.ifPresent(arr -> reverse(arr, k + 1, current.length - 1));
					
					index  = k;
					result = unsafePermutation(current);
				} else {
					index++;
					result = next();
				}
				updateHasNext();
				return result;
			}
			
			private boolean sortedRight(int index) {
				for (int i=index ; i<current.length - 1 ; i++)
					if (cmp.compare(current[i],current[i + 1]) < 0)
						return false;
				return true;
			}
			
			private boolean updateHasNext() { return ended = Arrays.equals(current, goal.perm); }
		};
	}
}
