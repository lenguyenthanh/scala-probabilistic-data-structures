package se.thanh.pds.bloomfilter

import java.nio.charset.StandardCharsets

class HashSpec extends munit.FunSuite:

  test("hashing a String is stable across calls"):
    for s <- List("", "a", "hello", "héllo wörld", "日本語", "日本語です、これはテスト") do
      assertEquals(Hash[String].hash(s), Hash[String].hash(s), s"'$s'")

  test("hashing a String is stable across separately-built equal instances"):
    val a = "lichess"
    val b = new String(Array('l', 'i', 'c', 'h', 'e', 's', 's'))
    assertEquals(a, b)
    assertEquals(Hash[String].hash(a), Hash[String].hash(b))

  /* Hashing `from.length` -- the char count -- over a backing array that holds two bytes per
   * char for non-Latin-1 content fed only the first half of the array to the hash, so a
   * string and its own first half hashed identically.
   */
  test("hashing a String covers the whole backing array for non-Latin-1 input"):
    assertNotEquals(Hash[String].hash("日本語"), Hash[String].hash("日"))
    assertNotEquals(Hash[String].hash("日本語です、これはテスト"), Hash[String].hash("日本語です"))

  test("hashing a String distinguishes strings differing only in their second half"):
    assertNotEquals(Hash[String].hash("あいうえお"), Hash[String].hash("あいうかき"))

  test("hashing a String distinguishes Latin-1 strings"):
    val hashes = List("alice", "bob", "carol", "dave", "erin").map(Hash[String].hash)
    assertEquals(hashes.distinct.size, hashes.size)

  test("hashing Array[Byte] agrees with hashing the same bytes as a Latin-1 String"):
    // A Latin-1 String's backing array is exactly its ISO-8859-1 encoding, so the two
    // typeclass instances must see identical bytes and produce identical hashes.
    val s     = "hello world"
    val bytes = s.getBytes(StandardCharsets.ISO_8859_1)
    assertEquals(Hash[String].hash(s), Hash[Array[Byte]].hash(bytes))
