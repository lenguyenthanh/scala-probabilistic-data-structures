package se.thanh.pds.bloomfilter

import se.thanh.pds.bloomfilter.internal.hashing.MurmurHash3

import scala.util.Random

class HashSpec extends munit.FunSuite:

  private val edgeCases = List(
    "",
    "\u0000",
    "a\u0000b",
    "hello",
    "h\u00e9llo \u00ff",
    "\u65e5\u672c\u8a9e",
    "\u0100abc",
    "abc\u0100",
    "\ud83d\ude00",
    "x\ud83d\ude00y",
    "\ud800",
    "\udc00",
    "a\ud800b",
    "a\udc00b"
  )

  test("the safe String hash has frozen UTF-16LE vectors"):
    val vectors = List(
      ""                   -> 0L,
      "hello"              -> 7620464346463930955L,
      "\u65e5\u672c\u8a9e" -> 437889797428208642L
    )

    vectors.foreach: (value, expected) =>
      assertEquals(Hash.strings.safe.hash(value), expected, clue(value))

  test("the safe String hash uses logical UTF-16LE for representative content"):
    edgeCases.foreach: value =>
      assertEquals(Hash.strings.safe.hash(value), hashBytes(utf16LeBytes(value), 0), clue(value))

  test("the safe String hash uses logical UTF-16LE for randomized code units"):
    val random = new Random(0L)
    var sample = 0
    while sample < 1000 do
      val value = randomValue(random, sample)
      assertEquals(
        Hash.strings.safe.hash(value),
        hashBytes(utf16LeBytes(value), 0),
        s"sample $sample"
      )
      sample += 1

  test("the UTF-16LE String entry point preserves nonzero Murmur seeds"):
    val seeds = List(Int.MinValue, -1, 1, 42, Int.MaxValue)
    for
      value <- edgeCases
      seed  <- seeds
    do
      assertEquals(
        MurmurHash3.murmurhash3_x64_64_utf16le(value, seed),
        hashBytes(utf16LeBytes(value), seed)
      )

  test("the safe String hash does not use the compact Latin-1 representation"):
    val value       = "hello world"
    val latin1Bytes = value.map(_.toByte).toArray
    assertNotEquals(
      Hash.strings.safe.hash(value),
      hashBytes(latin1Bytes, 0)
    )

  private def hashBytes(bytes: Array[Byte], seed: Int): Long =
    MurmurHash3.murmurhash3_x64_64(bytes, 0, bytes.length, seed)

  private def randomValue(random: Random, shape: Int): String =
    val length = random.nextInt(300)
    val chars  = Math.floorMod(shape, 6) match
      case 0 => Array.fill(length)(random.nextInt(0x100).toChar)
      case 1 => Array.fill(length)((0x4e00 + random.nextInt(1000)).toChar)
      case 2 => Array.fill(length)(random.nextInt(0x10000).toChar)
      case 3 => Array.tabulate(length)(i => if i % 2 == 0 then '\ud83d' else '\ude00')
      case 4 =>
        Array.tabulate(length)(i => if i == length - 1 then '\u4e00' else random.nextInt(0x80).toChar)
      case _ =>
        Array.tabulate(length)(i => if i == length / 2 then '\u0000' else random.nextInt(0x100).toChar)
    new String(chars)

  private def utf16LeBytes(value: String): Array[Byte] =
    val bytes = new Array[Byte](value.length * 2)
    var i     = 0
    while i < value.length do
      val codeUnit = value.charAt(i)
      bytes(i * 2) = codeUnit.toByte
      bytes(i * 2 + 1) = (codeUnit >>> 8).toByte
      i += 1
    bytes
