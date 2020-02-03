package scife.enumeration.benchmarks.streams

import scife.util.structures.riff.RiffFormat.{Chunk,
					      Leaf,
					      Payload,
					      Node}

import scala.collection.immutable.Stream

class StreamRiffImageBenchmark(key: String) extends StreamBenchmarkAdapter[Chunk](key) {
  def makeStream(size: Int): Stream[Chunk] = StreamRiff.makeRiff(size)
}

object StreamRiff {
  import scife.enumeration.benchmarks.iterators.Riff.expected

  def makeRiff(size: Int, dataSize: Int, jiffLoss: Int, avChunks: Int): Stream[Chunk] = {
    if (size == 0 && dataSize == 0) Stream(Leaf)
    else if (size == 1 && dataSize > 0 && avChunks <= 1 && (jiffLoss * 4) % dataSize == 0)
      Stream(Payload(dataSize, jiffLoss * 4 / dataSize, avChunks))
    else if (size > 1 && dataSize > 0) {
      for {
	leftSize <- 0.until(size - 1).toStream
	leftAudio <- math.max(0, avChunks - (size - leftSize - 1)).to(math.min(leftSize, avChunks)).toStream
	leftDataSize <- 0.to(dataSize / 2).toStream
	leftJiff <- math.max(0, jiffLoss - (dataSize - leftDataSize)).to(math.min(leftDataSize, jiffLoss)).toStream
	leftTree <- makeRiff(leftSize, leftDataSize, leftJiff, leftAudio)
	rightTree <- makeRiff(size - leftSize - 1,
			      dataSize - leftDataSize,
			      jiffLoss - leftJiff,
			      avChunks - leftAudio)
      } yield Node(dataSize, leftTree, rightTree)
    } else Stream.Empty
  } // makeRiff

  // for the initial call
  def makeRiff(size: Int): Stream[Chunk] = {
    makeRiff(size, size, (size + 1) / 2, size / 2)
  }

  def main(args: Array[String]) {
    if (args.length != 1) {
      println("Needs a size bound")
    } else {
      val size = args(0).toInt
      val exList = expected(size)
      val gotList = makeRiff(size).toList
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
    }
  } // main
} // Riff
