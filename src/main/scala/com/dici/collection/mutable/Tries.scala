package com.dici.collection.mutable

import scala.collection.mutable

/**
  * Concrete implementation of com.dici.collection.mutable.Trie. This implementation uses a hash map to represent the
  * children of a node, and therefore does not guarantee any traversal order.
  */
class HashMapTrie[ITEM, SEQ](implicit toSeq: SEQ => Seq[ITEM]) extends Trie[ITEM, SEQ] {
  override protected def makeChildren(): Children = new Children {
    private lazy val children = mutable.HashMap[ITEM, Node]()

    override def get        (item: ITEM): Option[Node]           = children.get(item)
    override def getOrCreate(item: ITEM): Node                   = children.getOrElseUpdate(item, new Node)
    override def contains   (item: ITEM): Boolean                = children.contains(item)
    override def iterator               : Iterator[(ITEM, Node)] = children.iterator
  }
}

/**
  * Concrete implementation of com.dici.collection.mutable.Trie. This implementation keeps track of the "weight" of each
  * node of the Trie. The weight is incremented every time the node is "touched", which happens when the node is
  * traversed in a certain context.
  *
  * The following methods will touch all the nodes traversed during their execution:
  *
  * - WeightedTrie.+=(item)
  *
  * The following methods will have no effect on the nodes traversed during their execution:
  *
  * - WeightedTrie.prefixedBy(seq)
  * - WeightedTrie.listAllSequences()
  */
class WeightedTrie[ITEM, SEQ](implicit toSeq: SEQ => Seq[ITEM]) extends Trie[ITEM, SEQ] {
  override protected def makeChildren(): Children = new Children {
    private lazy val weights          = mutable.HashMap[ITEM, Long]()
    private lazy val childrenByWeight = mutable.TreeMap[Long, mutable.Map[ITEM, Node]]()(implicitly[Ordering[Long]].reverse)

    override def get(item: ITEM): Option[Node] = weights.get(item).flatMap(childrenByWeight.get).flatMap(_.get(item))

    override def getOrCreate(item: ITEM): Node = {
      def safeIncrement(weight: Long) = Math.min(Long.MaxValue - 1, weight) + 1

      val (weight_, node_) = weights.get(item) match {
        case Some(weight) =>
          val node     = childrenByWeight(weight).remove(item).get
          val w        = safeIncrement(weight)
          (w, node)
        case None         => (1L, new Node)
      }

      weights          += item    -> weight_
      childrenByWeight += weight_ -> (childrenByWeight.getOrElseUpdate(weight_, new mutable.HashMap[ITEM, Node]) += item -> node_)
      node_
    }

    override def contains(item: ITEM): Boolean                = weights.contains(item)
    override def iterator            : Iterator[(ITEM, Node)] = childrenByWeight.valuesIterator.flatMap(_.iterator)
  }
}