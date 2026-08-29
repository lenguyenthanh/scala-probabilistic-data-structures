package se.thanh.pds.bloomfilter

import java.lang.Math

/**
 * The implementation is based on https://github.com/alexandrnikitin/bloom-filter-scala
 * Copyright 2026 Alex Nikitin
 * Licensed under the MIT License.
 */

trait BloomFilter[T]:
  def add(x: T): Unit
  def mightContain(x: T): Boolean

/* use ffm or unsafe to allocate big memory to overcome array size limit */
trait OffHeapBloomFilter[T] extends BloomFilter[T], AutoCloseable

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
