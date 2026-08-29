package se.thanh.pds.bloomfilter
package internal

/**
 * The implementation is based on https://github.com/alexandrnikitin/bloom-filter-scala
 * Copyright 2026 Alex Nikitin
 * Licensed under the MIT License.
 */

import sun.misc.Unsafe as JUnsafe

import scala.util.Try

import types.*

// Unsafe implementation
final private[bloomfilter] class UnsafeBitArray(numberOfWords: PositiveLong) extends OffHeapBitArray:
  import UnsafeBitArray.unsafe

  require(
    numberOfWords <= OffHeapBitArray.MaxNumberOfWords,
    s"numberOfWords cannot exceed ${OffHeapBitArray.MaxNumberOfWords}"
  )

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
