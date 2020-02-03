package scife.enumeration.benchmarks.iterators

import scife.enumeration.memoization.MemoizationScope
import scife.util.structures.riff.RiffFormat.{Chunk,
					      Leaf,
					      Payload,
					      Node}

import Iterators._

class RiffImageBenchmark(key: String, makeCache: () => IteratorCache[(Int, Int, Int, Int), Chunk]) extends BenchmarkAdapter[(Int, Int, Int, Int), Chunk](key, makeCache) {
  def makeIterator(size: Int, cache: IteratorCache[(Int, Int, Int, Int), Chunk]): Iterator[Chunk] = {
    Riff.makeRiff(size)(cache)
  }
  def makeMeasureIterator(size: Int, cache: IteratorCache[(Int, Int, Int, Int), Chunk]): Iterator[Chunk] = {
    makeIterator(size, cache)
  }
  def makeWarmUpIterator(size: Int, cache: IteratorCache[(Int, Int, Int, Int), Chunk]): Iterator[Chunk] = {
    makeIterator(size, cache)
  }
}

object Riff {
  def makeRiff(size: Int, dataSize: Int, jiffLoss: Int, avChunks: Int)(implicit cache: IteratorCache[(Int, Int, Int, Int), Chunk]): Iterator[Chunk] = {
    def uncachedIterator: Iterator[Chunk] = {
      if (size == 0 && dataSize == 0) Leaf
      else if (size == 1 && dataSize > 0 && avChunks <= 1 && (jiffLoss * 4) % dataSize == 0)
	Payload(dataSize, jiffLoss * 4 / dataSize, avChunks)
      else if (size > 1 && dataSize > 0) {
	for {
	  leftSize <- 0.until(size - 1).iterator
	  leftAudio <- math.max(0, avChunks - (size - leftSize - 1)).to(math.min(leftSize, avChunks)).iterator
	  leftDataSize <- 0.to(dataSize / 2).iterator
	  leftJiff <- math.max(0, jiffLoss - (dataSize - leftDataSize)).to(math.min(leftDataSize, jiffLoss)).iterator
	  leftTree <- makeRiff(leftSize, leftDataSize, leftJiff, leftAudio)
	  rightTree <- makeRiff(size - leftSize - 1,
				dataSize - leftDataSize,
				jiffLoss - leftJiff,
				avChunks - leftAudio)
	} yield Node(dataSize, leftTree, rightTree)
      } else EmptyIterator
    } // uncachedIterator

    if (size < 15) {
      cache.tryCache((size, dataSize, jiffLoss, avChunks), uncachedIterator)
    } else {
      uncachedIterator
    }
  } // makeRiff

  // for the initial call
  def makeRiff(size: Int)(implicit cache: IteratorCache[(Int, Int, Int, Int), Chunk]): Iterator[Chunk] = {
    makeRiff(size, size, (size + 1) / 2, size / 2)
  }

  def expected(size: Int, dataSize: Int, jiffLoss: Int, avChunks: Int): List[Chunk] = {
    import scife.enumeration.Enum
    import scife.enumeration.dependent.Depend
    import scife.enumeration.benchmarks.RiffImage
    val dep: Depend[(Int, Int, Int, Int), Chunk] =
      (new RiffImage).constructEnumerator
    val en: Enum[Chunk] = dep.apply((size, dataSize, jiffLoss, avChunks))
    en.toList
  }

  def expected(size: Int): List[Chunk] = {
    expected(size, size, (size + 1) / 2, size / 2)
  }

  // returns the number of structures in this space
  def walkThrough(size: Int, memo: MemoizationScope): Long = {
    import scife.enumeration.Enum
    import scife.enumeration.dependent.Depend
    import scife.enumeration.benchmarks.RiffImage

    val dataSize = size
    val jiffLoss = (size + 1) / 2
    val avChunks = size / 2
    var numStructures: Long = 0

    val dep: Depend[(Int, Int, Int, Int), Chunk] =
      (new RiffImage).constructEnumerator(memo)
    val en: Enum[Chunk] = dep.apply((size, dataSize, jiffLoss, avChunks))
    en.foreach(_ => numStructures += 1)
    numStructures
  }

  def main(args: Array[String]) {
    if (args.length != 1) {
      println("Needs a size bound")
    } else {
      val size = args(0).toInt
      val exList = expected(size)
      val gotList = makeRiff(size)(new RealCache).toList
      if (exList.size != gotList.size) {
	println("RAW LISTS DIFFERED IN SIZES")
      } else {
	println("NUM ELEMENTS: " + gotList.size)
	// println("ELEMENTS: " + gotList)
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
} // Riff
