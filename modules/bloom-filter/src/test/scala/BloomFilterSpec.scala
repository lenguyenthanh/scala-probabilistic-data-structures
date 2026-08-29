package se.thanh.pds.bloomfilter

class BloomFilterSpec extends munit.FunSuite:

  test("a String filter keeps the false-positive rate near the requested one"):
    val numberOfItems = 10000
    val filter        = BloomFilter[String](numberOfItems, 0.01)
    (1 to numberOfItems).foreach(i => filter.add(s"present-$i"))

    val falsePositives =
      (1 to numberOfItems).count(i => filter.mightContain(s"absent-$i"))
    val rate = falsePositives.toDouble / numberOfItems
    assert(rate < 0.05, s"false-positive rate was $rate")

  test("expectedFalsePositiveRate rises from zero as items are added"):
    val filter = BloomFilter[Long](1000L, 0.01).asInstanceOf[BloomFilter.BloomFilterImpl[Long]]
    assertEquals(filter.expectedFalsePositiveRate(), 0.0)
    (1L to 1000L).foreach(filter.add)
    val rate = filter.expectedFalsePositiveRate()
    assert(rate > 0.0 && rate < 0.1, s"rate was $rate")

  test("sizing uses the standard Bloom-filter formula"):
    val numberOfBits = BloomFilter.optimalNumberOfBits(1000L, 0.01)
    assertEquals(numberOfBits, 9586L)
    assertEquals(BloomFilter.optimalNumberOfHashes(1000L, numberOfBits), 7)

  test("construction rejects invalid sizing arguments"):
    val _ = intercept[IllegalArgumentException](BloomFilter[Long](0L, 0.01))
    val _ = intercept[IllegalArgumentException](BloomFilter[Long](10L, 0.0))
    val _ = intercept[IllegalArgumentException](BloomFilter[Long](10L, 1.0))
    val _ = intercept[IllegalArgumentException](BloomFilter[Long](10L, Double.NaN))

  test("an on-heap filter exposes no resource lifecycle"):
    val filter = BloomFilter[Long](10L, 0.01)
    assert(!filter.isInstanceOf[AutoCloseable])

  test("double hashing maps every probe into the BitArray range"):
    val bits = new RecordingBitArray(65L)
    given Hash[Long] with
      override def hash(from: Long): Long = from

    val filter = new BloomFilter.BloomFilterImpl[Long](17, bits)
    List(Long.MinValue, Long.MinValue + 1, -1L, 0L, 1L, Long.MaxValue).foreach: hash =>
      filter.add(hash)
      assert(filter.mightContain(hash))

  final private class RecordingBitArray(val size: Long) extends BitArray:
    val indices                            = collection.mutable.ListBuffer.empty[Long]
    override def get(index: Long): Boolean =
      require(index >= 0 && index < size, s"index $index outside [0, $size)")
      indices.contains(index)
    override def set(index: Long): Unit =
      require(index >= 0 && index < size, s"index $index outside [0, $size)")
      indices += index
    override def nonEmptyBits: Long = indices.distinct.size.toLong
