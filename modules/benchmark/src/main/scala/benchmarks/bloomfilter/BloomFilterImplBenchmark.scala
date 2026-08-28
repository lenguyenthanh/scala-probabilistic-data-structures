package benchmarks.bloomfilter

import org.openjdk.jmh.annotations.{
  Benchmark,
  Fork,
  Level,
  OperationsPerInvocation,
  Param,
  Scope,
  Setup,
  State
}
import org.openjdk.jmh.infra.Blackhole
import se.thanh.pds.bloomfilter.{ BenchmarkBloomFilter, BloomFilter }

import scala.util.Random

@Fork(
  value = 1,
  jvmArgsAppend = Array("-Xmx1G", "--add-opens=java.base/java.lang=ALL-UNNAMED")
)
@State(Scope.Benchmark)
class BloomFilterImplBenchmark:

  @Param(Array("10"))
  var tokens: Int = compiletime.uninitialized

  @Param(Array("10"))
  var length: Int = compiletime.uninitialized

  @Param(Array("latin1"))
  var charset: String = compiletime.uninitialized

  @Param(Array("1", "2", "4", "7", "8", "16", "32"))
  var numberOfHashes: Int = compiletime.uninitialized

  private val numberOfBits = 958505838L

  private var item: String                 = compiletime.uninitialized
  private var current: BloomFilter[String] = compiletime.uninitialized

  @Setup(Level.Trial)
  def setup(): Unit =
    val random = new Random(0L)
    val chars  = Array.tabulate(length)(_ =>
      if charset == "latin1" then ('a' + random.nextInt(26)).toChar
      else (0x4e00 + random.nextInt(1000)).toChar
    )
    item = new String(chars)

    current = BenchmarkBloomFilter.onHeap[String](numberOfBits, numberOfHashes)
    current.add(item)

    require(current.mightContain(item), "current BloomFilterImpl must contain the setup item")

  @Benchmark
  @OperationsPerInvocation(BloomFilterImplBenchmark.invocation)
  def currentGet(blackhole: Blackhole): Unit =
    var index = 0
    while index < BloomFilterImplBenchmark.invocation do
      blackhole.consume(current.mightContain(item))
      Blackhole.consumeCPU(tokens)
      index += 1

object BloomFilterImplBenchmark:
  inline val invocation = 173
