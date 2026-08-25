package se.thanh.pds.bloomfilter.internal.hashing

import scala.util.Random

import MurmurHash3.getLong

class MurmurHash3GetLongSpec extends munit.FunSuite:

  private def previousGetLong(buf: Array[Byte], offset: Int): Long =
    (buf(offset + 7).toLong << 56) |
      ((buf(offset + 6) & 0xffL) << 48) |
      ((buf(offset + 5) & 0xffL) << 40) |
      ((buf(offset + 4) & 0xffL) << 32) |
      ((buf(offset + 3) & 0xffL) << 24) |
      ((buf(offset + 2) & 0xffL) << 16) |
      ((buf(offset + 1) & 0xffL) << 8) |
      buf(offset) & 0xffL

  test("getLong matches the previous hand-rolled implementation at every offset"):
    val random = new Random(7)
    val buf    = new Array[Byte](256)
    random.nextBytes(buf)
    for offset <- 0 to (buf.length - 8) do
      assertEquals(getLong(buf, offset), previousGetLong(buf, offset), s"at offset $offset")

  test("getLong reads little-endian"):
    val buf = Array[Byte](1, 0, 0, 0, 0, 0, 0, 0)
    assertEquals(getLong(buf, 0), 1L)

  test("getLong sign-extends the most significant byte"):
    val buf = Array[Byte](0, 0, 0, 0, 0, 0, 0, 0x80.toByte)
    assertEquals(getLong(buf, 0), Long.MinValue)

  test("getLong reads all-ones as -1"):
    assertEquals(getLong(Array.fill[Byte](8)(0xff.toByte), 0), -1L)
