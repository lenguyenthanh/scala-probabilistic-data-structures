package se.thanh.pds.bloomfilter
package benchmark

import bloomfilter.mutable.BloomFilter as OriginalBloomFilter
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.*

@Fork(
  value = 1,
  jvmArgsAppend = Array("-Xmx1G", "--add-opens=java.base/java.lang=ALL-UNNAMED")
)
@State(Scope.Benchmark)
class LongItemBenchmark:

  @Param(Array("10"))
  var tokens: Int = compiletime.uninitialized

  private val itemsExpected     = 1000000L
  private val falsePositiveRate = 0.01
  private val item              = 1L

  private var implementation: String                  = compiletime.uninitialized
  private var onHeap: BloomFilter[Long]               = compiletime.uninitialized
  private var ffmOffHeap: OffHeapBloomFilter[Long]    = compiletime.uninitialized
  private var unsafeOffHeap: OffHeapBloomFilter[Long] = compiletime.uninitialized
  private var original: OriginalBloomFilter[Long]     = compiletime.uninitialized

  @Setup(Level.Trial)
  def setup(benchmarkParams: BenchmarkParams): Unit =
    val benchmark = benchmarkParams.getBenchmark.split('.').last
    implementation =
      if benchmark.startsWith("onHeap") then
        onHeap = BloomFilter[Long](itemsExpected, falsePositiveRate)
        onHeap.add(item)
        "onHeap"
      else if benchmark.startsWith("ffmOffHeap") then
        ffmOffHeap = BenchmarkBloomFilter.foreignMemory[Long](itemsExpected, falsePositiveRate)
        ffmOffHeap.add(item)
        "ffmOffHeap"
      else if benchmark.startsWith("unsafeOffHeap") then
        unsafeOffHeap = BenchmarkBloomFilter.unsafe[Long](itemsExpected, falsePositiveRate)
        unsafeOffHeap.add(item)
        "unsafeOffHeap"
      else if benchmark.startsWith("original") then
        original = OriginalBloomFilter[Long](itemsExpected, falsePositiveRate)
        original.add(item)
        "original"
      else throw new IllegalArgumentException(s"unknown benchmark: $benchmark")

  @TearDown(Level.Trial)
  def tearDown(): Unit =
    implementation match
      case "ffmOffHeap"    => ffmOffHeap.close()
      case "unsafeOffHeap" => unsafeOffHeap.close()
      case "original"      => original.dispose()
      case _               => ()

  @Benchmark
  @OperationsPerInvocation(LongItemBenchmark.invocation)
  def onHeapAdd(blackhole: Blackhole): Unit =
    var index = 0
    while index < LongItemBenchmark.invocation do
      onHeap.add(item)
      blackhole.consume(item)
      Blackhole.consumeCPU(tokens)
      index += 1

  @Benchmark
  @OperationsPerInvocation(LongItemBenchmark.invocation)
  def onHeapGet(blackhole: Blackhole): Unit =
    var index = 0
    while index < LongItemBenchmark.invocation do
      blackhole.consume(onHeap.contains(item))
      Blackhole.consumeCPU(tokens)
      index += 1

  @Benchmark
  @OperationsPerInvocation(LongItemBenchmark.invocation)
  def ffmOffHeapAdd(blackhole: Blackhole): Unit =
    var index = 0
    while index < LongItemBenchmark.invocation do
      ffmOffHeap.add(item)
      blackhole.consume(item)
      Blackhole.consumeCPU(tokens)
      index += 1

  @Benchmark
  @OperationsPerInvocation(LongItemBenchmark.invocation)
  def ffmOffHeapGet(blackhole: Blackhole): Unit =
    var index = 0
    while index < LongItemBenchmark.invocation do
      blackhole.consume(ffmOffHeap.contains(item))
      Blackhole.consumeCPU(tokens)
      index += 1

  @Benchmark
  @OperationsPerInvocation(LongItemBenchmark.invocation)
  def unsafeOffHeapAdd(blackhole: Blackhole): Unit =
    var index = 0
    while index < LongItemBenchmark.invocation do
      unsafeOffHeap.add(item)
      blackhole.consume(item)
      Blackhole.consumeCPU(tokens)
      index += 1

  @Benchmark
  @OperationsPerInvocation(LongItemBenchmark.invocation)
  def unsafeOffHeapGet(blackhole: Blackhole): Unit =
    var index = 0
    while index < LongItemBenchmark.invocation do
      blackhole.consume(unsafeOffHeap.contains(item))
      Blackhole.consumeCPU(tokens)
      index += 1

  @Benchmark
  @OperationsPerInvocation(LongItemBenchmark.invocation)
  def originalAdd(blackhole: Blackhole): Unit =
    var index = 0
    while index < LongItemBenchmark.invocation do
      blackhole.consume(original.add(item))
      Blackhole.consumeCPU(tokens)
      index += 1

  @Benchmark
  @OperationsPerInvocation(LongItemBenchmark.invocation)
  def originalGet(blackhole: Blackhole): Unit =
    var index = 0
    while index < LongItemBenchmark.invocation do
      blackhole.consume(original.mightContain(item))
      Blackhole.consumeCPU(tokens)
      index += 1

object LongItemBenchmark:
  inline val invocation = 173
