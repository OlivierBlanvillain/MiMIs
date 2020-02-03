package scife.enumeration.benchmarks.iterators

import scife.util.structures.BSTrees.{Tree, Leaf, Node}
import scife.enumeration.memoization.{MemoizationScope, scope}
import scife.enumeration.Enum

import Iterators._

import scala.collection.immutable.Range.Inclusive

class BSTBenchmark(key: String, makeCache: () => IteratorCache[(Int, Inclusive), Tree]) extends BenchmarkAdapter[(Int, Inclusive), Tree](key, makeCache) {
  def makeIterator(size: Int, cache: IteratorCache[(Int, Inclusive), Tree]): Iterator[Tree] = {
    BST.makeBSTNodeBound(size, 1.to(size))(cache)
  }

  def makeMeasureIterator(size: Int, cache: IteratorCache[(Int, Inclusive), Tree]): Iterator[Tree] = makeIterator(size, cache)
  def makeWarmUpIterator(size: Int, cache: IteratorCache[(Int, Inclusive), Tree]): Iterator[Tree] = makeIterator(size, cache)
}

object BST {
  // Basic idea to avoid blind search:
  // -Size bounds the number of internal nodes in the tree, exactly
  // -Range bounds the values permissible in nodes
  // -Never allow the range to be more narrow than the size
  def makeBSTNodeBound(size: Int, range: Inclusive)(implicit cache: IteratorCache[(Int, Inclusive), Tree]): Iterator[Tree] =
    cache.tryCache(size -> range, {
      // assert(size <= range.size,
      // 	   "SIZE: " + size + " RANGE: " + range)
      if (size <= 0) {
	Leaf
      } else if (size == 1) {
	for {
	  i <- range.iterator
	} yield Node(Leaf, i, Leaf)
      } else {
	for {
	  leftSize <- 0.until(size).iterator
	  rightSize = size - leftSize - 1 // don't forget myself
	  // basic idea: conserve at least leftSize elements for the left,
	  // and rightSize elements for the right.  The conserved leftSize
	  // start at the beginning of the range, and the conserved rightSize
	  // start at the end of the range.  Anything in between can go to
	  // either one.
	  startBetween = range.drop(leftSize).start
	  endBetween = range.reverse.drop(rightSize).reverse.end
	  // _ = println("SIZE: " + size +
	  // 	    " RANGE: " + range +
	  // 	    " leftSize: " + leftSize +
	  // 	    " rightSize: " + rightSize +
	  // 	    " startBetween: " + startBetween + 
	  // 	    " endBetween: " + endBetween)
	  median <- startBetween.to(endBetween).iterator
	  left <- makeBSTNodeBound(leftSize, range.start.to(median - 1))
	  right <- makeBSTNodeBound(rightSize, (median + 1).to(range.end))
	} yield {
	  Node(left, median, right)
	}
      }
    })

  def params(bound: Int): (Int, Inclusive) = {
    (bound, 1.to(bound))
  }

  def makeEnumerator(bound: Int, scope: MemoizationScope): Enum[Tree] = {
    import scife.enumeration.dependent.Depend
    import scife.enumeration.benchmarks.BinarySearchTreeBenchmark

    (new BinarySearchTreeBenchmark).constructEnumerator(scope).apply(params(bound))
  }

  def expected(bound: Int): List[Tree] = {
    makeEnumerator(bound, new scope.AccumulatingScope).toList
  }
    
  // takes a bound, and makes sure that we agree with SciFe
  def main(args: Array[String]) {
    if (args.length != 1) {
      println("Needs a size bound")
    } else {
      val bound = args(0).toInt
      val (size, range) = params(bound)
      val exList = expected(bound).toList
      val gotList = makeBSTNodeBound(size, range)(new RealCache).toList
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
  }
}


