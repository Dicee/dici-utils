package com.dici.math.scala

import com.dici.math.{Permutation, PermutationGenerator}

import scala.collection.Iterator

class PermGen(var perm: Permutation, var reverse : Boolean = false) extends Iterable[Permutation] {
	private var generator : PermutationGenerator = new PermutationGenerator(perm, reverse)
	
	def iterator = new Iterator[Permutation] {
		private val it = generator.iterator
		def next    = new Permutation(it.next)
		def hasNext = it.hasNext
	}
}

object PermGen {
	def apply(seed : String) = new PermGen(Permutation.fromDigits(seed), false)
}