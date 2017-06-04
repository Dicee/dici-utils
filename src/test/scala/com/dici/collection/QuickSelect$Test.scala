package com.dici.collection

import org.scalatest.{FunSuite, Matchers}

import scala.util.Random

class QuickSelect$Test extends FunSuite with Matchers {
  private val NumShuffles = 10

  test("testMedian_evenNumberOfElements_selectsLastElementOfFirstHalf") {
    val sortedArray = (1 to 10).toArray
    assertMedianIs(sortedArray, 5)

    for (_ <- 1 to NumShuffles) {
      val shuffledArray = Random.shuffle(sortedArray.toSeq)
      assertMedianIs(shuffledArray, 5)
    }
  }

  test("testMedian_oddNumberOfElements_selectsMiddle") {
    val sortedArray = (1 to 11).toArray
    assertMedianIs(sortedArray, 6)

    for (_ <- 1 to NumShuffles) {
      val shuffledArray = Random.shuffle(sortedArray.toSeq)
      assertMedianIs(shuffledArray, 6)
    }
  }

  private def assertMedianIs(seq: Seq[Int], expected: Int) = {
    println("Testing with " + seq)
    QuickSelect.median(seq.toArray) should equal(expected)
  }
}
