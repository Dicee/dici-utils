package com.dici.files;

import static com.dici.check.Check.notBlank;
import static com.dici.check.Check.notNull;

import java.io.File;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.dici.check.Check;
import com.dici.collection.richIterator.NullableRichIterator;
import com.dici.collection.richIterator.RichIterators;

public class TokenParser {
    public static TokenIterator parse(File f, String token) { return new TokenParser(token).parse(f); }
    public static TokenIterator parse(String s, String token) { return new TokenParser(token).parse(s); }
    public static TokenIterator parse(Iterator<Character> source, String token) { return new TokenParser(token).parse(source); }
    
	private List<Character> token;

	public TokenParser(String token) {
		this.token  = notBlank(token).chars().mapToObj(i -> (char) i).collect(Collectors.toList());
	}
	
	public TokenIterator parse(String s) { return new TokenIterator(RichIterators.characters(s), token); }
	public TokenIterator parse(File f) { return new TokenIterator(RichIterators.characters(f), token); }
	public TokenIterator parse(Iterator<Character> source) { return new TokenIterator(source, token); }
	
	public static class TokenIterator extends NullableRichIterator<String> {
	    private List<Character> token;
	    private Iterator<Character>	source;
	    
	    public TokenIterator(Iterator<Character> source, List<Character> token) {
	        this.source = notNull(source);
	        this.token  = Check.notEmpty(token);
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
}