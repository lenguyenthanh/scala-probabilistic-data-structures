package se.thanh.pds.bloomfilter
package internal

import java.lang.foreign.{ Arena, MemorySegment, ValueLayout }
import java.lang.invoke.VarHandle

final private[bloomfilter] class ForeignMemoryBitArray(val size: Long) extends OffHeapBitArray:
  import ForeignMemoryBitArray.word

  require(size > 0, "size must be positive")

  private val numberOfWords          = (size - 1) / java.lang.Long.SIZE + 1
  private val arena                  = Arena.ofShared()
  private val segment: MemorySegment = arena.allocate(java.lang.Long.BYTES * numberOfWords, 8)
  private var bitCount               = 0L

  override def get(index: Long): Boolean =
    val value: Long = word.get(segment, index.wordByteOffset).asInstanceOf[Long]
    (value & index.bitMask) != 0

  override def set(index: Long): Unit =
    val offset      = index.wordByteOffset
    val value: Long = word.get(segment, offset).asInstanceOf[Long]
    val mask        = index.bitMask
    if (value & mask) == 0 then
      word.set(segment, offset, value | mask)
      bitCount += 1

  override def nonEmptyBits: Long = bitCount

  override def close(): Unit = arena.close()

private object ForeignMemoryBitArray:
  private val word: VarHandle = ValueLayout.JAVA_LONG.varHandle()
