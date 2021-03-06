package com.dici.sort;

import static com.dici.math.MathUtils.lowerOrEqual;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.dici.sort.InsertionSort;
import com.dici.sort.MergeSort;
import com.dici.sort.QuickSort;
import com.dici.sort.SelectionSort;
import com.dici.sort.SortAlgorithm;

public class CompareSort {
	public static void main(String[] args) {
		int n = 100000;
		Random rd = new Random();
		Integer[] arr = new Integer[n];
		for (int i=0 ; i<n ; i++)
			arr[i] = rd.nextInt(n);
		
		List<SortAlgorithm<Integer>> algorithms = Arrays.asList(new SelectionSort<>(),new InsertionSort<>(),new QuickSort<>(),new MergeSort<>());
		for (SortAlgorithm<Integer> algorithm : algorithms) {
			Integer[] test = Arrays.copyOf(arr,arr.length);
			long start     = System.nanoTime();
			algorithm.sort(test);
			checkSorted(test);
			System.out.println(String.format("Time elapsed with %s : %.2f ms", algorithm.getName(),(System.nanoTime() - start)/1e6));
		}
	}

	private static void checkSorted(Integer[] arr) {
		for (int i=1 ; i<arr.length ; i++) 
			if (!lowerOrEqual(arr[i - 1],arr[i]))
				throw new AssertionError(String.format("Array not well sorted : %d > %d in %s",arr[i - 1],arr[i],Arrays.toString(Arrays.copyOfRange(arr,0,i + 2))));
	}
}