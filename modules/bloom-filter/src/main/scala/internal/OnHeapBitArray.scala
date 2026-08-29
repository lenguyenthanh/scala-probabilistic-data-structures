package se.thanh.pds.bloomfilter
package internal

/**
 * The implementation is based on https://github.com/alexandrnikitin/bloom-filter-scala
 * Copyright 2026 Alex Nikitin
 * Licensed under the MIT License.
 */

import types.*

/* Array backed BitArray implementation */
final private[bloomfilter] class OnHeapBitArray(numberOfWords: PositiveInt) extends BitArray:

  private val words       = new Array[Long](numberOfWords)
  private var bitCount    = 0L
  override val size: Long = numberOfWords.toLong * java.lang.Long.SIZE

  override def get(index: Long): Boolean =
    inline def word = words((index >>> 6).toInt)
    (word & index.bitMask) != 0

  override def set(index: Long): Unit =
    val wordIndex = (index >>> 6).toInt
    val word      = words(wordIndex)
    val mask      = index.bitMask
    if (word & mask) == 0 then
      words(wordIndex) = word | mask
      bitCount += 1

  override def nonEmptyBits: Long = bitCount

private[bloomfilter] object OnHeapBitArray:

  def instance(minNumberOfBits: PositiveLong): OnHeapBitArray =
    new OnHeapBitArray(numberOfWords(minNumberOfBits))

  def numberOfWords(minNumberOfBits: PositiveLong): PositiveInt =
    require(
      minNumberOfBits <= MaxNumberOfBits,
      s"""|The maximum number of bits supported by the default Bloom filter is
          |$MaxNumberOfBits. The expectedNumberOfItems is too high or the
          |falsePositiveRate is too low. Consider adjusting those values or using
          |the off-heap variant.
          |""".stripMargin
    )
    PositiveInt.unsafe(((minNumberOfBits - 1) / java.lang.Long.SIZE + 1).toInt)

  final val MaxNumberOfBits: Long = Int.MaxValue.toLong * java.lang.Long.SIZE
