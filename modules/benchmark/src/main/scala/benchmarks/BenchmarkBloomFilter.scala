package se.thanh.pds.bloomfilter
package benchmark

import internal.types.*
import BloomFilter.{ BloomFilterImpl, OffHeapBloomFilterImpl }

object BenchmarkBloomFilter:

  def onHeap[T: Hash](numberOfBits: Long, numberOfHashes: Int): BloomFilter[T] =
    new BloomFilterImpl[T](numberOfHashes, BitArray(numberOfBits))

  def foreignMemory[T: Hash](numberOfItems: Long, falsePositiveRate: Double): OffHeapBloomFilter[T] =
    require(Runtime.version().feature() >= 22, "foreign memory benchmarks require JDK 22 or newer")
    offHeap(numberOfItems, falsePositiveRate)(new internal.ForeignMemoryBitArray(_))

  def unsafe[T: Hash](numberOfItems: Long, falsePositiveRate: Double): OffHeapBloomFilter[T] =
    offHeap(numberOfItems, falsePositiveRate)(new internal.UnsafeBitArray(_))

  private def offHeap[T: Hash](
      numberOfItems: Long,
      falsePositiveRate: Double
  )(createBits: PositiveLong => OffHeapBitArray): OffHeapBloomFilter[T] =
    val numberOfBits  = BloomFilter.optimalNumberOfBits(numberOfItems, falsePositiveRate)
    val numberOfWords = OffHeapBitArray.numberOfWords(
      PositiveLong.unsafe(numberOfBits, "numberOfBits")
    )
    val bits           = createBits(numberOfWords)
    val numberOfHashes = BloomFilter.optimalNumberOfHashes(numberOfItems, bits.size)

    new OffHeapBloomFilterImpl[T](numberOfHashes, bits)
