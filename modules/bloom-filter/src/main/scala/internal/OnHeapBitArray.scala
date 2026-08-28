package se.thanh.pds.bloomfilter
package internal

/**
 * The implementation is based on https://github.com/alexandrnikitin/bloom-filter-scala
 * Copyright 2026 Alex Nikitin
 * Licensed under the Mit License.
 */

/* Array backed BitArray implementation */
final private[bloomfilter] class OnHeapBitArray(minNumberOfBits: Long) extends BitArray:
  require(minNumberOfBits > 0, "minNumberOfBits must be positive")

  private val numberOfWords = (minNumberOfBits - 1) / java.lang.Long.SIZE + 1
  require(
    numberOfWords <= Int.MaxValue,
    s"an on-heap bit array cannot contain $numberOfWords words"
  )

  private val words    = new Array[Long](numberOfWords.toInt)
  private var bitCount = 0L

  override val size: Long = numberOfWords * java.lang.Long.SIZE

  override def get(index: Long): Boolean =
    val word = words((index >>> 6).toInt)
    (word & index.bitMask) != 0

  override def set(index: Long): Unit =
    val wordIndex = (index >>> 6).toInt
    val word      = words(wordIndex)
    val mask      = index.bitMask
    if (word & mask) == 0 then
      words(wordIndex) = word | mask
      bitCount += 1

  override def nonEmptyBits: Long = bitCount
