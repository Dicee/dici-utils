package com.dici.sort;

public interface SortAlgorithm<T extends Comparable<T>> {
	void sort(T[] arr);
	String getName();
}
