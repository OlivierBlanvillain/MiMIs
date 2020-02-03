package scife.enumeration
package benchmarks
package suite

import benchmarks._

import org.scalameter._

import reporting._
import execution._
import Key._

// if set, does not run full-blown micro-benchmark test suite; it runs
// a quicker benchmark with less reliable results

class BenchmarkSuiteMinimal extends PerformanceTest.OfflineReport {
  override def persistor = new persistence.SerializationPersistor

  import BenchmarkSuite._

  val benchmarks = List(
//    (new BinarySearchTreeBenchmark, "Binary Search Trees"),
//    (new SortedListDependentBenchmark, "Sorted Lists"),
//    (new RedBlackTreeDependentBenchmark, "Red-Black Trees"),
//    (new HeapArrayBenchmark, "Heap Arrays"),
    (new scife.enumeration.parallel.BinarySearchTreeBenchmark(Runtime.getRuntime.availableProcessors/2),
      "Binary Search Trees - parallel")
    )

  implicit val configArguments = contextMinimal

  for (((benchmark, name), maxSize) <- benchmarks zip minimalSizes)
    benchmark.fixture("Minimal benchmarks", name, maxSize)

}

class BenchmarkSuiteFull extends PerformanceTest {
  override def persistor = new persistence.SerializationPersistor

  override def reporter: Reporter =
    new SciFeReporter(
      Reporter.Composite(
        new RegressionReporter(
          RegressionReporter.Tester.OverlapIntervals(),
          RegressionReporter.Historian.Complete()),
        // do not embed data into js
        HtmlReporter(false)))

  def executor = SeparateJvmsExecutor(
    Executor.Warmer.Default(),
    Aggregator.min,
    new Executor.Measurer.Default)

  import BenchmarkSuite._

  implicit val configArguments = configArgumentsFull

  for ( (name, benchmark, maxSize) <- allBenchmarks)
    benchmark.fixtureRun(benchmarkMainName, "SciFe", maxSize, name)

//  val dummyBenchmark = new DummyBenchmark
//
//  for ((name, maxSize) <- allBenchmarksNames zip fullBlownSizes)
//    dummyBenchmark.fixtureRun(benchmarkMainName, "Korat", maxSize, name)
//
//  for ((name, maxSize) <- clpBenchmarksNames zip fullBlownSizes)
//    dummyBenchmark.fixtureRun(benchmarkMainName, "CLP", maxSize, name)

}

class BenchmarkSuiteParallel extends PerformanceTest {

  override def executor = SeparateJvmsExecutor(
    Executor.Warmer.Default(),
    Aggregator.min,
    new Executor.Measurer.Default)

  import BenchmarkSuite._

  implicit val configArguments = configArgumentsFull +
    (exec.jvmflags -> (JVMFlags ++ heapSize(10)).mkString(" "))
    
  import scife.enumeration.parallel._
    
  val benchmarks = List(
//    ("Binary Search Trees - parallel",
//      new BinarySearchTreeBenchmark(_: Int), 15),
    ("Riff Image - parallel", new RiffImage(_: Int), 12)
  )
  
//  val parallelBenchmarks =
//    new scife.enumeration.parallel.BinarySearchTreeBenchmark(Runtime.getRuntime.availableProcessors/2) :: Nil
//    
//  val benchmarkNames = "Binary Search Trees - parallel" :: Nil
//
//  val benchmarkSizes = 15 :: Nil
    
//  for (threads <- 1 to Runtime.getRuntime.availableProcessors/2) {
  for (threads <- 5 to 10) {
    for ((name, benchmark, maxSize) <- benchmarks)
      benchmark(threads).fixtureRun(benchmarkMainName, "SciFe", maxSize, s"$name/$threads")
  }

//  for (((benchmark, name), maxSize) <- allBenchmarks zip allBenchmarksNames zip fullBlownSizes)
//    benchmark.fixtureRun(benchmarkMainName, "SciFe", maxSize, name)
    
  //override def reporter = new LoggingReporter
  override def reporter =
    Reporter.Composite(
        new RegressionReporter(
          RegressionReporter.Tester.OverlapIntervals(),
          RegressionReporter.Historian.Complete()),
        // do not embed data into js
        HtmlReporter(false))
  
  override def persistor =
    //Persistor.None
    new persistence.SerializationPersistor

}

// benchmarks for which it may take a while to finish (e.g. ones without memoization)
class BenchmarkSuiteSlow extends PerformanceTest {
  override def persistor = api.Persistor.None

  def executor = SeparateJvmsExecutor(
    Executor.Warmer.Default(),
    Aggregator.min,
    // new Executor.Measurer.MemoryFootprint)
    new Executor.Measurer.Default)

  def reporter = new LoggingReporter

  import BenchmarkSuite._

  implicit val configArguments =
    org.scalameter.Context(
      exec.minWarmupRuns -> 1,
      exec.maxWarmupRuns -> 1,
      exec.outliers.retries -> 0,
      exec.benchRuns -> 3,
      exec.independentSamples -> 1,
      exec.jvmcmd -> javaCommand,
      exec.jvmflags -> (JVMFlags ++ heapSize(30)).mkString(" "))

  import scife.enumeration.benchmarks.iterators._

  // val sizes = 8.to(14)
  // val benchmarks = Seq((new BinarySearchTreeBenchmark, "Binary Search Tree (w/ mem)"),
  // 		       (new BSTBenchmark("dummy", () => new NoCache), "Binary Search Tree (iterators - no caching)"),
  // 		       (new BSTBenchmark("real", () => new RealCache), "Binary Search Tree (iterators - caching)"))

  // val sizes = 14.to(18)
  // val benchmarks = Seq((new RedBlackTreeConcise, "SciFe Red-Black Tree"),
  // 		       (new RBTreeBenchmark("dummy", () => new NoCache), "Iterators (NO caching) Red-Black Tree"),
  // 		       (new RBTreeBenchmark("real", () => new RealCache), "Iterators (caching) Red-Black Tree"))

  // bound 11 takes a LONG time
  // val sizes = 7.to(11)
  // val benchmarks = Seq((new HeapArrayBenchmark, "SciFe Heap Arrays"),
  // 		       (new HeapArraysBenchmark("dummy", () => new NoCache), "Iterators (NO caching) Heap Arrays"),
  // 		       (new HeapArraysBenchmark("real - 1000", () => new RealCacheForceSize(1000)), "Iterators (caching - 1000) Heap Arrays"),
  // 		       (new HeapArraysBenchmark("real - 10000", () => new RealCacheForceSize(10000)), "Iterators (caching - 10000) Heap Arrays"),
  // 		       (new HeapArraysBenchmark("real - 100000", () => new RealCacheForceSize(100000)), "Iterators (caching - 100000) Heap Arrays"),
  // 		       (new HeapArraysBenchmark("real - 1000000", () => new RealCacheForceSize(1000000)), "Iterators (caching - 1000000) Heap Arrays"),
  // 		       (new HeapArraysBenchmark("real - 10000000", () => new RealCacheForceSize(10000000)), "Iterators (caching - 10000000) Heap Arrays"),
  // 		       (new HeapArraysBenchmark("real - 100000000", () => new RealCacheForceSize(100000000)), "Iterators (caching - 100000000) Heap Arrays"))

  // val sizes = 21.to(30)
  // val benchmarks = Seq((new BTreeTest, "SciFe B-Trees"),
  // 		       (new BTreesBenchmark("dummy", () => new NoCache), "Iterators (NO caching) B-Trees"),
  // 		       (new BTreesBenchmark("real - 1000", () => new RealCacheForceSize(1000)), "Iterators (caching - 1000) B-Trees"),
  // 		       (new BTreesBenchmark("real - 10000", () => new RealCacheForceSize(10000)), "Iterators (caching - 10000) B-Trees"),
  // 		       (new BTreesBenchmark("real - 100000", () => new RealCacheForceSize(100000)), "Iterators (caching - 100000) B-Trees"),
  // 		       (new BTreesBenchmark("real - 1000000", () => new RealCacheForceSize(1000000)), "Iterators (caching - 1000000) B-Trees"),
  // 		       (new BTreesBenchmark("real - 10000000", () => new RealCacheForceSize(10000000)), "Iterators (caching - 10000000) B-Trees"),
  // 		       (new BTreesBenchmark("real - 100000000", () => new RealCacheForceSize(100000000)), "Iterators (caching - 100000000) B-Trees"))

  // val sizes = 12.to(14)
  // val benchmarks = Seq((new RiffImage, "SciFe Riff Images"),
  // 		       (new RiffImageBenchmark("dummy", () => new NoCache), "Iterators (NO caching) Riff Images"),
  // 		       (new RiffImageBenchmark("real - 1000", () => new RealCacheForceSize(1000)), "Iterators (caching - 1000) Riff Images"),
  // 		       (new RiffImageBenchmark("real - 10000", () => new RealCacheForceSize(10000)), "Iterators (caching - 10000) Riff Images"),
  // 		       (new RiffImageBenchmark("real - 100000", () => new RealCacheForceSize(100000)), "Iterators (caching - 100000) Riff Images"),
  // 		       (new RiffImageBenchmark("real - 1000000", () => new RealCacheForceSize(1000000)), "Iterators (caching - 1000000) Riff Images"),
  // 		       (new RiffImageBenchmark("real - 10000000", () => new RealCacheForceSize(10000000)), "Iterators (caching - 10000000) Riff Images"),
  // 		       (new RiffImageBenchmark("real - 100000000", () => new RealCacheForceSize(100000000)), "Iterators (caching - 100000000) Riff Images"))

  type LowLevelBenchmark = DependentMemoizedBenchmark[Int, _]
  case class Benchmark(name: String,
		       sizes: Range,
		       scife: LowLevelBenchmark,
		       scifeLimit: Int,
		       mimi: LowLevelBenchmark) {
    def run() {
      def runBenchmark(bench: LowLevelBenchmark, benchName: String, size: Int) {
	val fullName = name + "; " + benchName + "; " + size
	bench.fixtureRun(benchmarkMainName, fullName, size, fullName)
      }
	
      sizes.foreach(size => {
	if (size <= scifeLimit) {
	  runBenchmark(scife, "SciFe", size)
	}
	runBenchmark(mimi, "MiMI", size)
      })
    }
  }

  val k = 100000 // for MiMIs
  def makeCache[A, B](): IteratorCache[A, B] = new RealCacheForceSize(k)
  
  val benchmarks = Seq(// Benchmark("BSTs",
		       // 		 8.to(18),
		       // 		 new BinarySearchTreeBenchmark,
		       // 		 16,
		       // 		 new BSTBenchmark("real", makeCache _)),
		       // Benchmark("RBTs",
		       // 		 14.to(24),
		       // 		 new RedBlackTreeConcise,
		       // 		 19,
		       // 		 new RBTreeBenchmark("real", makeCache _)),
		       // Benchmark("B-Trees",
		       // 		 28.to(41),
		       // 		 new BTreeTest,
		       // 		 36,
		       // 		 new BTreesBenchmark("real", makeCache _)),
		       Benchmark("RIFF Images",
				 12.to(21),
				 new RiffImage,
				 16,
				 new RiffImageBenchmark("real", makeCache _)),
		       Benchmark("Heaps",
		       		 5.to(11),
		       		 new HeapArrayBenchmark,
		       		 11,
		       		 new HeapArraysBenchmark("real", makeCache _)))

  benchmarks.foreach(_.run())
    
  // for (
  //   (benchmark, name, maxSize) <- List(
  //     // (new nomemoization.BinarySearchTreeBenchmark, "Binary Search Tree", 10),
  //     (new BinarySearchTreeBenchmark, "Binary Search Tree (w/ mem)", 14),
  //     (new BSTBenchmark(() => new NoCache), "Binary Search Tree (iterators - no caching)", 14),
  //     (new BSTBenchmark(() => new RealCache), "Binary Search Tree (iterators - caching)", 14))
  // ) benchmark.fixtureRun(benchmarkMainName, "SciFe (no memoization)", maxSize, name)
}

class DummyBenchmark extends PerformanceTest.OfflineReport {

  def fixtureRun(
    benchmarkMainName: String,
    name: String,
    maxSize: Int,
    run: String)(implicit configArguments: org.scalameter.Context) = {
    require(name != null)

    performance of benchmarkMainName in {
      measure method run in {
        using(Gen.range("size")(1, maxSize, 1)) config (
          configArguments) curve (name) warmUp {
          } in { _ => }
      }
    }
  }
}

object BenchmarkSuite {

  val benchmarkMainName = "Benchmarks"
  
  val allBenchmarks = List(
   ("Binary Search Trees", new BinarySearchTreeBenchmark, 15),
   ("Sorted Lists", new SortedListDependentBenchmark, 15),
   ("Red-Black Trees", new RedBlackTreeDependentBenchmark, 15),
   ("Red-Black Trees", new RedBlackTreeConcise, 15),
   ("Heap Arrays", new HeapArrayBenchmark, 11),
   ("Directed Acyclic Graph", new DAGStructureBenchmark, 4),
   ("B-tree", new BTreeTest, 15),
   ("RIFF Format", new RiffImage, 3),
   // ("Lazy BST", (new scife.enumeration.lazytraversal.BinarySearchTree:
   //   StructuresBenchmark[scife.enumeration.dependent.Depend[((Int, Range),
   //     scife.enumeration.lazytraversal.LazyEnum[scife.util.structures.LazyBSTrees.Tree]),
   //     scife.util.structures.LazyBSTrees.Tree]]), 15),
   ("Normal BST, testing", new scife.enumeration.lazytraversal.BinarySearchTreeNormal, 15),
   // ("Lazy BST", new scife.enumeration.lazytraversal.BinarySearchTree, 14),
   ("Normal BST, testing2", new scife.enumeration.lazytraversal.BinarySearchTreeNormal2, 15),
   ("Binary Search Trees rnd", new BinarySearchTreeRandom, 15),
   ("Binary Search Trees rnd, noo", new BinarySearchTreeRandomNoOver, 15),
   ("Binary Search Trees no memoization", new nomemoization.BinarySearchTreeBenchmark, 15)
  )

//  val allBenchmarks = List(
//    new BinarySearchTreeBenchmark,
//    new SortedListDependentBenchmark,
//    new RedBlackTreeDependentBenchmark,
//    new HeapArrayBenchmark,
//    new DAGStructureBenchmark,
//    new BTreeTest,
//    new RiffImage
//  )
//
//  val allBenchmarksNames = List(
//    "Binary Search Tree",
//    "Sorted List",
//    "Red-Black Tree",
//    "Heap Array",
//    "Directed Acyclic Graph",
////    "Class-Interface DAG",
//    "B-tree",
//    "RIFF Format"
//  )

  val clpBenchmarksNames = List(
    "Binary Search Tree",
    "Sorted List",
    "Red-Black Tree",
    "Heap Array")

  var maxSize = 15

  // max datastructure size
  val minimalSizes = Stream.continually(3)
  val fullBlownSizes = List(15, 15, 15, 11, 4, 15, 3)
  // normal executor options
  val warmUps = 8
  val numberOfRuns = 3
  val JVMs = 3

  //  val fullBlownSizes = List(3, 3, 3, 3, 3)
  //  val warmUps = 1; val numberOfRuns = 3; val JVMs = 1

  lazy val javaCommand = "/usr/lib/jvm/java-7-openjdk-amd64/bin/java -server"
  lazy val JVMFlags = List(
    // not sure if we should repeat this flag
    "-server",
    // print important outputs
    //    "-XX:+PrintCompilation",
    // verbose GC
    //    "-verbose:gc", "-XX:+PrintGCTimeStamps", "-XX:+PrintGCDetails",
    // compilation
    "-Xbatch",
    // explicit GC calls we need
    "-XX:-DisableExplicitGC",
    //    "--XX:CICompilerCount=1",
    // optimizations
    "-XX:ReservedCodeCacheSize=512M",
    "-XX:CompileThreshold=10", "-XX:+TieredCompilation",
    "-XX:+AggressiveOpts", "-XX:MaxInlineSize=512",
    // disable adaptive policy
    "-XX:-UseAdaptiveSizePolicy"
//    "-XX:MinHeapFreeRatio=80",
//    "-XX:MaxHeapFreeRatio=100"
  )

  def heapSize(s: Int) = List(
    // new generation size
//    s"-XX:NewSize=${s-2}G",
    s"-Xms${s}G", s"-Xmx${s}G"
  )
  //  println("JVM FLags: " + JVMFlags.mkString(" "))

  val configArgumentsFull =
    org.scalameter.Context(
      exec.maxWarmupRuns -> 3,
      exec.benchRuns -> 3,
      exec.independentSamples -> 1,
      exec.jvmcmd -> javaCommand,
      exec.jvmflags -> (JVMFlags ++ heapSize(32)).mkString(" "))
      
  val contextMinimal =
    org.scalameter.Context(
      exec.maxWarmupRuns -> 2,
      exec.benchRuns -> 3,
      exec.independentSamples -> 1)

}
