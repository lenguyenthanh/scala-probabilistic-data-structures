package se.thanh.pds.bloomfilter
package benchmark

import bloomfilter.mutable.BloomFilter as OriginalBloomFilter
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.*

import scala.util.Random

@Fork(
  value = 1,
  jvmArgsAppend = Array("-Xmx1G")
)
@State(Scope.Benchmark)
class StringItemBenchmark:

  @Param(Array("10"))
  var tokens: Int = compiletime.uninitialized

  private val itemsExpected     = 100000000L
  private val falsePositiveRate = 0.01

  @Param(Array("8", "32", "256", "1024"))
  var length: Int = compiletime.uninitialized

  @Param(Array("latin1", "utf16"))
  var charset: String = compiletime.uninitialized

  private var item: String                              = compiletime.uninitialized
  private var implementation: String                    = compiletime.uninitialized
  private var onHeap: BloomFilter[String]               = compiletime.uninitialized
  private var ffmOffHeap: OffHeapBloomFilter[String]    = compiletime.uninitialized
  private var unsafeOffHeap: OffHeapBloomFilter[String] = compiletime.uninitialized
  private var original: OriginalBloomFilter[String]     = compiletime.uninitialized

  @Setup(Level.Trial)
  def setup(benchmarkParams: BenchmarkParams): Unit =
    val random = new Random(0L)
    val chars  = Array.tabulate(length)(_ =>
      if charset == "latin1" then ('a' + random.nextInt(26)).toChar
      else (0x4e00 + random.nextInt(1000)).toChar
    )
    item = new String(chars)

    val benchmark = benchmarkParams.getBenchmark.split('.').last
    implementation =
      if benchmark.startsWith("onHeap") then
        onHeap = BloomFilter[String](itemsExpected, falsePositiveRate)
        onHeap.add(item)
        "onHeap"
      else if benchmark.startsWith("ffmOffHeap") then
        ffmOffHeap = BenchmarkBloomFilter.foreignMemory[String](itemsExpected, falsePositiveRate)
        ffmOffHeap.add(item)
        "ffmOffHeap"
      else if benchmark.startsWith("unsafeOffHeap") then
        unsafeOffHeap = BenchmarkBloomFilter.unsafe[String](itemsExpected, falsePositiveRate)
        unsafeOffHeap.add(item)
        "unsafeOffHeap"
      else if benchmark.startsWith("original") then
        original = OriginalBloomFilter[String](itemsExpected, falsePositiveRate)
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
  @OperationsPerInvocation(StringItemBenchmark.invocation)
  def onHeapAdd(blackhole: Blackhole): Unit =
    var index = 0
    while index < StringItemBenchmark.invocation do
      onHeap.add(item)
      blackhole.consume(item)
      Blackhole.consumeCPU(tokens)
      index += 1

  @Benchmark
  @OperationsPerInvocation(StringItemBenchmark.invocation)
  def onHeapGet(blackhole: Blackhole): Unit =
    var index = 0
    while index < StringItemBenchmark.invocation do
      blackhole.consume(onHeap.contains(item))
      Blackhole.consumeCPU(tokens)
      index += 1

  @Benchmark
  @OperationsPerInvocation(StringItemBenchmark.invocation)
  def ffmOffHeapAdd(blackhole: Blackhole): Unit =
    var index = 0
    while index < StringItemBenchmark.invocation do
      ffmOffHeap.add(item)
      blackhole.consume(item)
      Blackhole.consumeCPU(tokens)
      index += 1

  @Benchmark
  @OperationsPerInvocation(StringItemBenchmark.invocation)
  def ffmOffHeapGet(blackhole: Blackhole): Unit =
    var index = 0
    while index < StringItemBenchmark.invocation do
      blackhole.consume(ffmOffHeap.contains(item))
      Blackhole.consumeCPU(tokens)
      index += 1

  @Benchmark
  @OperationsPerInvocation(StringItemBenchmark.invocation)
  def unsafeOffHeapAdd(blackhole: Blackhole): Unit =
    var index = 0
    while index < StringItemBenchmark.invocation do
      unsafeOffHeap.add(item)
      blackhole.consume(item)
      Blackhole.consumeCPU(tokens)
      index += 1

  @Benchmark
  @OperationsPerInvocation(StringItemBenchmark.invocation)
  def unsafeOffHeapGet(blackhole: Blackhole): Unit =
    var index = 0
    while index < StringItemBenchmark.invocation do
      blackhole.consume(unsafeOffHeap.contains(item))
      Blackhole.consumeCPU(tokens)
      index += 1

  @Benchmark
  @OperationsPerInvocation(StringItemBenchmark.invocation)
  def originalAdd(blackhole: Blackhole): Unit =
    var index = 0
    while index < StringItemBenchmark.invocation do
      original.add(item)
      blackhole.consume(item)
      Blackhole.consumeCPU(tokens)
      index += 1

  @Benchmark
  @OperationsPerInvocation(StringItemBenchmark.invocation)
  def originalGet(blackhole: Blackhole): Unit =
    var index = 0
    while index < StringItemBenchmark.invocation do
      blackhole.consume(original.mightContain(item))
      Blackhole.consumeCPU(tokens)
      index += 1

object StringItemBenchmark:
  inline val invocation = 173
