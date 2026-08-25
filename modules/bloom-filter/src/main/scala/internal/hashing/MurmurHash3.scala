package se.thanh.pds.bloomfilter.internal.hashing

/**
 * This is adapted from original version by Alex Nikitin in
 * https://github.com/alexandrnikitin/bloom-filter-scala.
 */

import java.lang.Long.rotateLeft
import java.lang.invoke.{ MethodHandles, VarHandle }
import java.nio.ByteOrder

private[bloomfilter] object MurmurHash3:

  private val longFromBytes: VarHandle =
    MethodHandles.byteArrayViewVarHandle(
      classOf[Array[Long]],
      ByteOrder.LITTLE_ENDIAN
    )

  private val c1: Long = 0x87c37b91114253d5L
  private val c2: Long = 0x4cf5ad432745937fL

  def fmix64(l: Long): Long =
    var k = l
    k ^= k >>> 33
    k *= 0xff51afd7ed558ccdL
    k ^= k >>> 33
    k *= 0xc4ceb9fe1a85ec53L
    k ^= k >>> 33
    k

  inline def getLong(key: Array[Byte], offset: Int): Long =
    longFromBytes.get(key, offset).asInstanceOf[Long]

  def murmurhash3_x64_64(
      key: Array[Byte],
      offset: Int,
      len: Int,
      seed: Int
  ): Long =
    var h1: Long = seed & 0x00000000ffffffffL
    var h2: Long = seed & 0x00000000ffffffffL

    val roundedEnd = offset + (len & 0xfffffff0); // round down to 16 byte block

    var i = offset
    while i < roundedEnd do
      var k1 = getLong(key, i)
      var k2 = getLong(key, i + 8)
      k1 *= c1; k1 = rotateLeft(k1, 31); k1 *= c2; h1 ^= k1
      h1 = rotateLeft(h1, 27); h1 += h2; h1 = h1 * 5 + 0x52dce729
      k2 *= c2; k2 = rotateLeft(k2, 33); k2 *= c1; h2 ^= k2
      h2 = rotateLeft(h2, 31); h2 += h1; h2 = h2 * 5 + 0x38495ab5

      i += 16

    var k1: Long = 0
    var k2: Long = 0

    val lenVar = len & 15
    if lenVar == 15 then k2 = (key(roundedEnd + 14) & 0xffL) << 48
    if lenVar >= 14 then k2 |= (key(roundedEnd + 13) & 0xffL) << 40
    if lenVar >= 13 then k2 |= (key(roundedEnd + 12) & 0xffL) << 32
    if lenVar >= 12 then k2 |= (key(roundedEnd + 11) & 0xffL) << 24
    if lenVar >= 11 then k2 |= (key(roundedEnd + 10) & 0xffL) << 16
    if lenVar >= 10 then k2 |= (key(roundedEnd + 9) & 0xffL) << 8
    if lenVar >= 9 then
      k2 |= (key(roundedEnd + 8) & 0xffL)
      k2 *= c2
      k2 = rotateLeft(k2, 33)
      k2 *= c1
      h2 ^= k2
    if lenVar >= 8 then k1 = key(roundedEnd + 7).toLong << 56
    if lenVar >= 7 then k1 |= (key(roundedEnd + 6) & 0xffL) << 48
    if lenVar >= 6 then k1 |= (key(roundedEnd + 5) & 0xffL) << 40
    if lenVar >= 5 then k1 |= (key(roundedEnd + 4) & 0xffL) << 32
    if lenVar >= 4 then k1 |= (key(roundedEnd + 3) & 0xffL) << 24
    if lenVar >= 3 then k1 |= (key(roundedEnd + 2) & 0xffL) << 16
    if lenVar >= 2 then k1 |= (key(roundedEnd + 1) & 0xffL) << 8
    if lenVar >= 1 then
      k1 |= (key(roundedEnd) & 0xffL)
      k1 *= c1
      k1 = rotateLeft(k1, 31)
      k1 *= c2
      h1 ^= k1

    h1 ^= len; h2 ^= len

    h1 += h2
    h2 += h1

    h1 = fmix64(h1)
    h2 = fmix64(h2)

    h1 += h2
    h2 += h1

    h1 + h2

  def murmurhash3_x64_128(
      key: Array[Byte],
      offset: Int,
      len: Int,
      seed: Int
  ): (Long, Long) =
    var h1: Long = seed & 0x00000000ffffffffL
    var h2: Long = seed & 0x00000000ffffffffL

    val roundedEnd = offset + (len & 0xfffffff0); // round down to 16 byte block

    var i = offset
    while i < roundedEnd do
      var k1 = getLong(key, i)
      var k2 = getLong(key, i + 8)
      k1 *= c1; k1 = rotateLeft(k1, 31); k1 *= c2; h1 ^= k1
      h1 = rotateLeft(h1, 27); h1 += h2; h1 = h1 * 5 + 0x52dce729
      k2 *= c2; k2 = rotateLeft(k2, 33); k2 *= c1; h2 ^= k2
      h2 = rotateLeft(h2, 31); h2 += h1; h2 = h2 * 5 + 0x38495ab5

      i += 16

    var k1: Long = 0
    var k2: Long = 0

    val lenVar = len & 15
    if lenVar == 15 then k2 = (key(roundedEnd + 14) & 0xffL) << 48
    if lenVar >= 14 then k2 |= (key(roundedEnd + 13) & 0xffL) << 40
    if lenVar >= 13 then k2 |= (key(roundedEnd + 12) & 0xffL) << 32
    if lenVar >= 12 then k2 |= (key(roundedEnd + 11) & 0xffL) << 24
    if lenVar >= 11 then k2 |= (key(roundedEnd + 10) & 0xffL) << 16
    if lenVar >= 10 then k2 |= (key(roundedEnd + 9) & 0xffL) << 8
    if lenVar >= 9 then
      k2 |= (key(roundedEnd + 8) & 0xffL)
      k2 *= c2
      k2 = rotateLeft(k2, 33)
      k2 *= c1
      h2 ^= k2
    if lenVar >= 8 then k1 = key(roundedEnd + 7).toLong << 56
    if lenVar >= 7 then k1 |= (key(roundedEnd + 6) & 0xffL) << 48
    if lenVar >= 6 then k1 |= (key(roundedEnd + 5) & 0xffL) << 40
    if lenVar >= 5 then k1 |= (key(roundedEnd + 4) & 0xffL) << 32
    if lenVar >= 4 then k1 |= (key(roundedEnd + 3) & 0xffL) << 24
    if lenVar >= 3 then k1 |= (key(roundedEnd + 2) & 0xffL) << 16
    if lenVar >= 2 then k1 |= (key(roundedEnd + 1) & 0xffL) << 8
    if lenVar >= 1 then
      k1 |= (key(roundedEnd) & 0xffL)
      k1 *= c1
      k1 = rotateLeft(k1, 31)
      k1 *= c2
      h1 ^= k1

    h1 ^= len; h2 ^= len

    h1 += h2
    h2 += h1

    h1 = fmix64(h1)
    h2 = fmix64(h2)

    h1 += h2
    h2 += h1

    (h1, h2)
