package se.thanh.pds.bloomfilter

import internal.{ ForeignMemoryBitArray, OnHeapBitArray, UnsafeBitArray }
import laws.BitArrayTests

class BitArraySpec extends munit.DisciplineSuite:

  private val hasForeignMemory: Boolean = Runtime.version().feature() >= 22
  private val hasUnsafe: Boolean        = Runtime.version().feature() < 22

  private val Storages: List[(String, Long => BitArray)] =
    List[(String, Long => BitArray)]("on-heap" -> (new OnHeapBitArray(_)))
      ++ (if hasUnsafe then List("Unsafe" -> (new UnsafeBitArray(_))) else Nil)
      ++ (if hasForeignMemory then List("ffm" -> (new ForeignMemoryBitArray(_))) else Nil)

  Storages.foreach: (name, create) =>
    checkAll(s"BitArray.$name", BitArrayTests(create).bitArray)

  test("using an ffm array after close throws rather than corrupting memory"):
    assume(hasForeignMemory, "java.lang.foreign needs JDK 22+")
    val bits = new ForeignMemoryBitArray(128)
    bits.set(1)
    bits.close()
    val _ = intercept[IllegalStateException](bits.get(1))
    val _ = intercept[IllegalStateException](bits.set(2))

  test("OffHeapBitArray selects the implementation this JVM supports"):
    val expected = if hasForeignMemory then "ForeignMemoryBitArray" else "UnsafeBitArray"
    val bits     = OffHeapBitArray.instance(64)
    try assertEquals(bits.getClass.getSimpleName, expected)
    finally bits.close()

  test("closing an unsafe array twice throws instead of double-freeing"):
    val bits = new UnsafeBitArray(128)
    bits.close()
    val _ = intercept[IllegalStateException](bits.close())

  test("closing an ffm array twice throws"):
    assume(hasForeignMemory, "java.lang.foreign needs JDK 22+")
    val bits = OffHeapBitArray.instance(128)
    bits.close()
    val _ = intercept[IllegalStateException](bits.close())

  test("all implementations require a positive size"):
    Storages.foreach: (name, create) =>
      val _ = intercept[IllegalArgumentException](create(0), name)
