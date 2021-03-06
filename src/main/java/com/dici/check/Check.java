package com.dici.check;

import java.util.Collection;

public final class Check {
	private static final String	SHOULD_BE_TRUE			    = "This expression should be true";
	private static final String	SHOULD_BE_FALSE			    = "This expression should be false";
	private static final String	SHOULD_NOT_BE_NULL		    = "This variable should not be null";
	private static final String	SHOULD_BE_NULL			    = "This variable should be null";
	private static final String	SHOULD_NOT_BE_EQUAL		    = "These objects should not be equal";
	private static final String	SHOULD_NOT_BE_EMPTY		    = "This array should not be empty";
	private static final String	SHOULD_NOT_BE_BLANK		    = "This string should not be blank";
	private static final String	SHOULD_BE_GREATER		    = "The first parameter should be greater than the second one";
	private static final String	SHOULD_BE_GREATER_OR_EQUAL  = "The first parameter should be greater (or equal) than the second one";
	private static final String SHOULD_BE_POSITIVE		    = "This variable should be positive";
	private static String SHOULD_BE_BETWEEN(int low, int high) { return String.format("This number should be between %d and %d", low, high); }
	private static String SHOULD_BE_EQUAL(Object o1, Object o2) { return String.format("Expected : %s, got: %s", o1, o2); }
	private static String SHOULD_BE_DIFFERENT(Object o1, Object o2) { return String.format("%s should be different from %s", o1, o2); }
	private static String SHOULD_NOT_BE_NEGATIVE(long n) { return String.format("Expected strictly positive, got %d", n); }
	
	private Check() { }
	
	public static <T> T[] notEmpty(T[] arr) { return notEmpty(arr,SHOULD_NOT_BE_EMPTY); }
	public static <T> T[] notEmpty(T[] arr, String msg) { check(arr.length != 0,msg); return arr; }
	public static <T, C extends Collection<T>> C notEmpty(C collection, String msg) { check(notNull(collection).size() != 0, msg); return collection; 	}
	public static <T, C extends Collection<T>> C notEmpty(C collection) { return notEmpty(collection, SHOULD_NOT_BE_EMPTY); 	}
	
	public static <T> void isNull(T t) { isNull(t, SHOULD_BE_NULL); }
	public static <T> void isNull(T t, String msg) { check(t == null,msg); }
	
	public static <T> T notNull(T t) { return notNull(t,SHOULD_NOT_BE_NULL); }
	public static <T> T notNull(T t, String msg) {
		check(t != null,msg);
		return t;
	}
	
	public static boolean isTrue (boolean b) { return isTrue(b,SHOULD_BE_TRUE); } 
	public static boolean isTrue (boolean b, String msg) { check(b,msg); return b; } 
	public static boolean isFalse(boolean b) { return isFalse(b,SHOULD_BE_FALSE); }
	public static boolean isFalse(boolean b, String msg) { return isTrue(!b,msg); }

	public static int isPositive(int n) { return isPositive(n, SHOULD_BE_POSITIVE); } 
	public static int isPositive(int n, String msg) { check(n > 0, msg); return n; } 
	
	public static int notNegative(int n) { return notNegative(n, SHOULD_NOT_BE_NEGATIVE(n)); } 
    public static int notNegative(int n, String msg) { check(n >= 0, msg); return n; } 
	
    public static long notNegative(long n) { return notNegative(n, SHOULD_NOT_BE_NEGATIVE(n)); } 
    public static long notNegative(long n, String msg) { check(n < 0, msg); return n; } 
    
	public static void isGreaterThan(long a, long b) { isGreaterThan(a,b,SHOULD_BE_GREATER); }
	public static void isGreaterThan(long a, long b, String msg) { check(a > b, msg); }

	public static long isGreaterOrEqual(long reference, long expected) { return isGreaterOrEqual(reference, expected, SHOULD_BE_GREATER_OR_EQUAL); }
	public static long isGreaterOrEqual(long reference, long expected, String msg) { check(reference >= expected, msg); return reference; }
	public static int  isGreaterOrEqual(int reference, int expected) { return isGreaterOrEqual(reference, expected, SHOULD_BE_GREATER_OR_EQUAL); }
	public static int  isGreaterOrEqual(int reference, int expected, String msg) { return isGreaterOrEqual(reference, expected, defaultException(msg)); }
	public static int  isGreaterOrEqual(int reference, int expected, RuntimeException e) { check(reference >= expected, e); return reference; }
	public static byte isGreaterOrEqual(byte reference, byte expected) { return isGreaterOrEqual(reference, expected, SHOULD_BE_GREATER_OR_EQUAL); }
	public static byte isGreaterOrEqual(byte reference, byte expected, String msg) { check(reference >= expected, msg); return reference; }
	
	public static void isBetween(int low, int mid, int high) { isBetween(low, mid, high, SHOULD_BE_BETWEEN(low, high)); }
	public static void isBetween(int low, int mid, int high, String msg) { isBetween(low, mid, high, defaultException(msg)); }
	public static void isBetween(int low, int mid, int high, RuntimeException e) { check(low <= mid && mid < high, e); }
	
	public static void areEqual(Object o1, Object o2) { areEqual(o1, o2, SHOULD_BE_EQUAL(o1, o2)); }
	public static void areEqual(Object o1, Object o2, String msg) { check(o1.equals(o2), msg); }
	public static void areEqual(long i, long j) { areEqual(i, j, SHOULD_BE_EQUAL(i, j)); }
	public static void areEqual(long i, long j, String msg) { check(i == j, msg); }
	public static void areEqual(int i, int j) { areEqual(i, j, SHOULD_BE_EQUAL(i, j)); }
	public static void areEqual(int i, int j, String msg) { check(i == j, msg); }
	public static void areEqual(byte b1, byte b2) { areEqual(b1, b2, SHOULD_BE_EQUAL(b1, b2)); }
	public static void areEqual(byte b1, byte b2, String msg) { check(b1 == b2, msg); }
	
	public static void notEqual(int i, int j) { notEqual(i, j, SHOULD_BE_DIFFERENT(i, j)); }
	public static void notEqual(int i, int j, String msg) { check(i != j, msg); }
	
	public static void notEqual(Object o1, Object o2) { notEqual(o1,o2,SHOULD_NOT_BE_EQUAL); }
	public static void notEqual(Object o1, Object o2, String msg) { check(!o1.equals(o2),msg); }
 	
	public static String notBlank(String s) { return notBlank(s,SHOULD_NOT_BE_BLANK); }
	public static String notBlank(String s, String msg) { 
		check(s != null && !s.isEmpty(),msg);
		return s;
	}

	private static void check(boolean test, String msg) { check(test, defaultException(msg)); }
	private static void check(boolean test, RuntimeException e) { if (!test) throw e; }
	private static IllegalArgumentException defaultException(String msg) { return new IllegalArgumentException(msg); }
}
