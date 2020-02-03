package scife
package enumeration
package benchmarks

import dependent._
import memoization._
import scife.{ enumeration => e }
import scife.util._

import scife.util.logging._

import structures._
import BSTrees._

import org.scalatest._
import org.scalameter.api._

import scala.language.existentials

class BinarySearchTreeBenchmark
  extends StructuresBenchmark[Depend[(Int, Range), Tree]]
//  extends PerformanceTest.OfflineReport with ProfileLogger
  {

  type EnumType = Depend[(Int, Range), Tree]

  import scala.collection.mutable.Buffer
  val killList = Buffer[MemoizationScope]()

  def measureCode(junk: EnumType) = {
    { (size: Int) =>
      // val enum = tdEnum.getEnum((size, 1 to size))
      val s = new scope.AccumulatingScope
      killList += s
      val enum = constructEnumerator(s).getEnum((size, 1.to(size)))
      // println("ESTIMATED ENUM BEFORE MEASURE SciFe (" + size + "): " +
      // 	      (com.madhukaraphatak.sizeof.SizeEstimator.estimate(enum) / 1024))
      for (i <- 0 until enum.size) enum(i)
      // println("ESTIMATED SCOPE AFTER MEASURE SciFe (" + size + "): " +
      // 	      (com.madhukaraphatak.sizeof.SizeEstimator.estimate(s) / 1024))
      // println("ESTIMATED ENUM AFTER MEASURE SciFe (" + size + "): " +
      // 	      (com.madhukaraphatak.sizeof.SizeEstimator.estimate(enum) / 1024))
    }
  }

  def warmUp(junk: EnumType, maxSize: Int) {
    for (size <- 1 to maxSize) {
      // val enum = inEnum.getEnum((size, 1 to size))
      val s = new scope.AccumulatingScope
      killList += s
      val enum = constructEnumerator(s).getEnum((size, 1.to(size)))
      for (i <- 0 until enum.size) enum(i)
    }
  }

  override def constructEnumerator(implicit ms: MemoizationScope) = {
    Depend.memoized(
      (self: Depend[(Int, Range), Tree], pair: (Int, Range)) => {
        val (size, range) = pair

        if (size <= 0) e.Singleton(Leaf)
        else if (size == 1)
          e.WrapArray(range map { v => Node(Leaf, v, Leaf) })
        else {
          val roots = e.Enum(range)
          val leftSizes = e.Enum(0 until size)

          val rootLeftSizePairs = e.Product(leftSizes, roots)

          val leftTrees: Depend[(Int, Int), Tree] = InMap(self, { (par: (Int, Int)) =>
            val (leftSize, median) = par
            (leftSize, range.start to (median - 1))
          })

          val rightTrees: Depend[(Int, Int), Tree] =
            InMap(self, { (par: (Int, Int)) =>
              val (leftSize, median) = par
              (size - leftSize - 1, (median + 1) to range.end)
            })

          val leftRightPairs: Depend[(Int, Int), (Tree, Tree)] =
            Product(leftTrees, rightTrees)

          val allNodes =
            memoization.Chain[(Int, Int), (Tree, Tree), Node](rootLeftSizePairs, leftRightPairs,
              (p1: (Int, Int), p2: (Tree, Tree)) => {
                val ((leftSize, currRoot), (leftTree, rightTree)) = (p1, p2)

                Node(leftTree, currRoot, rightTree)
              })

          allNodes
        }
      })
  }

  override def tearDown(size: Int, e: EnumType, memScope: MemoizationScope) {
    println("ESTIMATED TEARDOWN SciFe (" + size + "): " +
    	    (com.madhukaraphatak.sizeof.SizeEstimator.estimate(memScope) / 1024))
    super.tearDown(size, e, memScope)
    killList.foreach(_.clear)
    killList.clear()
  }

  override def setUp(size: Int, e: EnumType, memScope: MemoizationScope) {
    // println("ESTIMATED SETUP SciFe (" + size + "): " +
    // 	    (com.madhukaraphatak.sizeof.SizeEstimator.estimate(memScope) / 1024))
    super.setUp(size, e, memScope)
  }
}
