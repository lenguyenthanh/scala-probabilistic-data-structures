package se.thanh.pds.bloomfilter

import scala.util.Using

class OffHeapBloomFilterSpec extends munit.FunSuite:

  test("off-heap expectedFalsePositiveRate rises as items are added"):
    Using.resource(BloomFilter.offHeap[Long](1000L, 0.01)): filter =>
      assertEquals(filter.falsePositiveRate(), 0.0)
      (1L to 1000L).foreach(filter.add)
      val rate = filter.falsePositiveRate()
      assert(rate > 0.0 && rate < 0.1, s"rate was $rate")

  test("an off-heap filter owns an explicit resource lifecycle"):
    val filter: OffHeapBloomFilter[Long] = BloomFilter.offHeap[Long](10L, 0.01)
    filter.close()
    val _ = intercept[IllegalStateException](filter.close())

  test("using an FFM-backed filter after close throws"):
    assume(Runtime.version().feature() >= 22, "FFM is final on JDK 22+")
    val filter = BloomFilter.offHeap[Long](10L, 0.01)
    filter.add(1L)
    filter.close()
    val _ = intercept[IllegalStateException](filter.mightContain(1L))
