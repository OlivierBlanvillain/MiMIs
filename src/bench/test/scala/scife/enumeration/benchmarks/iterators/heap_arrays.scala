package scife.enumeration.benchmarks.iterators

import scife.enumeration.memoization.MemoizationScope
import scife.util.structures.BSTrees.{Tree, Leaf, Node}

// I'm calling these heap arrays to match up to what SciFe says.
// However, these are NOT array-based heaps.

import Iterators._

class HeapArraysBenchmark(key: String, makeCache: () => IteratorCache[(Int, Range), Tree]) extends BenchmarkAdapter[(Int, Range), Tree](key, makeCache) {
  def makeMeasureIterator(size: Int, cache: IteratorCache[(Int, Range), Tree]): Iterator[Tree] = {
    makeIterator(size, cache)
  }
  def makeWarmUpIterator(size: Int, cache: IteratorCache[(Int, Range), Tree]): Iterator[Tree] = {
    makeIterator(size, cache)
  }
  def makeIterator(size: Int, cache: IteratorCache[(Int, Range), Tree]): Iterator[Tree] = {
    HeapArrays.makeHeap(size)(cache)
  }
}

object HeapArrays {
  def getRange(m: Int): Range = {
    m.to(0).by(-1)
  }

  // attempts to mirror constructEnumerator of HeapArrayBenchmark.scala
  // as closely as possible
  def makeHeap(size: Int, range: Range)(implicit cache: IteratorCache[(Int, Range), Tree]): Iterator[Tree] = {
    def uncachedIterator: Iterator[Tree] = {
      if (size > range.size) EmptyIterator
      else if (size <= 0) Leaf
      else if (size == 1) {
	for {
	  i <- 0.until(range.size).iterator
	} yield Node(Leaf, range.start + i, Leaf)
      }
      else if (!range.isEmpty) {
	val leftSize = (size - 1) / 2
	for {
	  rootInd <- range.iterator
	  childRange = getRange(rootInd)
	  left <- makeHeap(size - 1 - leftSize, childRange)
	  right <- makeHeap(leftSize, childRange)
	} yield Node(left, rootInd, right)
      }
      else EmptyIterator
    }

    if (size < 9) {
      cache.tryCache(size -> range, uncachedIterator)
    } else {
      uncachedIterator
    }
  } // makeHeap

  // for the initial call
  def makeHeap(size: Int)(implicit cache: IteratorCache[(Int, Range), Tree]): Iterator[Tree] = {
    makeHeap(size, getRange(size))
  }

  // returns the number of structures in this space
  def walkThrough(size: Int, memo: MemoizationScope): Long = {
    import scife.enumeration.Enum
    import scife.enumeration.dependent.Depend
    import scife.enumeration.benchmarks.HeapArrayBenchmark
    
    val dep: Depend[(Int, Range), Tree] =
      (new HeapArrayBenchmark).constructEnumerator(memo)
    val en: Enum[Tree] = dep.apply((size -> getRange(size)))
    var numStructures: Long = 0
    en.foreach(_ => numStructures += 1)
    numStructures
  }

  def expected(size: Int): List[Tree] = {
    import scife.enumeration.Enum
    import scife.enumeration.dependent.Depend
    import scife.enumeration.benchmarks.HeapArrayBenchmark
    
    val dep: Depend[(Int, Range), Tree] =
      (new HeapArrayBenchmark).constructEnumerator
    val en: Enum[Tree] = dep.apply((size -> getRange(size)))
    en.toList
  }

  def time[A](msg: String)(a: => A): A = {
    val start = System.currentTimeMillis()
    val result = a
    val end = System.currentTimeMillis()
    println(msg + ": " + ((end - start) / 1000))
    result
  }

  // takes a bound, and makes sure that we agree with SciFe
  def main(args: Array[String]) {
    if (args.length != 1) {
      println("Needs a size bound")
    } else {
      val size = args(0).toInt
      val exList = time("scife")(expected(size).toList)
      val gotList = time("mimi")(makeHeap(size)(new RealCache).toList)
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
} // HeapArrays
