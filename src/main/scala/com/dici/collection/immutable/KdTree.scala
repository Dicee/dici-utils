package com.dici.collection.immutable

import com.dici.collection.QuickSelect
import com.dici.collection.immutable.KdTree.PointND

/**
  * Immutable implementation of a kd-tree. The tree will be built from a dataset presented upfront using an algorithm that
  * ensures good balancing.
  */
class KdTree[T](points: PointND[T]*)(implicit ordering: Ordering[T]) {
  private val root: Node = if (points.isEmpty) null else init(points.toArray)

  private def init(points: Array[PointND[T]]): Node = {
    assert(points.view.map(_.dim).distinct.size == 1, "All points should have the same dimension")
    insert(points, 0)
  }

  private def insert(points: Array[PointND[T]], depth: Int): Node = {
    if (points.isEmpty) return null

    val axis       = depth % points(0).dim
    val median     = QuickSelect.median(points)(KdTree.pointNDOrdering(axis)(ordering))
    val splitIndex = (points.length - 1) / 2

    // slice addresses all bounds edge-cases by returning an empty view, so no need to worry about it
    val leftChild = insert(points.view.slice(0, splitIndex).toArray, depth + 1)
    val rightChild = insert(points.view.slice(splitIndex + 1, points.length).toArray, depth + 1)

    Node(median, leftChild, rightChild)
  }

  private case class Node(point: PointND[T], left: Node, right: Node)
}

object KdTree {
  def pointNDOrdering[T](axis: Int)(ordering: Ordering[T]): Ordering[PointND[T]] = (p1, p2) => ordering.compare(p1(axis), p2(axis))

  /**
    * Represents a point of any type with an associated ordering function. Mathematically, this ordering function can be
    * considered like the projection of an n-dimensional space on the normal vector to a given hyperplane. In this data
    * structure, it will be used to determine on what side of an hyperplane a given point lies.
    */
  class PointND[T](private val coords: Array[T])(implicit ordering: Ordering[T]) {
    val dim: Int = coords.length

    def apply(axis: Int): T = coords(axis)
    def compareTo(axis: Int, hyperplane: PointND[T]): Int = ordering.compare(coords(axis), hyperplane(axis))
  }
}
