package com.dici.math

object ScalaMathUtils {
    def combinations(k: Int, n: Int) = {
        def recComb(inc: Int, acc: Int): Int = if (inc > n) acc else recComb(inc + 1, (acc * inc) / (inc - k))
        if (k > n) 0 else recComb(k + 1, 1)
    }        
}