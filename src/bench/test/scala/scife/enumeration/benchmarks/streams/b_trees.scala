package scife.enumeration.benchmarks.streams

import scife.enumeration.memoization.MemoizationScope
import scife.util.structures.BTree._

import scala.collection.immutable.Stream


class StreamBTreesBenchmark(key: String) extends StreamBenchmarkAdapter[Tree](key) {
  def makeStream(size: Int): Stream[Tree] = StreamBTrees.makeRootBTree(size)
}

object StreamBTrees {
  import scife.enumeration.benchmarks.iterators.BTrees.{
    t,
    minChildSize,
    maxChildSize,
    expected
  }
    
  def getAdditions(size: Int, amount: Int, max: Int): Stream[List[Int]] = {
    if (size == 0 && amount > 0) Stream.Empty
    else if (size == 0) Stream(Nil)
    else if (size == 1 && amount > max) Stream.Empty
    else {
      for {
	added <- 0.to(math.min(amount, max)).toStream
	rest <- getAdditions(size - 1, amount - added, max)
      } yield added :: rest
    }
  }

  // sublist([], []).
  // sublist([H|T], [H|Rest]) :-
  //     sublist(T, Rest).
  // sublist([_|T], Rest) :-
  //     sublist(T, Rest).
  def sublists[A](list: List[A]): Stream[List[A]] = {
    if (list.isEmpty) {
      Stream(Nil)
    } else {
      sublists(list.tail).flatMap(rest =>
	Stream((list.head :: rest), rest))
    }
  }

  // the algorithm in SciFe is incredibly complex for this, and I don't completely
  // understand it.  Here, we do a simple filter with the results of sublists
  def sublistsOfSize[A](list: List[A], size: Int): Stream[List[A]] = {
    sublists(list).filter(_.size == size)
  }

  // for the initial call
  def makeRootBTree(size: Int): Stream[Tree] = {
    for {
      height <- 1.to(math.log(size + 1).toInt + 1).toStream
      result <- makeRootBTree(size, 1.to(size), height)
    } yield result
  }

  def makeRootBTree(size: Int, keyRange: Range, h: Int): Stream[Tree] = {
    if (h == 1 && size < 2 * t) {
      for {
	x <- sublistsOfSize(keyRange.toList, size)
      } yield Tree(x, Nil)
    } else if (h > 1) {
      for {
	x <- 2.to(2 * t).toStream
	result <- makeBTree(x, size, keyRange, h)
      } yield result
    } else Stream.Empty
  }

  def makeNonRootBTree(size: Int, keyRange: Range, h: Int): Stream[Tree] = {
    if (h == 1 && size < 2 * t && size >= t - 1) {
      for {
	x <- sublistsOfSize(keyRange.toList, size)
      } yield Tree(x, Nil)
    } else if (h > 1 && size > 0) {
      for {
	x <- t.to(2 * t).toStream
	result <- makeBTree(x, size, keyRange, h)
      } yield result
    } else Stream.Empty
  }

  def liftMap[A, B](list: List[A], makeStream: A => Stream[B]): Stream[List[B]] = {
    list match {
      case a :: as => {
	for {
	  b <- makeStream(a)
	  rest <- liftMap(as, makeStream)
	} yield b :: rest
      }
      case Nil => Stream(Nil)
    }
  }

  def makeBTree(nChildren: Int, size: Int, keyRange: Range, h: Int): Stream[Tree] = {
    val minChildSizeBelow: Int = minChildSize(h - 1)
    val maxChildSizeBelow: Int = maxChildSize(h - 1)
    val nKeys: Int = nChildren - 1
    val restOfNodes: Int = size - nKeys - nChildren * minChildSizeBelow
      
    if (restOfNodes < 0) {
      Stream.Empty
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
      } yield Tree(keys, children)
    }
  }

  // takes a bound, and makes sure that we agree with SciFe
  def main(args: Array[String]) {
    if (args.length != 1) {
      println("Needs a size bound")
    } else {
      val size = args(0).toInt
      val exList = expected(size).toList
      val gotList = makeRootBTree(size).toList
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
    }
  } // main
} // BTrees	
