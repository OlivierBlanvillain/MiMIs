package scife
package enumeration
package benchmarks

import dependent._
import scife.{ enumeration => e }
import memoization._

import scife.util._
import scife.util.logging._
import structures.RedBlackTrees._

import org.scalatest._
import org.scalameter.api._

import scala.language.postfixOps
import scala.language.existentials

class RedBlackTreeConcise
  extends StructuresBenchmark[Depend[(Int, Range, Range, Int), Tree]]
  with java.io.Serializable {

  type EnumType = Depend[(Int, Range, Range, Int), Tree]

  import scala.collection.mutable.Buffer
  val killList = Buffer[MemoizationScope]()

  def measureCode(junk: EnumType) = {
    { (size: Int) =>
      val s = new scope.AccumulatingScope
      killList += s
      for (
        blackHeight <- blackHeightRange(size);
        enum = constructEnumerator(s).getEnum(size, 1 to size, 0 to 1, blackHeight);
        ind <- 0 until enum.size
      ) enum(ind)
    }
  }

  def warmUp(junk: EnumType, maxSize: Int) {
    val s = new scope.AccumulatingScope
    killList += s

    for (size <- 1 to maxSize) {
      for (
        blackHeight <- 1 to (Math.log2(size + 1).toInt + 1);
        enum = constructEnumerator(s).getEnum(size, 1 to size, 0 to 1, blackHeight);
        ind <- 0 until enum.size
      ) enum(ind)
    }
  }
  
  def constructEnumerator(implicit ms: MemoizationScope) = {
    val noScope = scope.NoScope
    val enum = e.common.enumdef.RedBlackTreeEnum.constructEnumerator_concise(noScope)
    ms.add(enum.asInstanceOf[Memoizable])
    enum
  }

  override def tearDown(size: Int, e: EnumType, memScope: MemoizationScope) {
    println("ESTIMATED TEARDOWN SciFe (" + size + "): " +
    	    (com.madhukaraphatak.sizeof.SizeEstimator.estimate(killList) / 1024))
    super.tearDown(size, e, memScope)
    killList.foreach(_.clear)
    killList.clear()
  }
}
