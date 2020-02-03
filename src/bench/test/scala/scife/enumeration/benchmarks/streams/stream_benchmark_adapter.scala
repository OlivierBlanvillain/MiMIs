package scife.enumeration.benchmarks.streams

import scife.enumeration.benchmarks.StructuresBenchmark
import scala.collection.immutable.Stream

abstract class StreamBenchmarkAdapter[T](val key: String) extends StructuresBenchmark[Unit] {
  import scife.enumeration.memoization.MemoizationScope

  def measureCode(u: Unit) = {
    (size: Int) => {
      makeStream(size).foreach(_ => ())
    }
  }

  def warmUp(u: Unit, maxSize: Int) {
    1.to(maxSize).foreach(size =>
      makeStream(size).foreach(_ => ()))
  }

  def constructEnumerator(implicit ms: MemoizationScope) = ()

  // ---BEGIN ABSTRACT MEMBERS---
  def makeStream(size: Int): Stream[T]
  // ---END ABSTRACT MEMBERS---
}    
    
