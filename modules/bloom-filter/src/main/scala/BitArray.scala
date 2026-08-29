package se.thanh.pds.bloomfilter

trait BitArray:
  def get(index: Long): Boolean
  def set(index: Long): Unit
  def size: Long
  def nonEmptyBits: Long

trait OffHeapBitArray extends BitArray, AutoCloseable

object BitArray:
  import internal.types.*

  def apply(minNumberOfBits: Long): BitArray =
    internal.OnHeapBitArray.instance(PositiveLong.unsafe(minNumberOfBits, "minNumberOfBits"))

  def offHeap(minNumberOfBits: Long): OffHeapBitArray =
    OffHeapBitArray.instance(PositiveLong.unsafe(minNumberOfBits, "minNumberOfBits"))

private[bloomfilter] object OffHeapBitArray:
  import internal.types.*

  private val hasForeignMemory: Boolean = Runtime.version().feature() >= 22

  def instance(minNumberOfBits: PositiveLong): OffHeapBitArray =
    val words = numberOfWords(minNumberOfBits)
    if hasForeignMemory then new internal.ForeignMemoryBitArray(words)
    else new internal.UnsafeBitArray(words)

  def numberOfWords(minNumberOfBits: PositiveLong): PositiveLong =
    require(
      minNumberOfBits <= MaxNumberOfBits,
      s"an off-heap bit array cannot contain more than $MaxNumberOfBits bits"
    )
    PositiveLong.unsafe((minNumberOfBits - 1) / java.lang.Long.SIZE + 1)

  final val MaxNumberOfWords: Long = Long.MaxValue / java.lang.Long.SIZE
  final val MaxNumberOfBits: Long  = MaxNumberOfWords * java.lang.Long.SIZE
