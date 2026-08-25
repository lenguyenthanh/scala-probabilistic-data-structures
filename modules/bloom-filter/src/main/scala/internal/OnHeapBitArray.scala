package se.thanh.pds.bloomfilter
package internal

/**
 * This is adapted from original version by Alex Nikitin in
 * https://github.com/alexandrnikitin/bloom-filter-scala.
 */

/* Array backed BitArray implementation */
final private[bloomfilter] class OnHeapBitArray(val size: Long) extends BitArray:
  require(size > 0, "size must be positive")

  private val numberOfWords = (size - 1) / java.lang.Long.SIZE + 1
  require(
    numberOfWords <= Int.MaxValue,
    s"an on-heap bit array cannot contain $numberOfWords words"
  )

  private val words    = new Array[Long](numberOfWords.toInt)
  private var bitCount = 0L

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
