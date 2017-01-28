package com.dici.collection.mutable

import scala.collection.generic.CanBuildFrom
import scala.collection.mutable.ArrayBuffer

/**
 * Generic implementation of the trie (or prefix tree) data structure. This implementation is not thread-safe.
 */
abstract class Trie[ITEM, SEQ](implicit asSeq: SEQ => Seq[ITEM]) {
  private lazy val root = new Node

  /**
    * Adds a sequence to the Trie
    * @return true if the sequence was not already present in the Trie, false otherwise
    */
  def +=(seq: SEQ): Boolean = root += seq
  def ++=(seqs: Seq[SEQ]): Trie[ITEM, SEQ] = { seqs.foreach(this += _); this }

  /**
    * Finds all the sequences in the Trie that are prefixed by <code>seq</code> in form of a lazy evaluated iterator
    */
  def prefixedBy(seq: SEQ)(implicit canBuildFrom: CanBuildFrom[SEQ, ITEM, SEQ]): Iterator[SEQ] = root.prefixedBy(seq)(canBuildFrom)

  /**
    * Lists all sequences added to this Trie
    */
  def listAllSequences()(implicit canBuildFrom: CanBuildFrom[SEQ, ITEM, SEQ]): Iterator[SEQ] = root.listAllSequences(canBuildFrom().result())(canBuildFrom)

  protected def makeChildren(): Children

  protected trait Children extends Iterable[(ITEM, Node)] {
    /**
      * Whether or not the <code>item</code> branch exists
      * @return <code>true</code> if the branch exists, <code>false</code> otherwise
      */
    def contains(item: ITEM): Boolean

    /**
      * Gets the node located in the <code>item</code> branch
      * @return <code>Some(node)</code> if the branch exists, <code>None</code> otherwise
      */
    def get(item: ITEM): Option[Node]

    /**
      * Get the node located in the <code>item</code> branch if it exists, or create it otherwise.
      * @return itself, for allowing chaining
      */
    def getOrCreate(item: ITEM): Node
  }

  protected[this] final class Node() {
    private lazy val children = makeChildren()
    private var isTerminal    = false

    def +=(seq: SEQ): Boolean = {
      var (node, added) = (this, false)
      for (item <- seq) {
        added = added || !node.children.contains(item)
        node  = node.children.getOrCreate(item)
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

    // fully lazy implementation
    def listAllSequences(prefix: SEQ)(implicit canBuildFrom: CanBuildFrom[SEQ, ITEM, SEQ]): Iterator[SEQ] = {
      val initialNode = this
      new Iterator[SEQ] {
        private val childrenStack   = new ReversedIteratorStack[Iterator[(ITEM, Node)]](initialNode.children.iterator)
        private var currentSequence = new ReversedIteratorStack[ITEM](asSeq(prefix): _*)

        private var currentNode : Node = initialNode
        private var lastTerminal: Node = _

        private var nextOption  : Option[SEQ] = None

        override def hasNext: Boolean = nextOption match {
          case Some(_) => true
          case None    => nextOption = nextSeq(); nextOption.isDefined
        }

        override def next(): SEQ = {
          if (!hasNext) throw new NoSuchElementException
          val res    = nextOption.get
          nextOption = None
          res
        }

        private def nextSeq(): Option[SEQ] = {
          if (childrenStack.isEmpty) return None

          // DFS: go as deep as possible while collecting lazy iterators of siblings of the current node as well as the
          // current sequence
          do {
            if (isConsumableTerminalNode(currentNode)) {
              lastTerminal = currentNode
              return Some(buildSeq())
            }

            val (nextItem, nextNode) = childrenStack.top.next()
            currentNode = nextNode
            currentSequence.push(nextItem)
            childrenStack.push(currentNode.children.iterator)
          } while (childrenStack.top.nonEmpty)

          val res = Some(buildSeq())

          // find the next currentNode by exploring siblings first, and then siblings of the parent etc
          while (childrenStack.nonEmpty && childrenStack.top.isEmpty) {
            childrenStack.pop()
            if (childrenStack.nonEmpty) currentSequence.pop()
          }

          res
        }

        private def isConsumableTerminalNode(node: Node) = node.isTerminal && node != lastTerminal && node.children.nonEmpty
        private def buildSeq(): SEQ = (canBuildFrom(prefix) ++= currentSequence).result()
      }
    }

    private class ReversedIteratorStack[T](args: T*) extends Iterable[T] {
      private var stack = ArrayBuffer[T](args: _*)

      def push(t: T)        = stack += t
      def pop ()            = stack.remove(stack.length - 1)
      def top               = stack(stack.length - 1)
      override def isEmpty  = stack.isEmpty
      override def nonEmpty = stack.nonEmpty
      override def iterator = stack.iterator
    }

    override def toString: String = getClass.getSimpleName + (children, isTerminal)
  }
}
