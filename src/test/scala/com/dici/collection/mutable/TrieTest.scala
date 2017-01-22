package com.dici.collection.mutable

import org.scalatest.{FlatSpec, Matchers}

class TrieTest extends FlatSpec with Matchers {
  private val words = List("Hello", "Hi", "Hell", "Good")

  "A Trie" should " store all inserted sequences" in {
    val trie = new Trie[Char, String] ++= words
    trie.listAllSequences() should contain theSameElementsAs words
  }

  "A Trie" should " return the correct sequences for a given prefix" in {
    val trie = new Trie[Char, String] ++= words
    trie.prefixedBy("H"   ) should contain theSameElementsAs List("Hell", "Hello", "Hi")
    trie.prefixedBy("Hel" ) should contain theSameElementsAs List("Hell", "Hello")
    trie.prefixedBy("G"   ) should contain theSameElementsAs List("Good")
    trie.prefixedBy("N"   ) should contain theSameElementsAs List()
  }

  "A Trie" should " include the prefix itself when it is a valid sequence" in {
    val trie = new Trie[Char, String] ++= words
    trie.prefixedBy("Hell") should contain theSameElementsAs List("Hell", "Hello")
  }

  "A Trie" should " detect when an element was already present in the Trie" in {
    val trie = new Trie[Char, String]
    (trie += "Hello") should be (true )
    (trie += "Good" ) should be (true )
    (trie += "Good" ) should be (false)
    (trie += "Hello") should be (false)
  }

  "A Trie" should " detect when a new terminal sequence gets added without being a terminal node" in {
    val trie = new Trie[Char, String]
    (trie += "Hello") should be (true )
    (trie += "Hell" ) should be (true )
    (trie += "Hell" ) should be (false)
  }
}
