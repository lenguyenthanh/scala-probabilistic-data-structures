package se.thanh.pds.bloomfilter
package internal

import java.lang.invoke.VarHandle
import scala.annotation.static

import types.*

final private[bloomfilter] class ForeignMemoryBitArray(numberOfWords: PositiveLong) extends OffHeapBitArray:
  require(
    numberOfWords <= OffHeapBitArray.MaxNumberOfWords,
    s"numberOfWords cannot exceed ${OffHeapBitArray.MaxNumberOfWords}"
  )

  private val memory   = ForeignMemoryBitArray.allocate(numberOfWords)
  private val arena    = memory.arena
  private val segment  = memory.segment
  private var bitCount = 0L

  override val size: Long = numberOfWords * java.lang.Long.SIZE

  import ForeignMemoryBitArray.word
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

private[bloomfilter] object ForeignMemoryBitArray:

  // this helps jit fold this in get function
  @static final val word: VarHandle =
    valueLayoutClass.getMethod("varHandle").invoke(javaLong).asInstanceOf[VarHandle]

  @static
  final private case class Memory(arena: AutoCloseable, segment: AnyRef)
  private val arenaClass       = Class.forName("java.lang.foreign.Arena")
  private val valueLayoutClass = Class.forName("java.lang.foreign.ValueLayout")
  private val ofShared         = arenaClass.getMethod("ofShared")
  private val allocate         = arenaClass.getMethod("allocate", java.lang.Long.TYPE, java.lang.Long.TYPE)
  private val javaLong         = valueLayoutClass.getField("JAVA_LONG").get(null)

  private def newArena(): AutoCloseable = ofShared.invoke(null).asInstanceOf[AutoCloseable]

  private def allocate(arena: AutoCloseable, numberOfBytes: Long): AnyRef =
    allocate
      .invoke(arena, Long.box(numberOfBytes), Long.box(java.lang.Long.BYTES.toLong))
      .asInstanceOf[AnyRef]

  private def allocate(numberOfWords: Long): Memory =
    val arena = newArena()
    try Memory(arena, allocate(arena, java.lang.Long.BYTES * numberOfWords))
    catch
      case cause: Throwable =>
        try arena.close()
        catch case closeCause: Throwable => cause.addSuppressed(closeCause)
        throw cause
