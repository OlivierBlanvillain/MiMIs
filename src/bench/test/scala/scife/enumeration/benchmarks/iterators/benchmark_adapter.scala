package scife.enumeration.benchmarks.iterators

import scife.enumeration.benchmarks.StructuresBenchmark

abstract class BenchmarkAdapter[K, V](val key: String, val makeCache: () => IteratorCache[K, V]) extends StructuresBenchmark[Unit] {
  private var cache: Option[IteratorCache[K, V]] = None

  import scife.enumeration.memoization.MemoizationScope

  def measureCode(u: Unit) = {
    (size: Int) => {
      assert(cache.isEmpty)
      val useCache = makeCache()
      cache = Some(useCache)
      makeMeasureIterator(size, useCache).foreach(_ => ())
    }
  }

  def warmUp(u: Unit, maxSize: Int) {
    1.to(maxSize).foreach(size =>
      makeWarmUpIterator(size, makeCache()).foreach(_ => ()))
  }

  override def tearDown(size: Int, u: Unit, memScope: MemoizationScope) {
    super.tearDown(size, u, memScope)
    assert(cache.isDefined)
    println("ESTIMATED CACHE SIZE (" + key + ", " + size + "): " +
	    (com.madhukaraphatak.sizeof.SizeEstimator.estimate(cache.get) / 1024))
    cache = None
  }

  def constructEnumerator(implicit ms: MemoizationScope) = ()

  // ---BEGIN ABSTRACT MEMBERS---
  def makeMeasureIterator(size: Int, cache: IteratorCache[K, V]): Iterator[V]
  def makeWarmUpIterator(size: Int, cache: IteratorCache[K, V]): Iterator[V]
  // ---END ABSTRACT MEMBERS---
}    
    
