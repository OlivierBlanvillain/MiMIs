package scife.enumeration.benchmarks.iterators

import scife.enumeration.memoization.{MemoizationScope, scope}
import scife.enumeration.Enum

// We have had so many problems rooted in Scalameter
// doing nonsensical things that I'm bypassing it
// entirely.  It's so complicated that I cannot
// reason about anything that's happening, but
// I can conclusively show that it's consistently
// making ~12 minute benchmarks last hours.

object ScalameterBypassEvaluation {
  val TIMEOUT_IN_MINUTES = 60
  val defaultK = 100000 // for MiMIs

  def getBoundAndK(args: Array[String]): (Int, Int) = {
    args.length match {
      case 1 => (args(0).toInt, defaultK)
      case 2 => (args(0).toInt, args(1).toInt)
      case _ => throw new Exception("Needs a size bound and an optional k value")
    }
  }

  def timeWithTimeout(a: => Unit) {
    import scala.concurrent._
    import scala.concurrent.duration._
    import ExecutionContext.Implicits.global

    lazy val f = Future {
      val start = System.currentTimeMillis()
      val result = a
      val end = System.currentTimeMillis()
      val time = end - start
      println(time.toString + " ms")
    }
    Await.result(f, TIMEOUT_IN_MINUTES.minutes)
  }

  def printSize(of: AnyRef) {
    val kb = com.madhukaraphatak.sizeof.SizeEstimator.estimate(of) / 1024
    println(kb.toString + " kB")
  }

  def sciFeMain[A](
    benchmarkName: String,
    walkThrough: (Int, MemoizationScope) => Long,
    args: Array[String]) {

    val (bound, `defaultK`) = getBoundAndK(args)
    println(benchmarkName + "; SciFe; " + bound)
    val memo = new scope.AccumulatingScope
    timeWithTimeout {
      println("Num structures: " + walkThrough(bound, memo))
    }
    printSize(memo)
  }

  def mimiMain[A, B](
    benchmarkName: String,
    makeMimi: (Int, IteratorCache[A, B]) => Iterator[B],
    args: Array[String]) {

    val (bound, k) = getBoundAndK(args)
    println(benchmarkName + "; MiMI; " + bound)
    val memo: IteratorCache[A, B] = new RealCacheForceSize(k)
    timeWithTimeout {
      var numStructures: Long = 0
      makeMimi(bound, memo).foreach(_ => numStructures += 1)
      println("Num structures: " + numStructures)
    }
    printSize(memo)
  }

  def streamMain[A](
    benchmarkName: String,
    makeStream: Int => Stream[A],
    args: Array[String]) {

    val (bound, `defaultK`) = getBoundAndK(args)
    println(benchmarkName + "; Stream; " + bound)
    timeWithTimeout {
      var numStructures: Long = 0
      makeStream(bound).foreach(_ => numStructures += 1)
      println("Num structures: " + numStructures)
    }
    println("0 kB")
  }
}
import ScalameterBypassEvaluation._

object BSTSciFeEvaluate {
  def main(args: Array[String]) {
    sciFeMain(
      "BSTs",
      (bound, memo) => {
	var numStructures: Long = 0
	BST.makeEnumerator(bound, memo).foreach(_ => numStructures += 1)
	numStructures
      },
      args)
  }
}

object BSTMiMIEvaluate {
  def main(args: Array[String]) {
    import scife.util.structures.BSTrees.Tree
    import scala.collection.immutable.Range.Inclusive
    mimiMain(
      "BSTs",
      (bound: Int, memo: IteratorCache[(Int, Inclusive), Tree]) => {
	val (size, range) = BST.params(bound)
	BST.makeBSTNodeBound(size, range)(memo)
      },
      args)
  }
}

object BSTStreamEvaluate {
  def main(args: Array[String]) {
    streamMain(
      "BSTs",
      (bound: Int) => scife.enumeration.benchmarks.streams.StreamBST.makeBSTNodeBound(bound, 1.to(bound)),
      args)
  }
}
      
object RBTSciFeEvaluate {
  def main(args: Array[String]) {
    sciFeMain(
      "RBTs",
      RBTree.walkThrough,
      args)
  }
}

object RBTMiMIEvaluate {
  def main(args: Array[String]) {
    import scife.util.structures.RedBlackTrees.Tree
    import scala.collection.immutable.Range.Inclusive
    mimiMain(
      "RBTs",
      (bound: Int, memo: IteratorCache[(Int, Inclusive, Inclusive, Int), Tree]) => {
	RBTree.makeRBTreeMeasure(bound)(memo)
      },
      args)
  }
}

object RBTStreamEvaluate {
  def main(args: Array[String]) {
    streamMain(
      "RBTs",
      (bound: Int) => scife.enumeration.benchmarks.streams.StreamRBTree.makeRBTreeMeasure(bound),
      args)
  }
}

object HeapSciFeEvaluate {
  def main(args: Array[String]) {
    sciFeMain(
      "Heaps",
      HeapArrays.walkThrough,
      args)
  }
}

object HeapMiMIEvaluate {
  def main(args: Array[String]) {
    import scife.util.structures.BSTrees.Tree
    mimiMain(
      "Heaps",
      (bound: Int, memo: IteratorCache[(Int, Range), Tree]) => {
	HeapArrays.makeHeap(bound)(memo)
      },
      args)
  }
}

object HeapStreamEvaluate {
  def main(args: Array[String]) {
    streamMain(
      "Heaps",
      (bound: Int) => scife.enumeration.benchmarks.streams.StreamHeapArrays.makeHeap(bound),
      args)
  }
}

object BTreeSciFeEvaluate {
  def main(args: Array[String]) {
    sciFeMain(
      "B-Trees",
      BTrees.walkThrough,
      args)
  }
}

object BTreeMiMIEvaluate {
  def main(args: Array[String]) {
    import scife.util.structures.BTree.Tree

    mimiMain(
      "B-Trees",
      (bound: Int, memo: IteratorCache[Any, Tree]) => {
	BTrees.makeRootBTree(bound)(memo)
      },
      args)
  }
}

object BTreeStreamEvaluate {
  def main(args: Array[String]) {
    streamMain(
      "B-Trees",
      (bound: Int) => scife.enumeration.benchmarks.streams.StreamBTrees.makeRootBTree(bound),
      args)
  }
}

object RiffSciFeEvaluate {
  def main(args: Array[String]) {
    sciFeMain(
      "Riff",
      Riff.walkThrough,
      args)
  }
}

object RiffMiMIEvaluate {
  def main(args: Array[String]) {
    import scife.util.structures.riff.RiffFormat.Chunk

    mimiMain(
      "Riff",
      (bound: Int, memo: IteratorCache[(Int, Int, Int, Int), Chunk]) => {
	Riff.makeRiff(bound)(memo)
      },
      args)
  }
}

object RiffStreamEvaluate {
  def main(args: Array[String]) {
    streamMain(
      "Riff",
      (bound: Int) => scife.enumeration.benchmarks.streams.StreamRiff.makeRiff(bound),
      args)
  }
}

object JavaSciFeEvaluate {
  def main(args: Array[String]) {
    sciFeMain(
      "Java",
      scife.enumeration.benchmarks.JavaScifeGeneration.walkThrough,
      args)
  }
}

object JavaMiMIEvaluate {
  import scife.enumeration.benchmarks.java.JavaTest
  // var hack = List[JavaTest]()

  def main(args: Array[String]) {
    val (bound, k) = getBoundAndK(args)
    println("Java; MiMI; " + bound)
    val generator = JavaGeneration.apply(k)
    timeWithTimeout {
      var numStructures: Long = 0
      generator.makeTest(bound).foreach(s => {
	// hack = s :: hack
	numStructures += 1
      })
      println("Num structures: " + numStructures)
    }
    printSize(generator.allCaches)
  }
}

object JavaStreamEvaluate {
  def main(args: Array[String]) {
    streamMain(
      "Java",
      (bound: Int) => scife.enumeration.benchmarks.streams.StreamJavaGeneration().makeTest(bound),
      args)
  }
}
