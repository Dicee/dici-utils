package com.dici.collection;

import com.dici.collection.BoundedBuffer.SizeExceededException;
import com.dici.collection.BoundedBuffer.SizeExceededPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class BoundedBufferTest {
	private static final int MAX_SIZE = 3;
	private BoundedBuffer<Integer>	buffer;
	
	@BeforeEach
	public void setUp() {
		this.buffer = new BoundedBuffer<>(MAX_SIZE, SizeExceededPolicy.ERROR);
		this.buffer.push(3);
		this.buffer.push(6);
	}
	
	@Test
	public void behavesLikeANormalDeque_1() {
		buffer.addLast(7);
		assertThat(buffer.peekLast(), equalTo(7));
		assertThat(buffer.pop(), equalTo(6));
		assertThat(buffer.pollLast(), equalTo(7));
		assertThat(buffer.pollFirst(), equalTo(3));
		assertThat(buffer.pollFirst(), equalTo(null));
	}
	
	@Test
	public void behavesLikeANormalDeque_2() {
		buffer.pop();
		buffer.pop();
		assertThatThrownBy(buffer::removeLast).isExactlyInstanceOf(NoSuchElementException.class);
	}
	
	@Test
	public void isBounded() {
		buffer.add(5);
		assertThatThrownBy(() -> buffer.add(5))
				.isExactlyInstanceOf(BoundedBuffer.SizeExceededException.class);
	}
	
	@Test
	public void constructorFromCollectionIsBounded() {
		assertThatThrownBy(() -> new BoundedBuffer<>(MAX_SIZE, asList(Arrays.ofDim(Integer.class, MAX_SIZE + 3)), SizeExceededPolicy.ERROR))
				.isExactlyInstanceOf(SizeExceededException.class);
	}
	
	@Test
	public void constructorFromCollectionIgnoresSizeExceededIfPolicySet() {
		BoundedBuffer<Integer> buffer = new BoundedBuffer<>(MAX_SIZE, asList(Arrays.ofDim(Integer.class, MAX_SIZE + 3)), SizeExceededPolicy.IGNORE);
		assertThat(buffer.isFull(), is(true));
	}
	
	@Test
	public void ignoresSizeExceededIfPolicySet() {
		BoundedBuffer<Integer> buffer = new BoundedBuffer<>(1, SizeExceededPolicy.IGNORE);
		buffer.add(1);
		buffer.add(2);
		assertThat(buffer.isFull(), is(true));
	}
}
