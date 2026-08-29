package se.thanh.pds.bloomfilter

import java.lang.Math

sealed trait BloomFilter[T]:
  def add(x: T): Unit
  def contains(x: T): Boolean

  /**
   * return the current false positive rate
   * @return Double
   */
  def falsePositiveRate(): Double

/* use ffm or unsafe to allocate big memory to overcome array size limit */
sealed trait OffHeapBloomFilter[T] extends BloomFilter[T], AutoCloseable

object BloomFilter:

  def apply[T: Hash](numberOfItems: Long, falsePositiveRate: Double): BloomFilter[T] =
    val bits = BitArray(optimalNumberOfBits(numberOfItems, falsePositiveRate))
    new BloomFilterImpl[T](optimalNumberOfHashes(numberOfItems, bits.size), bits)

  def offHeap[T: Hash](numberOfItems: Long, falsePositiveRate: Double): OffHeapBloomFilter[T] =
    val bits = BitArray.offHeap(optimalNumberOfBits(numberOfItems, falsePositiveRate))
    new OffHeapBloomFilterImpl[T](optimalNumberOfHashes(numberOfItems, bits.size), bits)

  private[bloomfilter] def optimalNumberOfBits(numberOfItems: Long, falsePositiveRate: Double): Long =
    validateConstructionArguments(numberOfItems, falsePositiveRate)
    math.ceil(-numberOfItems.toDouble * Math.log(falsePositiveRate) / (log2 * log2)).toLong

  private[bloomfilter] def optimalNumberOfHashes(numberOfItems: Long, numberOfBits: Long): Int =
    require(numberOfItems > 0, "numberOfItems must be positive")
    require(numberOfBits > 0, "numberOfBits must be positive")
    math.max(1, math.round(numberOfBits.toDouble / numberOfItems * log2).toInt)

  private def validateConstructionArguments(expectedNumberOfItems: Long, falsePositiveRate: Double): Unit =
    require(expectedNumberOfItems > 0, "expectedNumberOfItems must be positive")
    require(
      falsePositiveRate > 0.0 && falsePositiveRate < 1.0 && falsePositiveRate.isFinite,
      "falsePositiveRate must be finite and between 0 and 1"
    )

  final private val log2: Double = Math.log(2)
  private inline val hashMagic   = 0x517cc1b727220a95L

  /**
   * The implementation is based on https://github.com/alexandrnikitin/bloom-filter-scala
   * Copyright 2026 Alex Nikitin
   * Licensed under the MIT License.
   */
  private[bloomfilter] class BloomFilterImpl[T](
      val numberOfHashes: Int,
      private val bits: BitArray
  )(using hashFor: Hash[T])
      extends BloomFilter[T]:

    val numberOfBits: Long = bits.size

    override def add(x: T): Unit =
      var h1 = hashFor.hash(x)
      val h2 = h1 * hashMagic
      bits.set(index(h1))

      var i = 1
      while i < numberOfHashes do
        h1 = nextHash(h1, h2)
        bits.set(index(h1))
        i += 1

    override def contains(x: T): Boolean =
      var h1 = hashFor.hash(x)
      if !bits.get(index(h1)) then false
      else
        val h2     = h1 * hashMagic
        var i      = 1
        var result = true
        while i < numberOfHashes && result do
          h1 = nextHash(h1, h2)
          result = bits.get(index(h1))
          i += 1
        result

    override def falsePositiveRate(): Double =
      java.lang.Math.pow(bits.nonEmptyBits.toDouble / numberOfBits, numberOfHashes.toDouble)

    private inline def index(hash: Long): Long =
      // Maps hash into [0, numberOfBits). Wants entropy across all 64 bits.
      // From https://lemire.me/blog/2016/06/27/a-fast-alternative-to-the-modulo-reduction/
      // via https://github.com/tomtomwombat/fastbloom/blob/5bbfc14f98b4fc4cd5a124626174fdf54e0b0c3d/src/lib.rs#L398
      java.lang.Math.unsignedMultiplyHigh(hash, numberOfBits)

    private inline def nextHash(h1: Long, h2: Long): Long =
      // From https://www.eecs.harvard.edu/~michaelm/postscripts/rsa2008.pdf via
      // https://github.com/tomtomwombat/fastbloom/blob/5bbfc14f98b4fc4cd5a124626174fdf54e0b0c3d/src/hasher.rs#L209
      java.lang.Long.rotateLeft(h1, 5) + h2

  final private[bloomfilter] class OffHeapBloomFilterImpl[T](numberOfHashes: Int, bits: OffHeapBitArray)(using
      Hash[T]
  ) extends BloomFilterImpl[T](numberOfHashes, bits),
        OffHeapBloomFilter[T]:

    override def close(): Unit = bits.close()
