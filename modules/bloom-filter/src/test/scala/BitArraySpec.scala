package se.thanh.pds.bloomfilter

import internal.types.*
import internal.{ ForeignMemoryBitArray, OnHeapBitArray, UnsafeBitArray }
import laws.BitArrayTests

class BitArraySpec extends munit.DisciplineSuite:

  private val hasForeignMemory: Boolean = Runtime.version().feature() >= 22
  private val hasUnsafe: Boolean        = Runtime.version().feature() < 22

  private def offHeapNumberOfWords(minNumberOfBits: Long): PositiveLong =
    OffHeapBitArray.numberOfWords(PositiveLong.unsafe(minNumberOfBits, "minNumberOfBits"))

  private val Storages: List[(String, Long => BitArray)] =
    List[(String, Long => BitArray)]("on-heap" -> BitArray.apply)
      ++ (if hasUnsafe then
            List[(String, Long => BitArray)](
              "Unsafe" -> (bits => new UnsafeBitArray(offHeapNumberOfWords(bits)))
            )
          else Nil)
      ++ (if hasForeignMemory then
            List[(String, Long => BitArray)](
              "ffm" -> (bits => new ForeignMemoryBitArray(offHeapNumberOfWords(bits)))
            )
          else Nil)

  Storages.foreach: (name, create) =>
    checkAll(s"BitArray.$name", BitArrayTests(create).bitArray)

  test("using an ffm array after close throws rather than corrupting memory"):
    assume(hasForeignMemory, "java.lang.foreign needs JDK 22+")
    val bits = new ForeignMemoryBitArray(offHeapNumberOfWords(128))
    bits.set(1)
    bits.close()
    val _ = intercept[IllegalStateException](bits.get(1))
    val _ = intercept[IllegalStateException](bits.set(2))

  test("OffHeapBitArray selects the implementation this JVM supports"):
    val expected = if hasForeignMemory then "ForeignMemoryBitArray" else "UnsafeBitArray"
    val bits     = BitArray.offHeap(64)
    try assertEquals(bits.getClass.getSimpleName, expected)
    finally bits.close()

  test("closing an unsafe array twice throws instead of double-freeing"):
    val bits = new UnsafeBitArray(offHeapNumberOfWords(128))
    bits.close()
    val _ = intercept[IllegalStateException](bits.close())

  test("closing an ffm array twice throws"):
    assume(hasForeignMemory, "java.lang.foreign needs JDK 22+")
    val bits = BitArray.offHeap(128)
    bits.close()
    val _ = intercept[IllegalStateException](bits.close())

  test("off-heap constructors use word counts"):
    val numberOfWords = PositiveLong.unsafe(2, "numberOfWords")
    val unsafe        = new UnsafeBitArray(numberOfWords)
    try assertEquals(unsafe.size, 128L)
    finally unsafe.close()

    if hasForeignMemory then
      val ffm = new ForeignMemoryBitArray(numberOfWords)
      try assertEquals(ffm.size, 128L)
      finally ffm.close()

  test("public factories require a positive size"):
    List[(String, Long => BitArray)]("on-heap" -> BitArray.apply, "off-heap" -> BitArray.offHeap)
      .foreach: (name, create) =>
        val _ = intercept[IllegalArgumentException](create(0), name)
        val _ = intercept[IllegalArgumentException](create(-1), name)

  test("factories reject sizes whose rounded representation would overflow"):
    val _ = intercept[IllegalArgumentException](BitArray(OnHeapBitArray.MaxNumberOfBits + 1))
    val _ = intercept[IllegalArgumentException](BitArray.offHeap(OffHeapBitArray.MaxNumberOfBits + 1))
