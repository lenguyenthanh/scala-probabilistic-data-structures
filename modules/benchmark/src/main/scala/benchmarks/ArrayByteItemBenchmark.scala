package se.thanh.pds.bloomfilter
package benchmark

import bloomfilter.mutable.BloomFilter as OriginalBloomFilter
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.*

import scala.util.Random

@Fork(
  value = 1,
  jvmArgsAppend = Array("-Xmx1G", "--add-opens=java.base/java.lang=ALL-UNNAMED")
)
@State(Scope.Benchmark)
class ArrayByteItemBenchmark:

  @Param(Array("10"))
  var tokens: Int = compiletime.uninitialized

  private val itemsExpected     = 1000000L
  private val falsePositiveRate = 0.01

  @Param(Array("8", "32", "256", "1024"))
  var length: Int = compiletime.uninitialized

  private var item: Array[Byte]                              = compiletime.uninitialized
  private var implementation: String                         = compiletime.uninitialized
  private var onHeap: BloomFilter[Array[Byte]]               = compiletime.uninitialized
  private var ffmOffHeap: OffHeapBloomFilter[Array[Byte]]    = compiletime.uninitialized
  private var unsafeOffHeap: OffHeapBloomFilter[Array[Byte]] = compiletime.uninitialized
  private var original: OriginalBloomFilter[Array[Byte]]     = compiletime.uninitialized

  @Setup(Level.Trial)
  def setup(benchmarkParams: BenchmarkParams): Unit =
    val random = new Random(0L)
    item = new Array[Byte](length)
    random.nextBytes(item)

    val benchmark = benchmarkParams.getBenchmark.split('.').last
    implementation =
      if benchmark.startsWith("onHeap") then
        onHeap = BloomFilter[Array[Byte]](itemsExpected, falsePositiveRate)
        onHeap.add(item)
        "onHeap"
      else if benchmark.startsWith("ffmOffHeap") then
        ffmOffHeap = BenchmarkBloomFilter.foreignMemory[Array[Byte]](itemsExpected, falsePositiveRate)
        ffmOffHeap.add(item)
        "ffmOffHeap"
      else if benchmark.startsWith("unsafeOffHeap") then
        unsafeOffHeap = BenchmarkBloomFilter.unsafe[Array[Byte]](itemsExpected, falsePositiveRate)
        unsafeOffHeap.add(item)
        "unsafeOffHeap"
      else if benchmark.startsWith("original") then
        original = OriginalBloomFilter[Array[Byte]](itemsExpected, falsePositiveRate)
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
  @OperationsPerInvocation(ArrayByteItemBenchmark.invocation)
  def onHeapAdd(blackhole: Blackhole): Unit =
    var index = 0
    while index < ArrayByteItemBenchmark.invocation do
      onHeap.add(item)
      blackhole.consume(item)
      Blackhole.consumeCPU(tokens)
      index += 1

  @Benchmark
  @OperationsPerInvocation(ArrayByteItemBenchmark.invocation)
  def onHeapGet(blackhole: Blackhole): Unit =
    var index = 0
    while index < ArrayByteItemBenchmark.invocation do
      blackhole.consume(onHeap.contains(item))
      Blackhole.consumeCPU(tokens)
      index += 1

  @Benchmark
  @OperationsPerInvocation(ArrayByteItemBenchmark.invocation)
  def ffmOffHeapAdd(blackhole: Blackhole): Unit =
    var index = 0
    while index < ArrayByteItemBenchmark.invocation do
      ffmOffHeap.add(item)
      blackhole.consume(item)
      Blackhole.consumeCPU(tokens)
      index += 1

  @Benchmark
  @OperationsPerInvocation(ArrayByteItemBenchmark.invocation)
  def ffmOffHeapGet(blackhole: Blackhole): Unit =
    var index = 0
    while index < ArrayByteItemBenchmark.invocation do
      blackhole.consume(ffmOffHeap.contains(item))
      Blackhole.consumeCPU(tokens)
      index += 1

  @Benchmark
  @OperationsPerInvocation(ArrayByteItemBenchmark.invocation)
  def unsafeOffHeapAdd(blackhole: Blackhole): Unit =
    var index = 0
    while index < ArrayByteItemBenchmark.invocation do
      unsafeOffHeap.add(item)
      blackhole.consume(item)
      Blackhole.consumeCPU(tokens)
      index += 1

  @Benchmark
  @OperationsPerInvocation(ArrayByteItemBenchmark.invocation)
  def unsafeOffHeapGet(blackhole: Blackhole): Unit =
    var index = 0
    while index < ArrayByteItemBenchmark.invocation do
      blackhole.consume(unsafeOffHeap.contains(item))
      Blackhole.consumeCPU(tokens)
      index += 1

  @Benchmark
  @OperationsPerInvocation(ArrayByteItemBenchmark.invocation)
  def originalAdd(blackhole: Blackhole): Unit =
    var index = 0
    while index < ArrayByteItemBenchmark.invocation do
      original.add(item)
      blackhole.consume(item)
      Blackhole.consumeCPU(tokens)
      index += 1

  @Benchmark
  @OperationsPerInvocation(ArrayByteItemBenchmark.invocation)
  def originalGet(blackhole: Blackhole): Unit =
    var index = 0
    while index < ArrayByteItemBenchmark.invocation do
      blackhole.consume(original.mightContain(item))
      Blackhole.consumeCPU(tokens)
      index += 1

object ArrayByteItemBenchmark:
  inline val invocation = 173
