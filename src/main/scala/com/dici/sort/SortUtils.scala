package com.dici.sort

import scala.util.Random
import com.dici.collection.ScalaArrayUtils.swap

object SortUtils {
    private val rd = new Random
    
    def quickSelect[T](arr: Array[T], index: Int)(implicit conversion: T => Ordered[T]): T = quickSelect(arr, index, 0, arr.length) 
        private def quickSelect[T](arr: Array[T], index: Int, lower: Int, upper: Int)(implicit conversion: T => Ordered[T]): T = {
            val i = pivot(arr, lower, upper)
            if      (i == index) arr(i) 
            else if (i <  index) quickSelect(arr, index, i + 1, upper)
            else                 quickSelect(arr, index, lower, i)
        }
        
    def pivot[T](arr: Array[T], lower: Int, upper: Int)(implicit conversion: T => Ordered[T]) = {
        val index = lower + rd.nextInt(upper - lower)
        val pivot = arr(index)
        swap(arr, index, upper - 1)
        
        var res = lower
        for (i <- lower until upper - 1) {
            if (arr(i) < pivot) {
                swap(arr, res, i)
                res += 1
            }
        }
        swap(arr, res, upper - 1)
        res
    }
}