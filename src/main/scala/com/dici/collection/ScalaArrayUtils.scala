package com.dici.collection

object ScalaArrayUtils {
    def swap[T](arr: Array[T], i: Int, j: Int) = {
        val tmp = arr(i)
        arr(i)  = arr(j)
        arr(j)  = tmp
    }  
}