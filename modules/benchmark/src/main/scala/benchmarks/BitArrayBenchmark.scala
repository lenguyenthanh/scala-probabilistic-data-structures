package se.thanh.pds.bloomfilter
package benchmark

import _root_.bloomfilter.mutable.UnsafeBitArray as LegacyUnsafeBitArray
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.*

import java.util.concurrent.TimeUnit

import internal.types.*

@Fork(
  value = 1,
  jvmArgsAppend = Array("-Xmx1G", "--add-opens=java.base/java.lang=ALL-UNNAMED")
)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
class BitArrayBenchmark:

  @Param(Array("5"))
  var tokens: Int = compiletime.uninitialized

  private val numberOfBits  = BitArrayBenchmark.invocation
  private val bitIndex      = numberOfBits / 2
  private val numberOfWords =
    OffHeapBitArray.numberOfWords(PositiveLong.unsafe(numberOfBits.toLong, "numberOfBits"))

  private var implementation: String                    = compiletime.uninitialized
  private var onHeap: BitArray                          = compiletime.uninitialized
  private var ffmOffHeap: OffHeapBitArray               = compiletime.uninitialized
  private var unsafeOffHeap: OffHeapBitArray            = compiletime.uninitialized
  private var legacyUnsafeOffHeap: LegacyUnsafeBitArray = compiletime.uninitialized

  @Setup(Level.Invocation)
  def setup(benchmarkParams: BenchmarkParams): Unit =
    val benchmark   = benchmarkParams.getBenchmark.split('.').last
    def positiveGet = benchmark.endsWith("Get")

    implementation =
      if benchmark.startsWith("onHeap") then
        onHeap = BitArray(numberOfBits)
        if positiveGet then onHeap.set(bitIndex)
        "onHeap"
      else if benchmark.startsWith("ffmOffHeap") then
        ffmOffHeap = new internal.ForeignMemoryBitArray(numberOfWords)
        if positiveGet then ffmOffHeap.set(bitIndex)
        "ffmOffHeap"
      else if benchmark.startsWith("unsafeOffHeap") then
        unsafeOffHeap = new internal.UnsafeBitArray(numberOfWords)
        if positiveGet then unsafeOffHeap.set(bitIndex)
        "unsafeOffHeap"
      else if benchmark.startsWith("legacyUnsafeOffHeap") then
        legacyUnsafeOffHeap = new LegacyUnsafeBitArray(numberOfBits)
        if positiveGet then legacyUnsafeOffHeap.set(bitIndex)
        "legacy"
      else throw new IllegalArgumentException(s"unknown benchmark: $benchmark")

  @TearDown(Level.Invocation)
  def tearDown(): Unit =
    implementation match
      case "ffmOffHeap"    => ffmOffHeap.close()
      case "unsafeOffHeap" => unsafeOffHeap.close()
      case "legacy"        => legacyUnsafeOffHeap.dispose()
      case _               => ()

  @Benchmark
  @OperationsPerInvocation(BitArrayBenchmark.invocation)
  def onHeapGet(blackhole: Blackhole): Unit =
    var index = 0L
    while index < numberOfBits do
      blackhole.consume(onHeap.get(bitIndex))
      Blackhole.consumeCPU(tokens)
      index += 1

  @Benchmark
  @OperationsPerInvocation(BitArrayBenchmark.invocation)
  def onHeapNewBitSet(blackhole: Blackhole): Unit =
    var index = 0L
    while index < numberOfBits do
      onHeap.set(index)
      Blackhole.consumeCPU(tokens)
      index += 1
    blackhole.consume(onHeap.nonEmptyBits)

  @Benchmark
  @OperationsPerInvocation(BitArrayBenchmark.invocation)
  def ffmOffHeapGet(blackhole: Blackhole): Unit =
    var index = 0L
    while index < numberOfBits do
      blackhole.consume(ffmOffHeap.get(bitIndex))
      Blackhole.consumeCPU(tokens)
      index += 1

  @Benchmark
  @OperationsPerInvocation(BitArrayBenchmark.invocation)
  def ffmOffHeapNewBitSet(blackhole: Blackhole): Unit =
    var index = 0L
    while index < numberOfBits do
      ffmOffHeap.set(index)
      Blackhole.consumeCPU(tokens)
      index += 1
    blackhole.consume(ffmOffHeap.nonEmptyBits)

  @Benchmark
  @OperationsPerInvocation(BitArrayBenchmark.invocation)
  def unsafeOffHeapGet(blackhole: Blackhole): Unit =
    var index = 0L
    while index < numberOfBits do
      blackhole.consume(unsafeOffHeap.get(bitIndex))
      Blackhole.consumeCPU(tokens)
      index += 1

  @Benchmark
  @OperationsPerInvocation(BitArrayBenchmark.invocation)
  def unsafeOffHeapNewBitSet(blackhole: Blackhole): Unit =
    var index = 0L
    while index < numberOfBits do
      unsafeOffHeap.set(index)
      Blackhole.consumeCPU(tokens)
      index += 1
    blackhole.consume(unsafeOffHeap.nonEmptyBits)

  @Benchmark
  @OperationsPerInvocation(BitArrayBenchmark.invocation)
  def legacyUnsafeOffHeapGet(blackhole: Blackhole): Unit =
    var index = 0L
    while index < numberOfBits do
      blackhole.consume(legacyUnsafeOffHeap.get(bitIndex))
      Blackhole.consumeCPU(tokens)
      index += 1

  @Benchmark
  @OperationsPerInvocation(BitArrayBenchmark.invocation)
  def legacyUnsafeOffHeapNewBitSet(blackhole: Blackhole): Unit =
    var index = 0L
    while index < numberOfBits do
      legacyUnsafeOffHeap.set(index)
      Blackhole.consumeCPU(tokens)
      index += 1
    blackhole.consume(legacyUnsafeOffHeap.get(numberOfBits - 1))

object BitArrayBenchmark:
  inline val invocation = 9585059
