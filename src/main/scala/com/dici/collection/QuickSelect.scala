package com.dici.collection

import scala.util.Random

object QuickSelect {
  import ScalaArrayUtils.swap

  private val rd = new Random

  /**
    * Calculates the median value in an array. The returned value is such that the set elements which are lower or equal
    * to this value has the same cardinality as the set of elements which are greater or equal. In the case of arrays
    * with an even number of elements, the selected value will always be the last value of the first half of the array if
    * it was sorted. For arrays with an odd number of elements, the central element is double counted as being present in
    * both sets.
    *
    * Important: this method mutates the array. The median will be at the same index it would be in a sorted array when
    * the method returns.
    */
  def median[T](arr: Array[T])(implicit ordering: Ordering[T]): T = kthElement(arr, 1 + (arr.length - 1) / 2)

  def kthElement[T](arr: Array[T], k: Int)(implicit ordering: Ordering[T]): T = kthElement(arr, k, 0, arr.length)(ordering)

  def kthElement[T](arr: Array[T], k: Int, min: Int, max: Int)(implicit ordering: Ordering[T]): T = {
    if (min < max - 1) {
      val p = pivot(arr, min, max)
      return if (p == k - 1) arr(p)
      else if (p < k - 1) kthElement(arr, k, p + 1, max)(ordering)
      else kthElement(arr, k, min, p)(ordering)
    }
    arr(Math.min(min, max))
  }

  private def pivot[T](arr: Array[T], min: Int, max: Int)(implicit ordering: Ordering[T]) = {
    var pivot = min + rd.nextInt(max - min)
    swap(arr, pivot, max - 1)
    pivot = min

    for (i <- min until max) {
      if (ordering.lt(arr(i), arr(max - 1))) {
        swap(arr, i, pivot)
        pivot += 1
      }
    }
    swap(arr, max - 1, pivot)
    pivot
  }
}
