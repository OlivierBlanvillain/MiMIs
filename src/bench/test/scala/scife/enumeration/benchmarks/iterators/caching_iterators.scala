package scife.enumeration.benchmarks.iterators

object IteratorCache {
  val DEFAULT_MAX_NUM_ELEMENTS = 1000
}

trait IteratorCache[K, V] {
  def tryCache(key: K, iterator: => Iterator[V], max: Int = IteratorCache.DEFAULT_MAX_NUM_ELEMENTS): Iterator[V]
}

class NoCache[K, V] extends IteratorCache[K, V] {
  def tryCache(key: K, iterator: => Iterator[V], max: Int = IteratorCache.DEFAULT_MAX_NUM_ELEMENTS): Iterator[V] = iterator
}

class RealCache[K, V] extends IteratorCache[K, V] {
  import scala.collection.mutable.{Buffer,
				   Map => MMap,
				   Set => MSet}

  // ---BEGIN CONSTRUCTOR---
  private val _cache: MMap[K, Buffer[V]] = MMap()
  private val _inFlight: MSet[K] = MSet()
  private var _doNotCache: MSet[K] = MSet()
  // ---END CONSTRUCTOR---

  private class CustomIterator(private val key: K,
			       private val around: Iterator[V],
			       private val max: Int) extends Iterator[V] {
    // ---BEGIN CONSTRUCTOR---
    private val save: Buffer[V] = Buffer()
    private var aborted: Boolean = false
    // ---END CONSTRUCTOR---

    def hasNext: Boolean = {
      val retval = around.hasNext
      if (!retval && !aborted) {
	// no more elements and we didn't abort saving
	// save these elements
	_cache += (key -> save)
	_inFlight -= key
      }
      retval
    }

    def next(): V = {
      val retval = around.next
      if (!aborted) {
	if (save.size < max) {
	  // have room to save another element
	  save += retval
	} else {
	// out of room.  Clear everything saved an abort.
	  save.clear()
	  aborted = true
	  _doNotCache += key
	  _inFlight -= key
	}
      }
      retval
    }
  } // CustomIterator

  def tryCache(key: K, iterator: => Iterator[V], max: Int = IteratorCache.DEFAULT_MAX_NUM_ELEMENTS): Iterator[V] = {
    if (_cache.contains(key)) {
      _cache(key).iterator
    } else if (!_inFlight(key) && !_doNotCache(key)) {
      _inFlight += key
      new CustomIterator(key, iterator, max)
    } else {
      iterator
    }
  } // tryCache

  // ---BEGIN TESTING-RELATED METHODS---
  def cache: Map[K, Seq[V]] = _cache.toMap.mapValues(_.toSeq)
  def inFlight: Set[K] = _inFlight.toSet
  def doNotCache: Set[K] = _doNotCache.toSet
  // ---END TESTING-RELATED METHODS---
} // RealCache

class RealCacheForceSize[K, V](val forcedSize: Int) extends IteratorCache[K, V] {
  private val cache: IteratorCache[K, V] = new RealCache()

  def tryCache(key: K, iterator: => Iterator[V], max: Int = IteratorCache.DEFAULT_MAX_NUM_ELEMENTS): Iterator[V] = {
    cache.tryCache(key, iterator, forcedSize)
  }
}
