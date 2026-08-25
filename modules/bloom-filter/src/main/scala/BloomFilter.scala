package se.thanh.pds.bloomfilter

trait BloomFilter[T]:
  def add(x: T): Unit
  def mightContain(x: T): Boolean

/* use ffm or unsafe to allocate big memory to overcome array size limit */
trait OffHeapBloomFilter[T] extends BloomFilter[T], AutoCloseable

object BloomFilter:

  def apply[T: Hash](numberOfItems: Long, falsePositiveRate: Double): BloomFilter[T] =
    val numberOfBits   = optimalNumberOfBits(numberOfItems, falsePositiveRate)
    val numberOfHashes = optimalNumberOfHashes(numberOfItems, numberOfBits)
    new BloomFilterImpl[T](numberOfBits, numberOfHashes, BitArray.instance(numberOfBits))

  def offHeap[T: Hash](numberOfItems: Long, falsePositiveRate: Double): OffHeapBloomFilter[T] =
    val numberOfBits   = optimalNumberOfBits(numberOfItems, falsePositiveRate)
    val numberOfHashes = optimalNumberOfHashes(numberOfItems, numberOfBits)
    new OffHeapBloomFilterImpl[T](
      numberOfBits,
      numberOfHashes,
      OffHeapBitArray.instance(numberOfBits)
    )

  def optimalNumberOfBits(numberOfItems: Long, falsePositiveRate: Double): Long =
    validateConstructionArguments(numberOfItems, falsePositiveRate)
    val log2 = math.log(2)
    math.ceil(-numberOfItems.toDouble * math.log(falsePositiveRate) / (log2 * log2)).toLong

  def optimalNumberOfHashes(numberOfItems: Long, numberOfBits: Long): Int =
    require(numberOfItems > 0, "numberOfItems must be positive")
    require(numberOfBits > 0, "numberOfBits must be positive")
    math.max(1, math.ceil(numberOfBits.toDouble / numberOfItems * math.log(2)).toInt)

  private def validateConstructionArguments(
      numberOfItems: Long,
      falsePositiveRate: Double
  ): Unit =
    require(numberOfItems > 0, "numberOfItems must be positive")
    require(
      falsePositiveRate > 0.0 && falsePositiveRate < 1.0 && falsePositiveRate.isFinite,
      "falsePositiveRate must be finite and between 0 and 1"
    )

final private class OffHeapBloomFilterImpl[T](
    numberOfBits: Long,
    numberOfHashes: Int,
    bits: OffHeapBitArray
)(using Hash[T])
    extends BloomFilterImpl[T](numberOfBits, numberOfHashes, bits),
      OffHeapBloomFilter[T]:

  override def close(): Unit = bits.close()
