package se.thanh.pds.bloomfilter

trait BitArray:
  def get(index: Long): Boolean
  def set(index: Long): Unit
  def size: Long
  def nonEmptyBits: Long

object BitArray:
  def instance(minNumberOfBits: Long): BitArray =
    new internal.OnHeapBitArray(minNumberOfBits)

trait OffHeapBitArray extends BitArray, AutoCloseable

private[bloomfilter] object OffHeapBitArray:
  private val hasForeignMemory: Boolean = Runtime.version().feature() >= 22

  def instance(minNumberOfBits: Long): OffHeapBitArray =
    if hasForeignMemory then new internal.ForeignMemoryBitArray(minNumberOfBits)
    else new internal.UnsafeBitArray(minNumberOfBits)
