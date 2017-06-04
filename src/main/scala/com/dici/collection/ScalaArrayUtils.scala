package com.dici.collection

object ScalaArrayUtils {
    def swap[T](arr: Array[T], i: Int, j: Int) = {
        val tmp = arr(i)
        arr(i)  = arr(j)
        arr(j)  = tmp
    }

    def printMatrix[T](matrix: Array[Array[T]], paddingChar: Char = ' ', spacingChar: Char = ' ')(formatter: (Int, Int, T) => String) = {
        val maxLength = (for (i <- matrix.indices; j <- matrix(0).indices) yield formatter(i, j, matrix(i)(j))).map(_.length).max
        for (i <- matrix.indices) {
            for (j <- matrix(0).indices) {
                print(spacingChar + formatter(i, j, matrix(i)(j)).padTo(maxLength, paddingChar))
            }
            println()
        }
    }
}