package com.dici.math

object ScalaMathUtils {
    def combinations(k: Int, n: Int) = {
        def recComb(inc: Int, acc: Int): Int = if (inc > n) acc else recComb(inc + 1, (acc * inc) / (inc - k))
        if (k > n) 0 else recComb(k + 1, 1)
    }

    def isPrime(n: Int) = {
        if      (n == 2 || n == 3)                   true
        else if (n <= 1 || n % 2 == 0 || n % 3 == 0) false
        else {
            val x = Math.sqrt(n).toInt
            Stream.from(1).takeWhile(6 * _ - 1 <= x).forall(i => n % (6 * i - 1) != 0 && n % (6 * i + 1) != 0)
        }
    }

    def eSieve(n: Int) = {
		val sieve = Array.fill[Boolean](n + 1)(true)
		sieve(0) = false
		sieve(1) = false

        Stream.range(2, n).filter(sieve(_)).foreach(i => Stream.iterate(2 * i)(_ + i).takeWhile(_ < n).foreach(sieve(_) = false))
		sieve
	}
}