package se.thanh.pds.bloomfilter
package benchmark

import org.openjdk.jmh.annotations.*

import java.util.concurrent.TimeUnit

@Fork(
  value = 1,
  jvmArgsAppend = Array(
    "-Xms1G",
    "-Xmx1G",
    "--add-opens=java.base/java.lang=ALL-UNNAMED"
  )
)
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
class StringHashBenchmark:

  @Param(Array("safe", "unsafe"))
  var implementation: String = compiletime.uninitialized

  @Param(Array("8", "32", "256", "1024"))
  var length: Int = compiletime.uninitialized

  @Param(Array("ascii", "latin1", "bmp")) // "supplementary", "early-wide", "late-wide"))
  var shape: String = compiletime.uninitialized

  private var value: String        = compiletime.uninitialized
  private var hasher: Hash[String] = compiletime.uninitialized

  @Setup(Level.Trial)
  def setup(): Unit =
    value = StringHashBenchmarkData.value(shape, length)
    val expectedBytes = implementation match
      case "safe" =>
        hasher = Hash[String]
        StringHashBenchmarkData.utf16LeBytes(value)
      case "unsafe" =>
        hasher = Hash.UnsafeCompact.stringHash
        StringHashBenchmarkData.compactBytes(value)
      case other => throw new IllegalArgumentException(s"unknown implementation: $other")
    val expected = Hash[Array[Byte]].hash(expectedBytes)
    require(hasher.hash(value) == expected, s"$implementation String hash semantics changed")

  @Benchmark
  def hash(): Long =
    hasher.hash(value)

private[benchmark] object StringHashBenchmarkData:

  def value(shape: String, length: Int): String =
    val chars = shape match
      case "ascii"  => Array.tabulate(length)(i => ('a' + i % 26).toChar)
      case "latin1" => Array.tabulate(length)(i => (0xc0 + i % 32).toChar)
      case "bmp"    => Array.tabulate(length)(i => (0x4e00 + i % 1000).toChar)
      // case "supplementary" =>
      //   val pair = Character.toChars(0x1f600)
      //   Array.tabulate(length)(i => pair(i & 1))
      // case "early-wide" =>
      //   Array.tabulate(length)(i => if i == 0 then '\u4e00' else ('a' + i % 26).toChar)
      // case "late-wide" =>
      //   Array.tabulate(length)(i => if i == length - 1 then '\u4e00' else ('a' + i % 26).toChar)
      case other => throw new IllegalArgumentException(s"unknown shape: $other")

    new String(chars)

  def compactBytes(value: String): Array[Byte] =
    if value.forall(_ <= 0xff) then
      val bytes = new Array[Byte](value.length)
      var i     = 0
      while i < value.length do
        bytes(i) = value.charAt(i).toByte
        i += 1
      bytes
    else utf16LeBytes(value)

  def utf16LeBytes(value: String): Array[Byte] =
    val bytes = new Array[Byte](value.length * 2)
    var i     = 0
    while i < value.length do
      val codeUnit = value.charAt(i)
      bytes(i * 2) = codeUnit.toByte
      bytes(i * 2 + 1) = (codeUnit >>> 8).toByte
      i += 1
    bytes
