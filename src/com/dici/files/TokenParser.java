package com.dici.files;

import static com.dici.check.Check.notBlank;
import static com.dici.check.Check.notNull;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.dici.collection.richIterator.NullableRichIterator;

public class TokenParser extends NullableRichIterator<String> {
	private List<Character> token;
	private Iterator<Character>	source;

	public TokenParser(Iterator<Character> source, String token) {
		this.source = notNull(source);
		this.token  = notBlank(token).chars().mapToObj(i -> (char) i).collect(Collectors.toList());
	}
	
	@Override
	public String nextOrNull() {
		StringBuilder    result = new StringBuilder();
		Deque<Character> delim  = new LinkedList<>();
		int              len    = token.size();
		
		while (source.hasNext() && !delim.equals(token)) {
			if (delim.size() >= len) delim.pollFirst();
			char ch = source.next();
			result.append(ch);
			delim.addLast(ch);
		}
		return result.length() < len ? null : result.substring(0, result.length() - len);
	}
}
