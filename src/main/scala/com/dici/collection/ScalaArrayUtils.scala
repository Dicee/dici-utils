package com.dici.collection

object ScalaArrayUtils {
    def swap[T](arr: Array[T], i: Int, j: Int) = {
        val tmp = arr(i)
        arr(i)  = arr(j)
        arr(j)  = tmp
    }

    def printMatrix[T](matrix: Array[Array[T]], paddingChar: Char = ' ', spacingChar: Char = ' ')(formatter: (Int, Int, T) => String) = {
        val maxLength = (for (i <- 0 until matrix.length ; j <- 0 until matrix(0).length) yield formatter(i, j, matrix(i)(j))).map(_.length).max
        for (i <- 0 until matrix.length) {
            for (j <- 0 until matrix(0).length) {
                print(spacingChar + formatter(i, j, matrix(i)(j)).padTo(maxLength, paddingChar))
            }
            println()
        }
    }
}