package com.dici.function

/**
 * I slightly modified some code found on Github to address two issues:
 * - case class should not be used with an attribute of varying hash
 * - view bounds are now deprecated (https://issues.scala-lang.org/browse/SI-7629). Replaced with implicit parameter.
 *
 * Original code: https://github.com/pathikrit/scalgos/blob/7ded867ba39c54011f03bf37a6ff845b020476ff/src/main/scala/com/github/pathikrit/scalgos/Memo.scala
 *
 * **************************************************************
 * Generic way to create memoized functions (even recursive and multiple-arg ones)
 * @see http://stackoverflow.com/questions/25129721/ for full explanation of this
 *
 * @param f the function to memoize
 * @tparam I input to f
 * @tparam K the keys we should use in cache instead of I
 * @tparam O output of f
 */
class Memoized[I, K, O](f: I => O)(implicit convert: I => K) extends (I => O) {
  import scala.collection.mutable.{Map => Dict}
  val cache = Dict.empty[K, O]
  override def apply(x: I) = { if (cache contains x) println("cached: " + x); cache getOrElseUpdate (convert(x), f(x)) }
}

object Memoized {
  /**
   * Type of a simple memoized function e.g. when I = K
   */
  type ==>[I, O] = Memoized[I, I, O]

  def apply[I, K, O](f: I => O)(implicit convert: I => K) = new Memoized[I, K, O](f)(convert)
}
