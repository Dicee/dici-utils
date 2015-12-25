package com.dici.collection.mutable

class Counter[T] extends Iterable[T] {
    private[this] val map = scala.collection.mutable.LinkedHashMap[T, Int]() 
    
    def this(iterable: Iterable[T]) = { this(); this ++= iterable }

    def apply(t: T) = map(t)
    
    def ++=(iterable: Iterable[T]) = iterable.foreach(this += _) 
    def --=(iterable: Iterable[T]) = iterable.foreach(this -= _) 

    def +=(t: T) = map += t -> (map.getOrElse(t, 0) + 1)
    def -=(t: T) = map.get(t) match {
        case None    => false
        case Some(1) => map -= t; true
        case Some(n) => map += t -> (n - 1); true 
    }
    
    override def iterator = new Iterator[T] {
        var counter = 0
        var wrapped = Counter.this.map.iterator
        var current: Option[T] = None
        
        override def next = {
            if (counter > 0) counter -= 1
            else { 
                var (next, count) = wrapped.next
                current           = Some(next)
                counter           = count - 1
            }
            current.get
        }
        override def hasNext = wrapped.hasNext || counter > 0
    } 
}