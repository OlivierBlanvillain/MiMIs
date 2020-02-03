package scife.enumeration.benchmarks.streams

import scife.util.structures.BSTrees.{Tree, Leaf, Node}

// I'm calling these heap arrays to match up to what SciFe says.
// However, these are NOT array-based heaps.

import scala.collection.immutable.Stream

class StreamHeapArraysBenchmark(key: String) extends StreamBenchmarkAdapter[Tree](key) {
  def makeStream(size: Int): Stream[Tree] = StreamHeapArrays.makeHeap(size)
}

object StreamHeapArrays {
  import scife.enumeration.benchmarks.iterators.HeapArrays.{getRange, expected}

  // attempts to mirror constructEnumerator of HeapArrayBenchmark.scala
  // as closely as possible
  def makeHeap(size: Int, range: Range): Stream[Tree] = {
    if (size > range.size) Stream.Empty
    else if (size <= 0) Stream(Leaf)
    else if (size == 1) {
      for {
	i <- 0.until(range.size).toStream
      } yield Node(Leaf, range.start + i, Leaf)
    }
    else if (!range.isEmpty) {
      val leftSize = (size - 1) / 2
      for {
	rootInd <- range.toStream
	childRange = getRange(rootInd)
	left <- makeHeap(size - 1 - leftSize, childRange)
	right <- makeHeap(leftSize, childRange)
      } yield Node(left, rootInd, right)
    }
    else Stream.Empty
  } // makeHeap

  // for the initial call
  def makeHeap(size: Int): Stream[Tree] = {
    makeHeap(size, getRange(size))
  }

  // takes a bound, and makes sure that we agree with SciFe
  def main(args: Array[String]) {
    if (args.length != 1) {
      println("Needs a size bound")
    } else {
      val size = args(0).toInt
      val exList = expected(size).toList
      val gotList = makeHeap(size).toList
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
