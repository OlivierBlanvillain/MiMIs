package scife.enumeration.benchmarks.iterators

import scife.enumeration.memoization.MemoizationScope
import scife.util.structures.BTree._

import Iterators._

class BTreesBenchmark(key: String, makeCache: () => IteratorCache[Any, Tree]) extends BenchmarkAdapter[Any, Tree](key, makeCache) {
  def makeIterator(size: Int, cache: IteratorCache[Any, Tree]): Iterator[Tree] = {
    BTrees.makeRootBTree(size)(cache)
  }
  def makeMeasureIterator(size: Int, cache: IteratorCache[Any, Tree]): Iterator[Tree] = {
    makeIterator(size, cache)
  }
  def makeWarmUpIterator(size: Int, cache: IteratorCache[Any, Tree]): Iterator[Tree] = {
    makeIterator(size, cache)
  }
}

object BTrees {
  val t = 2

  def minChildSize(h: Int): Int = 2 * math.pow(t, h - 1).toInt - 1
  def maxChildSize(h: Int): Int = math.pow(2 * t, h).toInt - 1

  def getAdditions(size: Int, amount: Int, max: Int): Iterator[List[Int]] = {
    if (size == 0 && amount > 0) EmptyIterator
    else if (size == 0) Nil
    else if (size == 1 && amount > max) EmptyIterator
    else {
      for {
	added <- 0.to(math.min(amount, max)).iterator
	rest <- getAdditions(size - 1, amount - added, max)
      } yield added :: rest
    }
  }

  // sublist([], []).
  // sublist([H|T], [H|Rest]) :-
  //     sublist(T, Rest).
  // sublist([_|T], Rest) :-
  //     sublist(T, Rest).
  def sublists[A](list: List[A]): Iterator[List[A]] = {
    if (list.isEmpty) {
      Nil
    } else {
      sublists(list.tail).flatMap(rest =>
	anyToIterator(list.head :: rest).or(rest))
    }
  }

  // the algorithm in SciFe is incredibly complex for this, and I don't completely
  // understand it.  Here, we do a simple filter with the results of sublists
  def sublistsOfSize[A](list: List[A], size: Int): Iterator[List[A]] = {
    sublists(list).filter(_.size == size)
  }

  // for the initial call
  def makeRootBTree(size: Int)(implicit cache: IteratorCache[Any, Tree]): Iterator[Tree] = {
    for {
      height <- 1.to(math.log(size + 1).toInt + 1).iterator
      result <- makeRootBTree(size, 1.to(size), height)
    } yield result
  }

  def makeRootBTree(size: Int, keyRange: Range, h: Int)(implicit cache: IteratorCache[Any, Tree]): Iterator[Tree] = {
    def uncachedIterator: Iterator[Tree] = {
      if (h == 1 && size < 2 * t) {
	for {
	  x <- sublistsOfSize(keyRange.toList, size)
	} yield Tree(x, Nil)
      } else if (h > 1) {
	for {
	  x <- 2.to(2 * t).iterator
	  result <- makeBTree(x, size, keyRange, h)
	} yield result
      } else EmptyIterator
    }
    cache.tryCache((size, keyRange, h), uncachedIterator)
  }

  def makeNonRootBTree(size: Int, keyRange: Range, h: Int)(implicit cache: IteratorCache[Any, Tree]): Iterator[Tree] = {
    def uncachedIterator: Iterator[Tree] = {
      if (h == 1 && size < 2 * t && size >= t - 1) {
	for {
	  x <- sublistsOfSize(keyRange.toList, size)
	} yield Tree(x, Nil)
      } else if (h > 1 && size > 0) {
	for {
	  x <- t.to(2 * t).iterator
	  result <- makeBTree(x, size, keyRange, h)
	} yield result
      } else EmptyIterator
    }
    cache.tryCache((size, keyRange, h), uncachedIterator)
  }

  def liftMap[A, B](list: List[A], makeIterator: A => Iterator[B]): Iterator[List[B]] = {
    list match {
      case a :: as => {
	for {
	  b <- makeIterator(a)
	  rest <- liftMap(as, makeIterator)
	} yield b :: rest
      }
      case Nil => Nil
    }
  }

  def makeBTree(nChildren: Int, size: Int, keyRange: Range, h: Int)(implicit cache: IteratorCache[Any, Tree]): Iterator[Tree] = {
    def uncachedIterator: Iterator[Tree] = {
      val minChildSizeBelow: Int = minChildSize(h - 1)
      val maxChildSizeBelow: Int = maxChildSize(h - 1)
      val nKeys: Int = nChildren - 1
      val restOfNodes: Int = size - nKeys - nChildren * minChildSizeBelow
      
      if (restOfNodes < 0) {
	EmptyIterator
      } else {
	for {
	  addList <- getAdditions(nChildren, restOfNodes, maxChildSizeBelow - minChildSizeBelow)
	  childSizes: List[Int] = addList.map(_ + minChildSizeBelow)
	  addKeys <- getAdditions(nChildren, keyRange.size - size, keyRange.size)
	  keys: List[Int] = childSizes.zip(addKeys).scanLeft(keyRange.start - 1)(
	    { case (soFar, (childSize, add)) => soFar + childSize + add + 1 }).tail.init
	  childRanges: List[Range] = ((keyRange.start - 1) :: keys).zip(keys :+ (keyRange.end + 1)).map(
	    { case (a, b) => (a + 1).to(b - 1) })
	  children <- liftMap(childSizes.zip(childRanges),
			      (pair: (Int, Range)) => makeNonRootBTree(pair._1, pair._2, h - 1))
	  // children: List[Iterator[Tree]] = childSizes.zip(childRanges).map(
	  //   { case (cs, cr) => makeNonRootBTree(cs, cr, h - 1) })
	} yield Tree(keys, children)
      }
    } // uncachedIterator

    cache.tryCache((nChildren, size, keyRange, h), uncachedIterator)
  }

  def expected(size: Int): List[Tree] = {
    import scife.enumeration.Enum
    import scife.enumeration.dependent.Depend
    import scife.enumeration.benchmarks.BTreeTest

    val dep: Depend[(Int, Range, Int), Tree] =
      (new BTreeTest).constructEnumerator
    for {
      height <- 1.to(math.log(size + 1).toInt + 1).toList
      enum = dep.apply((size, 1.to(size), height))
      ind <- 0.until(enum.size).toList
    } yield enum(ind)
  }

  def walkThrough(size: Int, memo: MemoizationScope): Long = {
    import scife.enumeration.Enum
    import scife.enumeration.dependent.Depend
    import scife.enumeration.benchmarks.BTreeTest

    var numStructures: Long = 0

    val dep: Depend[(Int, Range, Int), Tree] =
      (new BTreeTest).constructEnumerator(memo)

    1.to(math.log(size + 1).toInt + 1).foreach(height => {
      val enum = dep.apply((size, 1.to(size), height))
      enum.foreach(_ => numStructures += 1)
    })
    numStructures
  }
    
  // takes a bound, and makes sure that we agree with SciFe
  def main(args: Array[String]) {
    if (args.length != 1) {
      println("Needs a size bound")
    } else {
      val size = args(0).toInt
      val exList = expected(size).toList
      val gotList = makeRootBTree(size)(new RealCache).toList
      if (exList.size != gotList.size) {
	println("RAW LISTS DIFFERED IN SIZES")
      } else {
	println("NUM ELEMENTS: " + gotList.size)
	// println("ELEMENTS: " + gotList)
      }
      val ex = exList.toSet
      val got = gotList.toSet
      if (ex != got) {
	val missing = ex -- got
	val extra = got -- ex
	println("MISSING: " + missing)
	println("EXTRA: " + extra)
      }
      println("SIZE: " + gotList.size)
      gotList.foreach(println)
    }
  } // main
} // BTrees	
