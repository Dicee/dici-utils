package com.dici.collection.mutable

import scala.collection.generic.CanBuildFrom
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 * Generic implementation of the trie (or prefix tree) data structure. We make the assumption that the sequences to
 * insert (typically, words) are small compared to the size of the JVM's stack. In other words, the trie is expected to
 * be potentially wide but not deep. Therefore, we don't protect ourselves from stack overflows in recursive methods
 * when that would make the implementation trickier.
 *
 * This implementation is not thread-safe.
 */
class Trie[ITEM, SEQ](implicit toSeq: SEQ => Seq[ITEM]) {
  private lazy val root = new Node

  def +=(seq: SEQ) = root += seq
  def ++=(seqs: Seq[SEQ]) = { seqs.foreach(this += _); this }

  def prefixedBy(seq: SEQ)(implicit canBuildFrom: CanBuildFrom[SEQ, ITEM, SEQ]) = root.prefixedBy(seq)(canBuildFrom)

  def listAllSequences()(implicit canBuildFrom: CanBuildFrom[SEQ, ITEM, SEQ]): Iterator[SEQ] = root.listAllSequences(canBuildFrom().result())(canBuildFrom)

  private[this] class Node() {
    private lazy val children = mutable.HashMap[ITEM, Node]()
    private var isTerminal = false

    def +=(seq: SEQ): Boolean = {
      var (node, added) = (this, false)
      for (item <- seq) {
        added = added || !node.children.contains(item)
        node  = node.children.getOrElseUpdate(item, new Node)
      }

      added = added || !node.isTerminal
      node.isTerminal = true
      added
    }

    def prefixedBy(seq: SEQ)(implicit canBuildFrom: CanBuildFrom[SEQ, ITEM, SEQ]): Iterator[SEQ] = {
      var node = this
      for (item <- seq) node.children.get(item) match {
        case Some(child) => node = child
        case None        => return Iterator()
      }
      node.listAllSequences(seq)
    }

    def listAllSequences(prefix: SEQ, suffix: ArrayBuffer[ITEM] = ArrayBuffer())(implicit canBuildFrom: CanBuildFrom[SEQ, ITEM, SEQ]): Iterator[SEQ] = {
      def buildSeq() = (canBuildFrom(prefix) ++= prefix ++= suffix).result()

      var res = if (isTerminal) Iterator(buildSeq()) else Iterator[SEQ]()
      for ((item, node) <- children) {
        suffix += item
        // Iterator.++= lazily evaluates the right member of the expression, so need to make this call. However, doing
        // this we break all the desirable lazy evaluation (for an auto-completer for example, would be better to
        // return a few suggestions quickly than all suggestions - that can fit on the screen - slowly). This is my
        // first go at it, need to fix it.
        val sequences = node.listAllSequences(prefix, suffix)
        res ++= sequences
        suffix.remove(suffix.length - 1)
      }
      res
    }

    override def toString = getClass.getSimpleName + (children, isTerminal)
  }
}