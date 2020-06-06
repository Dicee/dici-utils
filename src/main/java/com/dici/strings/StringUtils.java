package com.dici.strings;

import static com.dici.check.Check.notBlank;
import static java.util.stream.Collectors.joining;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.dici.check.Check;
import com.google.common.collect.Streams;

public class StringUtils {
	private StringUtils() { }
	
	public static int count(String pattern, String input) {
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(input);
		
		int count;
		for (count = 0 ; m.find() ; count++);
		return count;
	}
	
	public static <T> String join(String first, String sep, String last, Iterable<T> iterable) {
		return join(first, sep, last, Object::toString, iterable);
	}
	
	public static <T> String join(String first, String sep, String last, Function<T,String> toString, Iterable<T> iterable) {
		return Streams.stream(iterable).map(toString).collect(joining(sep, first, last));
	}

	public static String blank(int length) { return " ".repeat(length); }

	public static String repeat(char ch, int length) { return String.valueOf(ch).repeat(length); }

	public static String fillWithBlanks(String toFill, int length) { return fillToLength(toFill,' ',length); }

	public static String fillToLength(String toFill, char filler, int length) {
		Check.isGreaterOrEqual(length,toFill.length());
		return toFill + repeat(filler, length - toFill.length());
	}
	
	public static char lastChar(String s) { return notBlank(s).charAt(s.length() - 1); }
}
