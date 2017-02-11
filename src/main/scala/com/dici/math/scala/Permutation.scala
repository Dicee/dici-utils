package com.dici.math.scala

import com.dici.math.{ Permutation => JavaPermutation }

class Permutation(private val perm: JavaPermutation) extends Iterable[Int] {
	def apply(index: Int) = perm.get(index)

	def iterator = new Iterator[Int] {
		private val it = perm.iterator
		def hasNext = it.hasNext
		def next    = it.next
	}
}