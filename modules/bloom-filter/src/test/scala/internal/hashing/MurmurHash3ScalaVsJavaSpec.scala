package se.thanh.pds.bloomfilter.internal.hashing

import org.scalacheck.Prop.forAll

import YonikMurmurHash3.LongPair

class MurmurHash3ScalaVsJavaSpec extends munit.ScalaCheckSuite:

  property("murmurhash3_x64_128 agrees with the Java reference"):
    forAll: (key: Array[Byte]) =>
      val (h1, h2) = MurmurHash3.murmurhash3_x64_128(key, 0, key.length, 0)
      val pair     = new LongPair
      YonikMurmurHash3.murmurhash3_x64_128(key, 0, key.length, 0, pair)
      pair.val1 == h1 && pair.val2 == h2

  /* The 64-bit variant is this module's actual hot path -- every `Hash` instance
   * but `Long` goes through it -- and the old suite never covered it. It is the 128-bit
   * function with `h1 + h2` in place of the tuple, so the reference implementation pins it too.
   */
  property("murmurhash3_x64_64 is the sum of the 128-bit halves"):
    forAll: (key: Array[Byte]) =>
      val (h1, h2) = MurmurHash3.murmurhash3_x64_128(key, 0, key.length, 0)
      MurmurHash3.murmurhash3_x64_64(key, 0, key.length, 0) == h1 + h2

  property("murmurhash3_x64_64 agrees with the Java reference"):
    forAll: (key: Array[Byte]) =>
      val pair = new LongPair
      YonikMurmurHash3.murmurhash3_x64_128(key, 0, key.length, 0, pair)
      MurmurHash3.murmurhash3_x64_64(key, 0, key.length, 0) == pair.val1 + pair.val2

  property("hashing is stable across calls"):
    forAll: (key: Array[Byte]) =>
      MurmurHash3.murmurhash3_x64_64(key, 0, key.length, 0) ==
        MurmurHash3.murmurhash3_x64_64(key, 0, key.length, 0)

  property("fmix64 is injective on the sampled values"):
    forAll: (a: Long, b: Long) =>
      (a == b) == (MurmurHash3.fmix64(a) == MurmurHash3.fmix64(b))
