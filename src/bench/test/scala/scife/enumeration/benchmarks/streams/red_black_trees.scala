package scife.enumeration.benchmarks.streams

import scife.util.structures.RedBlackTrees.{Tree, Leaf, Node,
					    blackHeightRange}

import scala.collection.immutable.Range.Inclusive
import scala.collection.immutable.Stream

class StreamRBTreeBenchmark(key: String) extends StreamBenchmarkAdapter[Tree](key) {
  def makeStream(size: Int): Stream[Tree] = StreamRBTree.makeRBTreeMeasure(size)
}
  
object StreamRBTree {
  import scife.enumeration.benchmarks.iterators.RBTree.{
    RED, BLACK,
    calcChildColors,
    calcChildBlackHeight,
    expected}

  // I'm intentionally trying to mirror the algorithm used by SciFe as much
  // as possible, which is why this looks pretty unnatural.  This avoids a
  // lot of search.
  //
  // -size: number of internal nodes in the tree
  // -range: possible values internal nodes may take
  // -colors: possible tree colors.  0 is red, 1 is black
  // -blackHeight: how deep of a chain of black nodes we need
  def makeRBTree(size: Int, range: Inclusive, colors: Inclusive, blackHeight: Int): Stream[Tree] = {
    if (range.size >= size && range.size < 0 || blackHeight < 0) Stream.Empty
    else if (size == 0 && blackHeight == 1 && colors.end >= 1) Stream(Leaf)
    else if (size > 0 && blackHeight >= 1) {
      for {
	leftSize <- 0.until(size).toStream
	rightSize = size - leftSize - 1 // don't forget myself
	startBetween = range.drop(leftSize).start
	endBetween = range.reverse.drop(rightSize).reverse.end
	median <- startBetween.to(endBetween).toStream
	myColor <- colors.toStream
	childColors = calcChildColors(myColor)
	childBlackHeight = calcChildBlackHeight(myColor, blackHeight)
	left <- makeRBTree(leftSize, range.start.to(median - 1), childColors, childBlackHeight)
	right <- makeRBTree(rightSize, (median + 1).to(range.end), childColors, childBlackHeight)
      } yield {
	Node(left, median, right, myColor == BLACK)
      }
    } else Stream.Empty
  } // makeRBTree

  def makeRBTreeMeasure(size: Int): Stream[Tree] = {
    for {
      blackHeight <- blackHeightRange(size).toStream
      tree <- makeRBTree(size, 1.to(size), 0.to(1), blackHeight)
    } yield tree
  }

  def main(args: Array[String]) {
    if (args.length != 1) {
      println("Needs a size bound")
    } else {
      val size = args(0).toInt
      val exList = expected(size)
      val gotList = makeRBTreeMeasure(size).toList
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
  } // main
} // RBTree
