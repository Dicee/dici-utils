package com.dici.sort;

public interface SortAlgorithm<T extends Comparable<T>> {
	public void sort(T[] arr);
	public String getName();
}
