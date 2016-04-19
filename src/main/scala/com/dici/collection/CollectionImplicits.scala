package com.dici.collection

import scala.collection.immutable.ListMap
import scala.collection.mutable.LinkedHashMap

object CollectionImplicits {
    implicit class AugmentedImmutableMap[K, V](map: Map[K, V]) {
        def mapKeys[KR](f: K => KR) = map.map { case (k, v) => (f(k), v) }.toMap

        def mapKeysPreservingOrder[KR](f: K => KR) = {
            val newMap = LinkedHashMap[KR, V]()
            for ((k, v) <- map) { newMap += f(k) -> v }
            ListMap(newMap.toSeq: _*)
        }
    }
}
