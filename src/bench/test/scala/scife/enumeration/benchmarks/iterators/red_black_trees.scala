package scife.enumeration.benchmarks.iterators

import scife.enumeration.memoization.MemoizationScope
import scife.util.structures.RedBlackTrees.{Tree, Leaf, Node,
					    blackHeightRange}

import Iterators._

import scala.collection.immutable.Range.Inclusive

class RBTreeBenchmark(key: String, makeCache: () => IteratorCache[(Int, Inclusive, Inclusive, Int), Tree]) extends BenchmarkAdapter[(Int, Inclusive, Inclusive, Int), Tree](key, makeCache) {
  def makeMeasureIterator(size: Int, cache: IteratorCache[(Int, Inclusive, Inclusive, Int), Tree]): Iterator[Tree] = {
    RBTree.makeRBTreeMeasure(size)(cache)
  }
  def makeWarmUpIterator(size: Int, cache: IteratorCache[(Int, Inclusive, Inclusive, Int), Tree]): Iterator[Tree] = {
    RBTree.makeRBTreeWarmup(size)(cache)
  }
}
  
object RBTree {
  val RED = 0
  val BLACK = 1

  def calcChildColors(myColor: Int): Inclusive = {
    if (myColor == RED) {
      BLACK.to(BLACK)
    } else {
      RED.to(BLACK)
    }
  }

  def calcChildBlackHeight(myColor: Int, blackHeight: Int): Int = {
    if (myColor == BLACK) {
      blackHeight - 1
    } else {
      blackHeight
    }
  }

  // I'm intentionally trying to mirror the algorithm used by SciFe as much
  // as possible, which is why this looks pretty unnatural.  This avoids a
  // lot of search.
  //
  // -size: number of internal nodes in the tree
  // -range: possible values internal nodes may take
  // -colors: possible tree colors.  0 is red, 1 is black
  // -blackHeight: how deep of a chain of black nodes we need
  def makeRBTree(size: Int, range: Inclusive, colors: Inclusive, blackHeight: Int)(implicit cache: IteratorCache[(Int, Inclusive, Inclusive, Int), Tree]): Iterator[Tree] = {
    cache.tryCache((size, range, colors, blackHeight), {
      if (range.size >= size && range.size < 0 || blackHeight < 0) EmptyIterator
      else if (size == 0 && blackHeight == 1 && colors.end >= 1) Leaf
      else if (size > 0 && blackHeight >= 1) {
	for {
	  leftSize <- 0.until(size).iterator
	  rightSize = size - leftSize - 1 // don't forget myself
	  startBetween = range.drop(leftSize).start
	  endBetween = range.reverse.drop(rightSize).reverse.end
	  median <- startBetween.to(endBetween).iterator
	  myColor <- colors.iterator
	  childColors = calcChildColors(myColor)
	  childBlackHeight = calcChildBlackHeight(myColor, blackHeight)
	  left <- makeRBTree(leftSize, range.start.to(median - 1), childColors, childBlackHeight)
	  right <- makeRBTree(rightSize, (median + 1).to(range.end), childColors, childBlackHeight)
	} yield {
	  Node(left, median, right, myColor == BLACK)
	}
      } else EmptyIterator
    })
  } // makeRBTree

  def makeRBTreeMeasure(size: Int)(implicit cache: IteratorCache[(Int, Inclusive, Inclusive, Int), Tree]): Iterator[Tree] = {
    for {
      blackHeight <- blackHeightRange(size).iterator
      tree <- makeRBTree(size, 1.to(size), 0.to(1), blackHeight)
    } yield tree
  }

  def makeRBTreeWarmup(maxSize: Int)(implicit cache: IteratorCache[(Int, Inclusive, Inclusive, Int), Tree]): Iterator[Tree] = {
    for {
      size <- 1.to(maxSize).iterator
      blackHeight <- 1.to(scife.util.Math.log2(size + 1).toInt + 1)
      tree <- makeRBTree(size, 1.to(size), 0.to(1), blackHeight)
    } yield tree
  }

  def expected(size: Int, range: Inclusive, colors: Inclusive, blackHeight: Int): List[Tree] = {
    import scife.enumeration.Enum
    import scife.enumeration.dependent.Depend
    import scife.enumeration.benchmarks.RedBlackTreeConcise
    val dep: Depend[(Int, Range, Range, Int), Tree] =
      (new RedBlackTreeConcise).constructEnumerator
    val en: Enum[Tree] = dep.apply((size, range, colors, blackHeight))
    en.toList
  }

  def expected(size: Int): List[Tree] = {
    for {
      blackHeight <- blackHeightRange(size).toList
      result <- expected(size, 1.to(size), 0.to(1), blackHeight)
    } yield result
  }

  // returns the number of structures in this space
  def walkThrough(bound: Int, memo: MemoizationScope): Long = {
    var numStructures: Long = 0
    blackHeightRange(bound).foreach(blackHeight => {
      import scife.enumeration.Enum
      import scife.enumeration.dependent.Depend
      import scife.enumeration.benchmarks.RedBlackTreeConcise
      val dep: Depend[(Int, Range, Range, Int), Tree] =
	(new RedBlackTreeConcise).constructEnumerator(memo)
      val en: Enum[Tree] = dep.apply((bound, 1.to(bound), 0.to(1), blackHeight))
      en.foreach(_ => numStructures += 1)
    })
    numStructures
  }

  def main(args: Array[String]) {
    if (args.length != 1) {
      println("Needs a size bound")
    } else {
      val size = args(0).toInt
      val exList = expected(size)
      val gotList = makeRBTreeMeasure(size)(new RealCache).toList
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
      gotList.foreach(println)
    }
  } // main
} // RBTree
