package se.thanh.pds.bloomfilter
package internal

/**
 * This is adapted from original version by Alex Nikitin in
 * https://github.com/alexandrnikitin/bloom-filter-scala.
 */

import sun.misc.Unsafe as JUnsafe

import scala.util.Try

// Unsafe implementation
final private[bloomfilter] class UnsafeBitArray(minNumberOfBits: Long) extends OffHeapBitArray:
  import UnsafeBitArray.unsafe

  require(minNumberOfBits > 0, "minNumberOfBits must be positive")

  private val numberOfWords = (minNumberOfBits - 1) / java.lang.Long.SIZE + 1
  private val numberOfBytes = java.lang.Long.BYTES * numberOfWords
  private val pointer       = unsafe.allocateMemory(numberOfBytes)
  unsafe.setMemory(pointer, numberOfBytes, 0.toByte)

  private var bitCount = 0L
  private var closed   = false

  override val size: Long = numberOfWords * java.lang.Long.SIZE

  override def get(index: Long): Boolean =
    val value = unsafe.getLong(pointer + index.wordByteOffset)
    (value & index.bitMask) != 0

  override def set(index: Long): Unit =
    val offset = pointer + index.wordByteOffset
    val value  = unsafe.getLong(offset)
    val mask   = index.bitMask
    if (value & mask) == 0 then
      unsafe.putLong(offset, value | mask)
      bitCount += 1

  override def nonEmptyBits: Long = bitCount

  override def close(): Unit =
    if closed then throw new IllegalStateException("already closed")
    closed = true
    unsafe.freeMemory(pointer)

private object UnsafeBitArray:
  private[bloomfilter] val unsafe: JUnsafe = Try {
    classOf[JUnsafe].getDeclaredFields
      .find(_.getType == classOf[JUnsafe])
      .map: field =>
        field.setAccessible(true)
        field.get(null).asInstanceOf[JUnsafe]
      .getOrElse(throw new IllegalStateException("cannot find sun.misc.Unsafe instance"))
  }.recover { case cause: Throwable =>
    throw new ExceptionInInitializerError(cause)
  }.get
