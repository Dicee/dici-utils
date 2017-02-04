package com.dici.collection.mutable

import org.scalatest.{FlatSpec, Matchers}

class TrieTest extends FlatSpec with Matchers {
  private val words = List("Hello", "Hi", "Hell", "Good")

  "A Trie" should " store all inserted sequences" in {
    val trie = new HashMapTrie[Char, String] ++= words
    trie.listAllSequences().toList should contain theSameElementsAs words
  }

  "A Trie" should " return the correct sequences for a given prefix" in {
    val trie = new HashMapTrie[Char, String] ++= words
    trie.prefixedBy("H"   ).toList should contain theSameElementsAs List("Hell", "Hello", "Hi")
    trie.prefixedBy("Hel" ).toList should contain theSameElementsAs List("Hell", "Hello")
    trie.prefixedBy("G"   ).toList should contain theSameElementsAs List("Good")
    trie.prefixedBy("N"   ).toList should contain theSameElementsAs List()
  }

  "A Trie" should " include the prefix itself when it is a valid sequence" in {
    val trie = new HashMapTrie[Char, String] ++= words
    trie.prefixedBy("Hell").toList should contain theSameElementsAs List("Hell", "Hello")
  }

  "An empty Trie" should " not fail when queried" in {
    val trie = new HashMapTrie[Char, String]
    trie.prefixedBy("").toList should contain theSameElementsAs List()
  }

  "A Trie" should " detect when an element was already present in the Trie" in {
    val trie = new HashMapTrie[Char, String]
    (trie += "Hello") should be (true )
    (trie += "Good" ) should be (true )
    (trie += "Good" ) should be (false)
    (trie += "Hello") should be (false)
  }

  "A Trie" should " detect when a new terminal sequence gets added without being a terminal node" in {
    val trie = new HashMapTrie[Char, String]
    (trie += "Hello") should be (true )
    (trie += "Hell" ) should be (true )
    (trie += "Hell" ) should be (false)
  }

  "A WeightedTrie" should " explores the nodes of the Trie in order of frequency" in {
    val trie = new WeightedTrie[Char, String] ++= words
    trie.prefixedBy("" ).toList should contain theSameElementsAs List("Hell", "Hello", "Hi", "Good")
    trie.prefixedBy("H").toList should contain theSameElementsAs List("Hell", "Hello", "Hi")

    trie ++= List("Hips", "Hipster", "High", "New", "No", "Noun", "Never", "Ninja", "Nitro", "Net", "Nessy")
    trie.prefixedBy("" ).toList should contain theSameElementsAs List("New", "Net", "Nessy", "Never", "Ninja", "Nitro", "No", "Noun", "Hi", "Hips", "Hipster", "High", "Hell", "Hello", "Good")
    trie.prefixedBy("H").toList should contain theSameElementsAs List("Hi", "Hips", "Hipster", "High", "Hell", "Hello")
  }
}
