# MiMIs #

The MiMI implementation is embedded into SciFe's codebase.
The vast majority of the code in this codebase is from SciFe; the original SciFe codebase is [here](https://github.com/ikuraj/SciFe).
This was necessary for two reasons:

1. This ensured that we were running with the exact same JVM configuration, ensuring fair comparison to SciFe.
2. This allowed us to use SciFe's classes for representing data structures, allowing the MiMI benchmarks to use the exact same representation as SciFe does.
   This also helped ensure a fair comparison to SciFe was being made.

The original SciFe codebase was evaluated using [Scalameter](https://scalameter.github.io/).
However, this code bypasses scalameter entirely.
Scalameter was originally intended to ensure that results were statistically meaningful.
However, in our experimentation with Scalameter, we found that:

1. It can report alarmingly different runtimes from what the actual runtimes are
2. It would seemingly ignore configuration information
3. It would frequently repeat the same evaluation for reasons we could not determine, to the point where a benchmark taking 12 minutes might take days.
   Scalameter usually does this when it doesn't think it has enough statistically meaningful data yet, but this behavior is configurable to be turned off.
   Observing the data, we could not determine why Scalameter would think the data was yet insufficient.
   Additionally, Scalameter ignored configuration parameters (see 2 above) putting hard caps on these repeats.

## MiMI Implementation and Benchmarks ##

The specific code relevant to MiMIs is in `src/bench/test/scala/scife/enumeration/benchmarks/iterators/`.
In that directory:

- `iterators.scala` includes a lazily-defined `++` operation.
  This is not strictly necessary, and wasn't evaluated.
  This also includes `EmptyIterator`; this works identically to `Iterator()` in Scala.
- `caching_iterators.scala`: includes everything related to caching
- `ScalameterBypassEvaluation.scala`: code to evaluate MiMIs and SciFe, bypassing Scalameter.
- `benchmark_adapeter.scala`: allowed the MiMI code to be evaluated in a manner consistent with everything else in SciFe; this is no longer necessary (this was specific to using Scalameter)
- Every other file holds a benchmark implemented with MiMIs.

Everything related to the CLP benchmarks is in the toplevel `clp` directory.


