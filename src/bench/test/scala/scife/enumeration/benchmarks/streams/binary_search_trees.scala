package scife.enumeration.benchmarks.streams

import scife.util.structures.BSTrees.{Tree, Leaf, Node}

import scala.collection.immutable.Range.Inclusive
import scala.collection.immutable.Stream

class StreamBSTBenchmark(key: String) extends StreamBenchmarkAdapter[Tree](key) {
  def makeStream(size: Int): Stream[Tree] = StreamBST.makeBSTNodeBound(size, 1.to(size))
}

object StreamBST {
  import scife.enumeration.benchmarks.iterators.BST.{params, expected}

  // Basic idea to avoid blind search:
  // -Size bounds the number of internal nodes in the tree, exactly
  // -Range bounds the values permissible in nodes
  // -Never allow the range to be more narrow than the size
  def makeBSTNodeBound(size: Int, range: Inclusive): Stream[Tree] = {
    if (size <= 0) {
      Stream(Leaf)
    } else if (size == 1) {
      for {
	i <- range.toStream
      } yield Node(Leaf, i, Leaf)
    } else {
      for {
	leftSize <- 0.until(size).toStream
	rightSize = size - leftSize - 1 // don't forget myself
	// basic idea: conserve at least leftSize elements for the left,
	// and rightSize elements for the right.  The conserved leftSize
	// start at the beginning of the range, and the conserved rightSize
	// start at the end of the range.  Anything in between can go to
	// either one.
	startBetween = range.drop(leftSize).start
	endBetween = range.reverse.drop(rightSize).reverse.end
	median <- startBetween.to(endBetween).toStream
	left <- makeBSTNodeBound(leftSize, range.start.to(median - 1))
	right <- makeBSTNodeBound(rightSize, (median + 1).to(range.end))
      } yield {
	Node(left, median, right)
      }
    }
  }

  // takes a bound, and makes sure that we agree with SciFe
  def main(args: Array[String]) {
    if (args.length != 1) {
      println("Needs a size bound")
    } else {
      val bound = args(0).toInt
      val (size, range) = params(bound)
      val exList = expected(bound).toList
      val gotList = makeBSTNodeBound(size, range).toList
      if (exList.size != gotList.size) {
	println("RAW LISTS DIFFERED IN SIZES")
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
  }
}


