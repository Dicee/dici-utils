package com.dici.math;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.Iterator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PermutationGeneratorTest {
	private Permutation	perm;

	@BeforeEach
	void setUp() {
		this.perm = Permutation.fromDigits("3102");
	}
	
	@Test
	void doesNotMutatePermutation() {
		new PermutationGenerator(perm).iterator().next();
		assertThat(perm, equalTo(Permutation.fromDigits("3102")));
	}
	
	@Test
	void reachesTheEnd() {
		Iterator<Permutation> it = new PermutationGenerator(perm).iterator();
		assertThat(it.next(), equalTo(Permutation.fromDigits("3102")));
		assertThat(it.next(), equalTo(Permutation.fromDigits("3120")));
		assertThat(it.next(), equalTo(Permutation.fromDigits("3201")));
		assertThat(it.next(), equalTo(Permutation.fromDigits("3210")));
		assertThat(it.hasNext(), is(false));
	}
	
	@Test
	void goesBackward() {
		Iterator<Permutation> it = new PermutationGenerator(perm, true).iterator();
		assertThat(it.next(), equalTo(Permutation.fromDigits("3102")));
		assertThat(it.next(), equalTo(Permutation.fromDigits("3021")));
		assertThat(it.next(), equalTo(Permutation.fromDigits("3012")));
		assertThat(it.next(), equalTo(Permutation.fromDigits("2310")));
		assertThat(it.next(), equalTo(Permutation.fromDigits("2301")));
		assertThat(it.next(), equalTo(Permutation.fromDigits("2130")));
		assertThat(it.next(), equalTo(Permutation.fromDigits("2103")));
		assertThat(it.next(), equalTo(Permutation.fromDigits("2031")));
		assertThat(it.next(), equalTo(Permutation.fromDigits("2013")));
	}
	
	@Test
	void permutesData() {
		String[] data = { "0", "1", "2" };
		Iterator<String[]> it = new PermutationGenerator(Permutation.fromDigits("012")).generatePermutations(data);
		assertThat(it.next(), equalTo(new String[] { "0", "1", "2" }));
		assertThat(it.next(), equalTo(new String[] { "0", "2", "1" }));
		assertThat(it.next(), equalTo(new String[] { "1", "0", "2" }));
		assertThat(it.next(), equalTo(new String[] { "1", "2", "0" }));
		assertThat(it.next(), equalTo(new String[] { "2", "0", "1" }));
		assertThat(it.next(), equalTo(new String[] { "2", "1", "0" }));
	}
}
